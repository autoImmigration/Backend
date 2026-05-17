package com.yongsik.immigrationops.casework.domain;

public enum CaseDocumentType {
    APPLICATION_FORM("APPLICATION_FORM", "통합신청서", "신청", "ZIP 업로드 시 학생 구간 분리의 기준 문서"),
    PASSPORT_COPY("PASSPORT_COPY", "여권사본", "신원", "학생 이름, 국적, 생년월일 추출 1순위"),
    VISA_ISSUANCE_CERTIFICATE("VISA_ISSUANCE_CERTIFICATE", "사증발급서", "비자", "학교와 입국 목적 일치 여부 확인"),
    ENROLLMENT_CERTIFICATE("ENROLLMENT_CERTIFICATE", "재학증명서", "학교", "학적 상태와 신청 학기 일치 여부 확인"),
    REAL_ESTATE_CONTRACT("REAL_ESTATE_CONTRACT", "부동산계약서", "거주", "주소 추출 대상 문서"),
    ALIEN_REGISTRATION_CARD_COPY("ALIEN_REGISTRATION_CARD_COPY", "외국인등록증 사본", "신분", "기존 등록번호 및 체류자격 확인"),
    ATTENDANCE_CERTIFICATE("ATTENDANCE_CERTIFICATE", "출석증명서", "학교", "출석률 기준 확인 필요"),
    BANK_BALANCE_CERTIFICATE("BANK_BALANCE_CERTIFICATE", "은행잔고증명서", "재정", "재정 요건 충족 여부 확인"),
    REASON_STATEMENT("REASON_STATEMENT", "사유서", "보완", "연장 또는 변경 사유 검토 대상"),
    POWER_OF_ATTORNEY("POWER_OF_ATTORNEY", "위임장", "대리", "유학원 대리 접수 여부 확인"),
    ADVISOR_CONFIRMATION("ADVISOR_CONFIRMATION", "지도교수 확인서", "학교", "학업 지속 사유 확인 문서"),
    STANDARD_ADMISSION_LETTER("STANDARD_ADMISSION_LETTER", "표준입학허가서", "학교", "입학허가 정보와 신청 유형 매칭"),
    TUITION_PAYMENT_CONFIRMATION("TUITION_PAYMENT_CONFIRMATION", "등록금납부확인서", "재정", "등록금 납부 여부 확인"),
    FINAL_EDUCATION_CERTIFICATE("FINAL_EDUCATION_CERTIFICATE", "최종학력 인증서", "학력", "최종학력 증빙 문서"),
    FINAL_TRANSCRIPT("FINAL_TRANSCRIPT", "최종학력 성적표", "학력", "직전 학력 성적 확인"),
    LANGUAGE_SCHOOL_ENROLLMENT("LANGUAGE_SCHOOL_ENROLLMENT", "어학당 재학증명서", "학교", "어학당 재학 상태 확인"),
    LANGUAGE_SCHOOL_TRANSCRIPT("LANGUAGE_SCHOOL_TRANSCRIPT", "어학당 성적증명서", "학교", "어학당 이수 성적 확인");

    private final String code;
    private final String displayName;
    private final String category;
    private final String reviewRule;

    CaseDocumentType(String code, String displayName, String category, String reviewRule) {
        this.code = code;
        this.displayName = displayName;
        this.category = category;
        this.reviewRule = reviewRule;
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    public String category() {
        return category;
    }

    public String reviewRule() {
        return reviewRule;
    }
}
