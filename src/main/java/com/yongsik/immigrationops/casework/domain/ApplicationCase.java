package com.yongsik.immigrationops.casework.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ApplicationCase(
        String id,
        StudentRecord student,
        ApplicationKind applicationKind,
        VisaType visaType,
        LocalDate applicationDate,
        ApplicationCaseStatus status,
        String lane,
        String note,
        String schoolName,
        String agencyName,
        String coordinatorName,
        String intakeBatch,
        int submittedDocumentCount,
        int missingDocumentCount,
        LocalDateTime updatedAt,
        List<CaseDocument> documents
) {
    public ApplicationCase {
        documents = List.copyOf(documents);
    }

    public boolean isAgencyManaged() {
        return agencyName != null && !agencyName.isBlank();
    }
}
