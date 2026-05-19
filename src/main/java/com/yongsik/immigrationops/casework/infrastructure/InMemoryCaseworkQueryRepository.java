package com.yongsik.immigrationops.casework.infrastructure;

import com.yongsik.immigrationops.casework.domain.ApplicationCase;
import com.yongsik.immigrationops.casework.domain.ApplicationCaseStatus;
import com.yongsik.immigrationops.casework.domain.ApplicationKind;
import com.yongsik.immigrationops.casework.domain.CaseDocument;
import com.yongsik.immigrationops.casework.domain.CaseDocumentStatus;
import com.yongsik.immigrationops.casework.domain.CaseDocumentType;
import com.yongsik.immigrationops.casework.domain.CaseworkQueryRepository;
import com.yongsik.immigrationops.casework.domain.StudentLookupCriteria;
import com.yongsik.immigrationops.casework.domain.StudentRecord;
import com.yongsik.immigrationops.casework.domain.UploadBatch;
import com.yongsik.immigrationops.casework.domain.UploadBatchPreviewFile;
import com.yongsik.immigrationops.casework.domain.UploadBatchStatus;
import com.yongsik.immigrationops.casework.domain.VisaType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
public class InMemoryCaseworkQueryRepository implements CaseworkQueryRepository {

    private final Map<String, StudentRecord> students;
    private final Map<String, ApplicationCase> applicationCases;
    private final Map<String, UploadBatch> uploadBatches;

    public InMemoryCaseworkQueryRepository() {
        this.students = seedStudents();
        this.applicationCases = seedApplicationCases(this.students);
        this.uploadBatches = seedUploadBatches();
    }

    @Override
    public Optional<StudentRecord> findStudentByLookup(StudentLookupCriteria criteria) {
        return students.values().stream()
                .filter(student -> matches(student, criteria))
                .findFirst();
    }

    @Override
    public List<ApplicationCase> findStudentCases(String studentId) {
        return applicationCases.values().stream()
                .filter(applicationCase -> applicationCase.student().id().equals(studentId))
                .toList();
    }

    @Override
    public List<ApplicationCase> findAllCases() {
        return List.copyOf(applicationCases.values());
    }

    @Override
    public Optional<ApplicationCase> findCaseById(String caseId) {
        return Optional.ofNullable(applicationCases.get(caseId));
    }

    @Override
    public List<UploadBatch> findUploadBatches() {
        return List.copyOf(uploadBatches.values());
    }

    @Override
    public Optional<UploadBatch> findUploadBatchById(String batchId) {
        return Optional.ofNullable(uploadBatches.get(batchId));
    }

    @Override
    public List<ApplicationCase> findCasesByBatchId(String batchId) {
        return findAllCases().stream()
                .filter(c -> batchId.equals(c.intakeBatch()))
                .toList();
    }

    private boolean matches(StudentRecord student, StudentLookupCriteria criteria) {
        StudentLookupCriteria normalizedStudent = new StudentLookupCriteria(
                student.nationality(),
                student.passportNumber(),
                student.birthDate()
        ).normalized();

        return normalizedStudent.nationality().equals(criteria.nationality())
                && normalizedStudent.passportNumber().equals(criteria.passportNumber())
                && normalizedStudent.birthDate().equals(criteria.birthDate());
    }

    private Map<String, StudentRecord> seedStudents() {
        Map<String, StudentRecord> seeded = new LinkedHashMap<>();
        seeded.put("STU-1001", student(
                "STU-1001",
                "린응옥안",
                "베트남",
                "M38492017",
                date(2002, 11, 14),
                "한빛대학교",
                "국제학부",
                "글로벌브릿지 유학원",
                "2026 봄학기"
        ));
        seeded.put("STU-1002", student(
                "STU-1002",
                "장웨이",
                "중국",
                "E82746102",
                date(2001, 3, 5),
                "한빛대학교",
                "경영학과",
                "동방에듀",
                "2026 봄학기"
        ));
        seeded.put("STU-1003", student(
                "STU-1003",
                "바트에르덴",
                "몽골",
                "N55290114",
                date(2000, 7, 19),
                "한빛대학교 한국어교육원",
                "한국어교육원",
                "스텝인코리아",
                "2026 여름학기"
        ));
        seeded.put("STU-1004", student(
                "STU-1004",
                "사토 미유",
                "일본",
                "T10928411",
                date(1999, 12, 3),
                "한빛대학교 국제처",
                "국제처",
                null,
                "학사편입 / 자격변경"
        ));
        seeded.put("STU-1005", student(
                "STU-1005",
                "응우옌티하",
                "베트남",
                "M92017444",
                date(2003, 1, 22),
                "한빛대학교 한국어교육원",
                "한국어교육원",
                "글로벌브릿지 유학원",
                "2026 여름학기"
        ));
        return Map.copyOf(seeded);
    }

    private Map<String, ApplicationCase> seedApplicationCases(Map<String, StudentRecord> students) {
        Map<String, ApplicationCase> seeded = new LinkedHashMap<>();

        StudentRecord linh = students.get("STU-1001");
        seeded.put("APP-2026-0412-001", applicationCase(
                "APP-2026-0412-001",
                linh,
                ApplicationKind.NEW,
                VisaType.ALIEN_REGISTRATION,
                date(2026, 4, 12),
                ApplicationCaseStatus.NEEDS_SUPPLEMENT,
                "학부 정규과정",
                "부동산계약서가 없어 주소 추출을 못 해서 보완 대기 상태입니다.",
                "한빛대학교",
                "글로벌브릿지 유학원",
                "이소정",
                "2026 봄학기 / 학부",
                4,
                1,
                time(2026, 4, 14, 9, 20),
                buildDocuments(VisaType.ALIEN_REGISTRATION, overrides(
                        submitted(CaseDocumentType.APPLICATION_FORM, date(2026, 4, 10), "학생 구간 분리 기준 문서로 인식", "통합신청서 1-3페이지, 연락처 항목 기재 완료"),
                        submitted(CaseDocumentType.PASSPORT_COPY, date(2026, 4, 10), "이름, 국적, 생년월일 추출 완료", "여권 사진면 스캔본"),
                        submitted(CaseDocumentType.VISA_ISSUANCE_CERTIFICATE, date(2026, 4, 9), "발급 번호 확인 완료", null),
                        submitted(CaseDocumentType.ENROLLMENT_CERTIFICATE, date(2026, 4, 9), "재학 상태 확인", null),
                        pending(CaseDocumentType.REAL_ESTATE_CONTRACT, "체류지 주소 추출 대상 문서 필요", "부동산계약서가 제출되면 주소 추출 결과가 표시됩니다.")
                ))
        ));

        seeded.put("APP-2026-0227-001", applicationCase(
                "APP-2026-0227-001",
                linh,
                ApplicationKind.EXTENSION,
                VisaType.D2_EXTENSION,
                date(2026, 2, 27),
                ApplicationCaseStatus.COMPLETED,
                "학부 체류 연장",
                "필수 서류 제출 및 검토 완료",
                "한빛대학교",
                "글로벌브릿지 유학원",
                "이소정",
                "2026 봄학기 / 학부",
                9,
                0,
                time(2026, 2, 27, 15, 0),
                buildDocuments(VisaType.D2_EXTENSION, overrides(
                        submitted(CaseDocumentType.APPLICATION_FORM, date(2026, 2, 20), "통합신청서 OCR 완료", null),
                        submitted(CaseDocumentType.PASSPORT_COPY, date(2026, 2, 20), "여권번호 자동 인식 성공", null),
                        submitted(CaseDocumentType.ALIEN_REGISTRATION_CARD_COPY, date(2026, 2, 19), "등록번호 확인 완료", null),
                        submitted(CaseDocumentType.ENROLLMENT_CERTIFICATE, date(2026, 2, 19), "학적 확인 완료", null),
                        submitted(CaseDocumentType.ATTENDANCE_CERTIFICATE, date(2026, 2, 18), "출석률 기준 충족", null),
                        submitted(CaseDocumentType.BANK_BALANCE_CERTIFICATE, date(2026, 2, 18), "재정 요건 충족", null),
                        submitted(CaseDocumentType.REASON_STATEMENT, date(2026, 2, 17), "연장 사유 확인", null),
                        submitted(CaseDocumentType.REAL_ESTATE_CONTRACT, date(2026, 2, 17), "주소 추출 완료", null),
                        submitted(CaseDocumentType.POWER_OF_ATTORNEY, date(2026, 2, 17), "대리 접수 확인", null)
                ))
        ));

        seeded.put("APP-2025-1218-001", applicationCase(
                "APP-2025-1218-001",
                linh,
                ApplicationKind.CHANGE,
                VisaType.D2_CHANGE,
                date(2025, 12, 18),
                ApplicationCaseStatus.NEEDS_SUPPLEMENT,
                "어학당 -> 학위과정 변경",
                "어학당 성적증명서와 부동산계약서가 누락되어 보완 요청 상태입니다.",
                "한빛대학교",
                "글로벌브릿지 유학원",
                "이소정",
                "2025 겨울학기 / 진학전환",
                10,
                2,
                time(2025, 12, 18, 10, 15),
                buildDocuments(VisaType.D2_CHANGE, overrides(
                        submitted(CaseDocumentType.APPLICATION_FORM, date(2025, 12, 11), "통합신청서 기준 구간 인식 완료", null),
                        submitted(CaseDocumentType.PASSPORT_COPY, date(2025, 12, 11), "여권값 우선 매칭 완료", null),
                        submitted(CaseDocumentType.ALIEN_REGISTRATION_CARD_COPY, date(2025, 12, 10), "기존 체류자격 확인", null),
                        submitted(CaseDocumentType.STANDARD_ADMISSION_LETTER, date(2025, 12, 10), "입학허가서 확인", null),
                        submitted(CaseDocumentType.TUITION_PAYMENT_CONFIRMATION, date(2025, 12, 9), "등록금 납부 확인", null),
                        submitted(CaseDocumentType.FINAL_EDUCATION_CERTIFICATE, date(2025, 12, 9), "최종학력 증빙 확보", null),
                        submitted(CaseDocumentType.FINAL_TRANSCRIPT, date(2025, 12, 8), "성적표 확인", null),
                        submitted(CaseDocumentType.LANGUAGE_SCHOOL_ENROLLMENT, date(2025, 12, 8), "어학당 재학 상태 확인", null),
                        pending(CaseDocumentType.LANGUAGE_SCHOOL_TRANSCRIPT, "어학당 성적 증빙 필요", null),
                        submitted(CaseDocumentType.BANK_BALANCE_CERTIFICATE, date(2025, 12, 8), "재정 요건 충족", null),
                        pending(CaseDocumentType.REAL_ESTATE_CONTRACT, "주소 추출 대상 누락", null),
                        submitted(CaseDocumentType.POWER_OF_ATTORNEY, date(2025, 12, 7), "대리 접수 확인", null)
                ))
        ));

        StudentRecord zhang = students.get("STU-1002");
        seeded.put("APP-2026-0411-001", applicationCase(
                "APP-2026-0411-001",
                zhang,
                ApplicationKind.EXTENSION,
                VisaType.D2_EXTENSION,
                date(2026, 4, 11),
                ApplicationCaseStatus.COMPLETED,
                "학부 체류 연장",
                "필수 서류 제출 및 검토 완료",
                "한빛대학교",
                "동방에듀",
                "박유진",
                "2026 봄학기 / 경영학과",
                9,
                0,
                time(2026, 4, 11, 13, 40),
                markAllSubmitted(VisaType.D2_EXTENSION, date(2026, 4, 8), "서류 검토 완료")
        ));

        StudentRecord bat = students.get("STU-1003");
        seeded.put("APP-2026-0413-001", applicationCase(
                "APP-2026-0413-001",
                bat,
                ApplicationKind.EXTENSION,
                VisaType.D4_EXTENSION,
                date(2026, 4, 13),
                ApplicationCaseStatus.NEEDS_SUPPLEMENT,
                "어학당 체류 연장",
                "지도교수 확인서와 위임장이 누락되어 보완 상태입니다.",
                "한빛대학교 한국어교육원",
                "스텝인코리아",
                "정민재",
                "2026 여름학기 / 어학당",
                8,
                2,
                time(2026, 4, 13, 16, 40),
                buildDocuments(VisaType.D4_EXTENSION, overrides(
                        submitted(CaseDocumentType.APPLICATION_FORM, date(2026, 4, 9), "통합신청서 인식 완료", null),
                        submitted(CaseDocumentType.PASSPORT_COPY, date(2026, 4, 9), "여권번호 자동 추출", null),
                        submitted(CaseDocumentType.ALIEN_REGISTRATION_CARD_COPY, date(2026, 4, 8), "등록증 확인", null),
                        submitted(CaseDocumentType.ENROLLMENT_CERTIFICATE, date(2026, 4, 8), "재학 상태 확인", null),
                        submitted(CaseDocumentType.ATTENDANCE_CERTIFICATE, date(2026, 4, 8), "출석률 확인", null),
                        submitted(CaseDocumentType.BANK_BALANCE_CERTIFICATE, date(2026, 4, 8), "잔고 확인", null),
                        submitted(CaseDocumentType.REASON_STATEMENT, date(2026, 4, 7), "연장 사유 확인", null),
                        pending(CaseDocumentType.ADVISOR_CONFIRMATION, "지도교수 확인 필요", null),
                        submitted(CaseDocumentType.REAL_ESTATE_CONTRACT, date(2026, 4, 7), "주소 추출 완료", "부동산계약서에서 서울시 동대문구 주소 추출"),
                        pending(CaseDocumentType.POWER_OF_ATTORNEY, "위임장 누락", null)
                ))
        ));

        StudentRecord sato = students.get("STU-1004");
        seeded.put("APP-2026-0410-001", applicationCase(
                "APP-2026-0410-001",
                sato,
                ApplicationKind.CHANGE_AND_EXTENSION,
                VisaType.STATUS_CHANGE_AND_EXTENSION,
                date(2026, 4, 10),
                ApplicationCaseStatus.COMPLETED,
                "학사편입 / 자격변경",
                "필수 서류 제출 및 검토 완료",
                "한빛대학교 국제처",
                null,
                "직접 신청",
                "학사편입 / 자격변경",
                11,
                0,
                time(2026, 4, 10, 11, 10),
                buildDocuments(VisaType.STATUS_CHANGE_AND_EXTENSION, overrides(
                        submitted(CaseDocumentType.APPLICATION_FORM, date(2026, 4, 2), "학생 구간 분리 완료", null),
                        submitted(CaseDocumentType.PASSPORT_COPY, date(2026, 4, 2), "기본 인적사항 추출 완료", null),
                        submitted(CaseDocumentType.ALIEN_REGISTRATION_CARD_COPY, date(2026, 4, 1), "등록증 확인", null),
                        submitted(CaseDocumentType.STANDARD_ADMISSION_LETTER, date(2026, 4, 1), "입학 허가 확인", null),
                        submitted(CaseDocumentType.TUITION_PAYMENT_CONFIRMATION, date(2026, 3, 31), "등록금 납부 확인", null),
                        submitted(CaseDocumentType.FINAL_EDUCATION_CERTIFICATE, date(2026, 3, 31), "최종학력 인증 완료", null),
                        submitted(CaseDocumentType.FINAL_TRANSCRIPT, date(2026, 3, 31), "성적표 확인 완료", null),
                        submitted(CaseDocumentType.BANK_BALANCE_CERTIFICATE, date(2026, 3, 30), "재정 요건 충족", null),
                        submitted(CaseDocumentType.REASON_STATEMENT, date(2026, 3, 30), "변경 사유 확인", null),
                        submitted(CaseDocumentType.REAL_ESTATE_CONTRACT, date(2026, 3, 30), "주소 추출 완료", null),
                        submitted(CaseDocumentType.POWER_OF_ATTORNEY, date(2026, 3, 30), "위임장 제출 완료", null)
                ))
        ));

        StudentRecord nguyen = students.get("STU-1005");
        seeded.put("APP-2026-0412-002", applicationCase(
                "APP-2026-0412-002",
                nguyen,
                ApplicationKind.CHANGE,
                VisaType.D2_CHANGE,
                date(2026, 4, 12),
                ApplicationCaseStatus.NEEDS_SUPPLEMENT,
                "어학당 -> 학위과정 변경",
                "어학당 성적증명서와 부동산계약서가 누락되어 보완 요청 상태입니다.",
                "한빛대학교 한국어교육원",
                "글로벌브릿지 유학원",
                "정민재",
                "2026 여름학기 / 어학당",
                10,
                2,
                time(2026, 4, 12, 16, 10),
                buildDocuments(VisaType.D2_CHANGE, overrides(
                        submitted(CaseDocumentType.APPLICATION_FORM, date(2026, 4, 10), "통합신청서 기준 구간 인식 완료", null),
                        submitted(CaseDocumentType.PASSPORT_COPY, date(2026, 4, 10), "여권값 우선 매칭 완료", null),
                        submitted(CaseDocumentType.ALIEN_REGISTRATION_CARD_COPY, date(2026, 4, 9), "기존 체류자격 확인", null),
                        submitted(CaseDocumentType.STANDARD_ADMISSION_LETTER, date(2026, 4, 9), "입학허가서 확인", null),
                        submitted(CaseDocumentType.TUITION_PAYMENT_CONFIRMATION, date(2026, 4, 8), "등록금 납부 확인", null),
                        submitted(CaseDocumentType.FINAL_EDUCATION_CERTIFICATE, date(2026, 4, 8), "최종학력 증빙 확보", null),
                        submitted(CaseDocumentType.FINAL_TRANSCRIPT, date(2026, 4, 8), "성적표 확인", null),
                        submitted(CaseDocumentType.LANGUAGE_SCHOOL_ENROLLMENT, date(2026, 4, 8), "어학당 재학 상태 확인", null),
                        pending(CaseDocumentType.LANGUAGE_SCHOOL_TRANSCRIPT, "어학당 성적 증빙 필요", null),
                        submitted(CaseDocumentType.BANK_BALANCE_CERTIFICATE, date(2026, 4, 8), "재정 요건 충족", null),
                        pending(CaseDocumentType.REAL_ESTATE_CONTRACT, "주소 추출 대상 누락", null),
                        submitted(CaseDocumentType.POWER_OF_ATTORNEY, date(2026, 4, 7), "대리 접수 확인", null)
                ))
        ));

        return Map.copyOf(seeded);
    }

    private Map<String, UploadBatch> seedUploadBatches() {
        Map<String, UploadBatch> seeded = new LinkedHashMap<>();
        seeded.put("BATCH-2026-0414-A", new UploadBatch(
                "BATCH-2026-0414-A",
                "hanbit_spring_batch_a.zip",
                time(2026, 4, 14, 8, 50),
                12,
                UploadBatchStatus.NEEDS_REVIEW,
                "부동산계약서 2건 누락, 통합신청서 1건 재분류 필요",
                List.of(
                        previewFile("SCAN-0414-01", "리응우안", "통합신청서", "1-3페이지", "학생 구간 첫 문서로 인식되어 케이스 시작점으로 분류됨"),
                        previewFile("SCAN-0414-02", "리응우안", "여권사본", "4페이지", "이름, 국적, 생년월일을 우선 추출한 여권 스캔본"),
                        previewFile("SCAN-0414-03", "왕치엔", "재학증명서", "11페이지", "학교명과 재학 상태가 읽혀 다음 문서와 같은 학생으로 연결됨"),
                        previewFile("SCAN-0414-04", "왕치엔", "부동산계약서", "15-16페이지", "주소 추출 대상 문서지만 서명 영역만 선명해 운영자 확인 필요")
                )
        ));
        seeded.put("BATCH-2026-0412-B", new UploadBatch(
                "BATCH-2026-0412-B",
                "language_center_extension.zip",
                time(2026, 4, 12, 14, 10),
                8,
                UploadBatchStatus.COMPLETED,
                "여권 우선 추출과 통합신청서 보완까지 처리 완료",
                List.of(
                        previewFile("SCAN-0412-01", "정우타티", "통합신청서", "1-2페이지", "ZIP 업로드 후 첫 학생 구간 문서로 분류 완료"),
                        previewFile("SCAN-0412-02", "정우타티", "여권사본", "3페이지", "여권번호와 생년월일 추출이 정상 완료됨"),
                        previewFile("SCAN-0412-03", "정우타티", "외국인등록증 사본", "4페이지", "기존 체류 자격과 등록번호 확인에 사용됨"),
                        previewFile("SCAN-0412-04", "정우타티", "출석증명서", "7페이지", "출석률 확인 필드가 정상 추출되어 완료 상태로 마감")
                )
        ));
        seeded.put("BATCH-2026-0410-C", new UploadBatch(
                "BATCH-2026-0410-C",
                "status_change_group_01.zip",
                time(2026, 4, 10, 10, 25),
                5,
                UploadBatchStatus.NEEDS_REVIEW,
                "어학당 성적증명서 누락 1건, 주소 추출 확인 필요 1건",
                List.of(
                        previewFile("SCAN-0410-01", "사토 미유", "통합신청서", "1-2페이지", "체류자격 변경 케이스의 시작 문서로 분류됨"),
                        previewFile("SCAN-0410-02", "사토 미유", "표준입학허가서", "5페이지", "학교 정보와 과정명이 신청 타입과 일치함"),
                        previewFile("SCAN-0410-03", "사토 미유", "최종학력 성적표", "8-10페이지", "성적표는 읽혔지만 어학당 성적증명서는 확인되지 않음"),
                        previewFile("SCAN-0410-04", "사토 미유", "부동산계약서", "12페이지", "주소 필드 추출값이 낮은 신뢰도로 표시되어 검수 대기 중")
                )
        ));
        return Map.copyOf(seeded);
    }

    private StudentRecord student(
            String id,
            String name,
            String nationality,
            String passportNumber,
            LocalDate birthDate,
            String schoolName,
            String schoolDepartment,
            String agencyName,
            String term
    ) {
        return new StudentRecord(id, name, nationality, passportNumber, birthDate, schoolName, schoolDepartment, agencyName, term);
    }

    private ApplicationCase applicationCase(
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
        return new ApplicationCase(
                id,
                student,
                applicationKind,
                visaType,
                applicationDate,
                status,
                lane,
                note,
                schoolName,
                agencyName,
                coordinatorName,
                intakeBatch,
                submittedDocumentCount,
                missingDocumentCount,
                updatedAt,
                documents,
                List.of()
        );
    }

    private List<CaseDocument> buildDocuments(VisaType visaType, Map<CaseDocumentType, DocumentSeed> overrides) {
        return requiredDocuments(visaType).stream()
                .map(documentType -> {
                    DocumentSeed seed = overrides.get(documentType);
                    if (seed == null) {
                        return new CaseDocument(
                                documentType,
                                CaseDocumentStatus.NOT_SUBMITTED,
                                null,
                                "확인 대기",
                                documentType.displayName() + " 스캔본이 선택되면 이 영역에 미리보기와 OCR 요약이 표시됩니다.",
                                null
                        );
                    }
                    return new CaseDocument(
                            documentType,
                            seed.status(),
                            seed.submittedAt(),
                            seed.note(),
                            seed.preview() == null
                                    ? documentType.displayName() + " 스캔본이 선택되면 이 영역에 미리보기와 OCR 요약이 표시됩니다."
                                    : seed.preview(),
                            null
                    );
                })
                .toList();
    }

    private List<CaseDocument> markAllSubmitted(VisaType visaType, LocalDate submittedAt, String note) {
        return requiredDocuments(visaType).stream()
                .map(documentType -> new CaseDocument(
                        documentType,
                        CaseDocumentStatus.SUBMITTED,
                        submittedAt,
                        note,
                        documentType.displayName() + " 스캔본 미리보기 준비 완료",
                        null
                ))
                .toList();
    }

    private List<CaseDocumentType> requiredDocuments(VisaType visaType) {
        return switch (visaType) {
            case ALIEN_REGISTRATION -> List.of(
                    CaseDocumentType.APPLICATION_FORM,
                    CaseDocumentType.PASSPORT_COPY,
                    CaseDocumentType.VISA_ISSUANCE_CERTIFICATE,
                    CaseDocumentType.ENROLLMENT_CERTIFICATE,
                    CaseDocumentType.REAL_ESTATE_CONTRACT
            );
            case D2_EXTENSION -> List.of(
                    CaseDocumentType.APPLICATION_FORM,
                    CaseDocumentType.PASSPORT_COPY,
                    CaseDocumentType.ALIEN_REGISTRATION_CARD_COPY,
                    CaseDocumentType.ENROLLMENT_CERTIFICATE,
                    CaseDocumentType.ATTENDANCE_CERTIFICATE,
                    CaseDocumentType.BANK_BALANCE_CERTIFICATE,
                    CaseDocumentType.REASON_STATEMENT,
                    CaseDocumentType.REAL_ESTATE_CONTRACT,
                    CaseDocumentType.POWER_OF_ATTORNEY
            );
            case D4_EXTENSION -> List.of(
                    CaseDocumentType.APPLICATION_FORM,
                    CaseDocumentType.PASSPORT_COPY,
                    CaseDocumentType.ALIEN_REGISTRATION_CARD_COPY,
                    CaseDocumentType.ENROLLMENT_CERTIFICATE,
                    CaseDocumentType.ATTENDANCE_CERTIFICATE,
                    CaseDocumentType.BANK_BALANCE_CERTIFICATE,
                    CaseDocumentType.REASON_STATEMENT,
                    CaseDocumentType.ADVISOR_CONFIRMATION,
                    CaseDocumentType.REAL_ESTATE_CONTRACT,
                    CaseDocumentType.POWER_OF_ATTORNEY
            );
            case STATUS_CHANGE_AND_EXTENSION -> List.of(
                    CaseDocumentType.APPLICATION_FORM,
                    CaseDocumentType.PASSPORT_COPY,
                    CaseDocumentType.ALIEN_REGISTRATION_CARD_COPY,
                    CaseDocumentType.STANDARD_ADMISSION_LETTER,
                    CaseDocumentType.TUITION_PAYMENT_CONFIRMATION,
                    CaseDocumentType.FINAL_EDUCATION_CERTIFICATE,
                    CaseDocumentType.FINAL_TRANSCRIPT,
                    CaseDocumentType.BANK_BALANCE_CERTIFICATE,
                    CaseDocumentType.REASON_STATEMENT,
                    CaseDocumentType.REAL_ESTATE_CONTRACT,
                    CaseDocumentType.POWER_OF_ATTORNEY
            );
            case D2_CHANGE -> List.of(
                    CaseDocumentType.APPLICATION_FORM,
                    CaseDocumentType.PASSPORT_COPY,
                    CaseDocumentType.ALIEN_REGISTRATION_CARD_COPY,
                    CaseDocumentType.STANDARD_ADMISSION_LETTER,
                    CaseDocumentType.TUITION_PAYMENT_CONFIRMATION,
                    CaseDocumentType.FINAL_EDUCATION_CERTIFICATE,
                    CaseDocumentType.FINAL_TRANSCRIPT,
                    CaseDocumentType.LANGUAGE_SCHOOL_ENROLLMENT,
                    CaseDocumentType.LANGUAGE_SCHOOL_TRANSCRIPT,
                    CaseDocumentType.BANK_BALANCE_CERTIFICATE,
                    CaseDocumentType.REAL_ESTATE_CONTRACT,
                    CaseDocumentType.POWER_OF_ATTORNEY
            );
        };
    }

    @SafeVarargs
    private final Map<CaseDocumentType, DocumentSeed> overrides(DocumentSeed... seeds) {
        Map<CaseDocumentType, DocumentSeed> values = new EnumMap<>(CaseDocumentType.class);
        for (DocumentSeed seed : seeds) {
            values.put(seed.type(), seed);
        }
        return Map.copyOf(values);
    }

    private DocumentSeed submitted(CaseDocumentType type, LocalDate submittedAt, String note, String preview) {
        return new DocumentSeed(type, CaseDocumentStatus.SUBMITTED, submittedAt, note, preview);
    }

    private DocumentSeed pending(CaseDocumentType type, String note, String preview) {
        return new DocumentSeed(type, CaseDocumentStatus.NOT_SUBMITTED, null, note, preview);
    }

    private UploadBatchPreviewFile previewFile(String id, String studentName, String documentName, String pageRange, String note) {
        return new UploadBatchPreviewFile(id, studentName, documentName, pageRange, note);
    }

    private LocalDate date(int year, int month, int dayOfMonth) {
        return LocalDate.of(year, month, dayOfMonth);
    }

    private LocalDateTime time(int year, int month, int dayOfMonth, int hour, int minute) {
        return LocalDateTime.of(year, month, dayOfMonth, hour, minute);
    }

    private record DocumentSeed(
            CaseDocumentType type,
            CaseDocumentStatus status,
            LocalDate submittedAt,
            String note,
            String preview
    ) {
    }
}
