package com.yongsik.immigrationops.casework.domain;

public enum VisaType {
    ALIEN_REGISTRATION("ALIEN_REGISTRATION", "외국인등록"),
    D2_EXTENSION("D2_EXTENSION", "D2연장"),
    D4_EXTENSION("D4_EXTENSION", "D4연장"),
    STATUS_CHANGE_AND_EXTENSION("STATUS_CHANGE_AND_EXTENSION", "세부체류자격 변경 및 연장"),
    D2_CHANGE("D2_CHANGE", "D2변경");

    private final String code;
    private final String displayName;

    VisaType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }
}
