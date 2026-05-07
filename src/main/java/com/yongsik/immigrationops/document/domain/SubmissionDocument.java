package com.yongsik.immigrationops.document.domain;

import com.yongsik.immigrationops.passport.domain.PassportData;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public class SubmissionDocument {

    private final DocumentId id;
    private final String originalFilename;
    private final String mediaType;
    private final String storagePath;
    private final Instant createdAt;
    private DocumentType documentType;
    private DocumentStatus status;
    private String extractedText;
    private double extractionConfidence;
    private boolean reviewRequired;
    private Map<String, String> extractedFields;
    private PassportData passportData;
    private Instant updatedAt;

    private SubmissionDocument(
            DocumentId id,
            String originalFilename,
            String mediaType,
            String storagePath
    ) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.mediaType = mediaType;
        this.storagePath = storagePath;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.documentType = DocumentType.UNKNOWN;
        this.status = DocumentStatus.UPLOADED;
        this.extractedText = "";
        this.extractionConfidence = 0.0;
        this.reviewRequired = false;
        this.extractedFields = Map.of();
    }

    public static SubmissionDocument uploaded(DocumentId id, String originalFilename, String mediaType, String storagePath) {
        return new SubmissionDocument(id, originalFilename, mediaType, storagePath);
    }

    public void completeProcessing(
            DocumentType documentType,
            String extractedText,
            double extractionConfidence,
            Map<String, String> extractedFields,
            boolean reviewRequired,
            PassportData passportData
    ) {
        this.documentType = documentType;
        this.extractedText = extractedText;
        this.extractionConfidence = extractionConfidence;
        this.extractedFields = extractedFields;
        this.reviewRequired = reviewRequired;
        this.passportData = passportData;
        this.status = reviewRequired ? DocumentStatus.REVIEW_REQUIRED : DocumentStatus.PROCESSED;
        this.updatedAt = Instant.now();
    }

    public DocumentId id() {
        return id;
    }

    public String originalFilename() {
        return originalFilename;
    }

    public String mediaType() {
        return mediaType;
    }

    public String storagePath() {
        return storagePath;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public DocumentType documentType() {
        return documentType;
    }

    public DocumentStatus status() {
        return status;
    }

    public String extractedText() {
        return extractedText;
    }

    public double extractionConfidence() {
        return extractionConfidence;
    }

    public boolean reviewRequired() {
        return reviewRequired;
    }

    public Map<String, String> extractedFields() {
        return extractedFields;
    }

    public Optional<PassportData> passportData() {
        return Optional.ofNullable(passportData);
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}

