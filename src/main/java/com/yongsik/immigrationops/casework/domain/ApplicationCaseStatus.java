package com.yongsik.immigrationops.casework.domain;

public enum ApplicationCaseStatus {
    DRAFT("임시"),
    SUBMITTED("접수"),
    RECEIVED("접수 확인"),
    NEEDS_REVIEW("검수 필요"),
    NEEDS_SUPPLEMENT("보완"),
    COMPLETED("완료"),
    REJECTED("반려");

    private final String displayName;

    ApplicationCaseStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
