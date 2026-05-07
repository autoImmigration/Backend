package com.yongsik.immigrationops.document.application;

import com.yongsik.immigrationops.classification.domain.ClassificationResult;
import com.yongsik.immigrationops.classification.domain.DocumentClassifier;
import com.yongsik.immigrationops.document.domain.DocumentId;
import com.yongsik.immigrationops.document.domain.DocumentRepository;
import com.yongsik.immigrationops.document.domain.DocumentType;
import com.yongsik.immigrationops.document.domain.SubmissionDocument;
import com.yongsik.immigrationops.ocr.domain.OcrEngine;
import com.yongsik.immigrationops.ocr.domain.OcrResult;
import com.yongsik.immigrationops.passport.application.PassportMrzExtractor;
import com.yongsik.immigrationops.passport.domain.PassportData;
import com.yongsik.immigrationops.review.application.ReviewPolicy;
import com.yongsik.immigrationops.storage.domain.FileStorage;
import com.yongsik.immigrationops.storage.domain.StoredFile;
import com.yongsik.immigrationops.template.application.TemplateRegistry;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentProcessingService {

    private final FileStorage fileStorage;
    private final OcrEngine ocrEngine;
    private final DocumentClassifier documentClassifier;
    private final PassportMrzExtractor passportMrzExtractor;
    private final ReviewPolicy reviewPolicy;
    private final TemplateRegistry templateRegistry;
    private final DocumentRepository documentRepository;

    public DocumentProcessingService(
            FileStorage fileStorage,
            OcrEngine ocrEngine,
            DocumentClassifier documentClassifier,
            PassportMrzExtractor passportMrzExtractor,
            ReviewPolicy reviewPolicy,
            TemplateRegistry templateRegistry,
            DocumentRepository documentRepository
    ) {
        this.fileStorage = fileStorage;
        this.ocrEngine = ocrEngine;
        this.documentClassifier = documentClassifier;
        this.passportMrzExtractor = passportMrzExtractor;
        this.reviewPolicy = reviewPolicy;
        this.templateRegistry = templateRegistry;
        this.documentRepository = documentRepository;
    }

    public DocumentProcessingResult process(MultipartFile file) throws IOException {
        DocumentId id = DocumentId.newId();
        StoredFile storedFile = fileStorage.store(id, file);
        SubmissionDocument document = SubmissionDocument.uploaded(
                id,
                file.getOriginalFilename(),
                storedFile.mediaType(),
                storedFile.path().toString()
        );

        OcrResult ocrResult = ocrEngine.extract(storedFile);
        ClassificationResult classificationResult = documentClassifier.classify(
                storedFile.originalFilename(),
                ocrResult.fullText()
        );

        DocumentType documentType = classificationResult.documentType();
        PassportData passportData = documentType == DocumentType.PASSPORT
                ? passportMrzExtractor.extract(ocrResult.fullText()).orElse(null)
                : null;

        Map<String, String> extractedFields = buildExtractedFields(documentType, ocrResult.fullText(), passportData);
        boolean reviewRequired = reviewPolicy.requiresReview(classificationResult, ocrResult, passportData, extractedFields);

        document.completeProcessing(
                documentType,
                ocrResult.fullText(),
                Math.min(ocrResult.confidence(), classificationResult.confidence()),
                extractedFields,
                reviewRequired,
                passportData
        );
        documentRepository.save(document);

        return toResult(document);
    }

    public java.util.List<DocumentProcessingResult> getAll() {
        return documentRepository.findAll().stream().map(this::toResult).toList();
    }

    public DocumentProcessingResult getById(String id) {
        SubmissionDocument document = documentRepository.findById(new DocumentId(id))
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
        return toResult(document);
    }

    private DocumentProcessingResult toResult(SubmissionDocument document) {
        return new DocumentProcessingResult(
                document.id().value(),
                document.originalFilename(),
                document.mediaType(),
                document.storagePath(),
                document.documentType(),
                document.status(),
                document.extractionConfidence(),
                document.reviewRequired(),
                document.extractedText(),
                document.extractedFields(),
                document.passportData().orElse(null),
                templateRegistry.expectedFields(document.documentType()),
                document.createdAt(),
                document.updatedAt()
        );
    }

    private Map<String, String> buildExtractedFields(DocumentType documentType, String extractedText, PassportData passportData) {
        if (documentType == DocumentType.PASSPORT && passportData != null) {
            Map<String, String> passportFields = new LinkedHashMap<>();
            passportFields.put("passportNumber", passportData.passportNumber());
            passportFields.put("surname", passportData.surname());
            passportFields.put("givenNames", passportData.givenNames());
            passportFields.put("birthDate", passportData.birthDate());
            passportFields.put("expiryDate", passportData.expiryDate());
            return passportFields;
        }
        return templateRegistry.extractFields(documentType, extractedText);
    }
}
