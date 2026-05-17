package com.yongsik.immigrationops.casework.domain;

public enum ApplicationKind {
    NEW("신규 신청"),
    EXTENSION("연장 신청"),
    CHANGE("변경 신청"),
    CHANGE_AND_EXTENSION("변경 및 연장");

    private final String displayName;

    ApplicationKind(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
