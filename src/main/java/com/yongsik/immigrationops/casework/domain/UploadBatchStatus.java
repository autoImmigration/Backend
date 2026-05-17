package com.yongsik.immigrationops.casework.domain;

public enum UploadBatchStatus {
    UPLOADED("업로드됨"),
    VALIDATING("검증 중"),
    RUNNING("처리 중"),
    RESULT_UPLOADED("결과 업로드 완료"),
    FINALIZING("DB 반영 중"),
    COMPLETED("완료"),
    NEEDS_REVIEW("보완"),
    FAILED("실패");

    private final String displayName;

    UploadBatchStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
