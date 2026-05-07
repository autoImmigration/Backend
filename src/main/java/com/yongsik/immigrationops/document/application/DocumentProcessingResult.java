package com.yongsik.immigrationops.document.application;

import com.yongsik.immigrationops.document.domain.DocumentStatus;
import com.yongsik.immigrationops.document.domain.DocumentType;
import com.yongsik.immigrationops.passport.domain.PassportData;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record DocumentProcessingResult(
        String id,
        String originalFilename,
        String mediaType,
        String storagePath,
        DocumentType documentType,
        DocumentStatus status,
        double extractionConfidence,
        boolean reviewRequired,
        String extractedText,
        Map<String, String> extractedFields,
        PassportData passportData,
        List<String> expectedFields,
        Instant createdAt,
        Instant updatedAt
) {
}

