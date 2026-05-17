package com.yongsik.immigrationops.casework.domain;

public enum CaseDocumentStatus {
    NOT_SUBMITTED("미제출"),
    SUBMITTED("제출"),
    NEEDS_REVIEW("검수 필요"),
    NEEDS_SUPPLEMENT("보완 필요"),
    APPROVED("승인");

    private final String displayName;

    CaseDocumentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
