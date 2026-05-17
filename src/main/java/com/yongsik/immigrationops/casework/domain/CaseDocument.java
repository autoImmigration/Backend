package com.yongsik.immigrationops.casework.domain;

import java.time.LocalDate;

public record CaseDocument(
        CaseDocumentType type,
        CaseDocumentStatus status,
        LocalDate submittedAt,
        String note,
        String preview
) {
}
