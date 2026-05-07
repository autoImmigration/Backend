package com.yongsik.immigrationops.classification.domain;

import com.yongsik.immigrationops.document.domain.DocumentType;
import java.util.List;

public record ClassificationResult(
        DocumentType documentType,
        double confidence,
        List<String> evidence
) {
}

