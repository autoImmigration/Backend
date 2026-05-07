package com.yongsik.immigrationops.review.application;

import com.yongsik.immigrationops.classification.domain.ClassificationResult;
import com.yongsik.immigrationops.document.domain.DocumentType;
import com.yongsik.immigrationops.ocr.domain.OcrResult;
import com.yongsik.immigrationops.passport.domain.PassportData;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ReviewPolicy {

    public boolean requiresReview(
            ClassificationResult classificationResult,
            OcrResult ocrResult,
            PassportData passportData,
            Map<String, String> extractedFields
    ) {
        if (classificationResult.documentType() == DocumentType.UNKNOWN) {
            return true;
        }
        if (ocrResult.confidence() < 0.60 || classificationResult.confidence() < 0.70) {
            return true;
        }
        if (ocrResult.fullText().isBlank()) {
            return true;
        }
        if (classificationResult.documentType() == DocumentType.PASSPORT && passportData == null) {
            return true;
        }
        return extractedFields.values().stream().anyMatch(String::isBlank);
    }
}

