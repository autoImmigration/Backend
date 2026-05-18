package com.yongsik.immigrationops.casework.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yongsik.immigrationops.casework.domain.ApplicationCaseStatus;
import com.yongsik.immigrationops.casework.domain.ApplicationKind;
import com.yongsik.immigrationops.casework.domain.ArtifactStorageType;
import com.yongsik.immigrationops.casework.domain.ProcessingJobStatus;
import com.yongsik.immigrationops.casework.domain.ProcessingJobType;
import com.yongsik.immigrationops.casework.domain.UploadBatchStatus;
import com.yongsik.immigrationops.casework.infrastructure.PythonOcrClient;
import com.yongsik.immigrationops.casework.infrastructure.persistence.ApplicationCaseEntity;
import com.yongsik.immigrationops.casework.infrastructure.persistence.ApplicationCaseJpaRepository;
import com.yongsik.immigrationops.casework.infrastructure.persistence.OrganizationJpaRepository;
import com.yongsik.immigrationops.casework.infrastructure.persistence.ProcessingJobEntity;
import com.yongsik.immigrationops.casework.infrastructure.persistence.ProcessingJobJpaRepository;
import com.yongsik.immigrationops.casework.infrastructure.persistence.StudentEntity;
import com.yongsik.immigrationops.casework.infrastructure.persistence.StudentJpaRepository;
import com.yongsik.immigrationops.casework.infrastructure.persistence.UploadBatchEntity;
import com.yongsik.immigrationops.casework.infrastructure.persistence.UploadBatchJpaRepository;
import com.yongsik.immigrationops.casework.infrastructure.persistence.VisaTypeEntity;
import com.yongsik.immigrationops.casework.infrastructure.persistence.VisaTypeJpaRepository;
import com.yongsik.immigrationops.common.BadRequestException;
import com.yongsik.immigrationops.security.AppUserEntity;
import com.yongsik.immigrationops.security.AppUserJpaRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import com.yongsik.immigrationops.casework.domain.CaseDocumentStatus;
import com.yongsik.immigrationops.casework.infrastructure.persistence.CaseDocumentRequirementEntity;
import com.yongsik.immigrationops.casework.infrastructure.persistence.CaseDocumentRequirementJpaRepository;
import com.yongsik.immigrationops.casework.infrastructure.persistence.DocumentTypeEntity;
import com.yongsik.immigrationops.casework.infrastructure.persistence.DocumentTypeJpaRepository;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AgencyCommandService {

    private static final Logger log = LoggerFactory.getLogger(AgencyCommandService.class);
    private static final DateTimeFormatter ID_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".bmp", ".webp", ".tif", ".tiff", ".pdf"
    );

    // Python 서류 코드 → Java CaseDocumentType 코드 매핑 (이름이 다른 것만 명시)
    private static final Map<String, String> PYTHON_TO_JAVA_DOC_CODE = Map.ofEntries(
            Map.entry("application_form", "APPLICATION_FORM"),
            Map.entry("passport_copy", "PASSPORT_COPY"),
            Map.entry("visa_issuance_certificate", "VISA_ISSUANCE_CERTIFICATE"),
            Map.entry("enrollment_certificate", "ENROLLMENT_CERTIFICATE"),
            Map.entry("real_estate_contract", "REAL_ESTATE_CONTRACT"),
            Map.entry("alien_registration_card_copy", "ALIEN_REGISTRATION_CARD_COPY"),
            Map.entry("attendance_certificate", "ATTENDANCE_CERTIFICATE"),
            Map.entry("bank_balance_certificate", "BANK_BALANCE_CERTIFICATE"),
            Map.entry("statement_of_reason", "REASON_STATEMENT"),
            Map.entry("power_of_attorney", "POWER_OF_ATTORNEY"),
            Map.entry("advisor_confirmation", "ADVISOR_CONFIRMATION"),
            Map.entry("admission_letter", "STANDARD_ADMISSION_LETTER"),
            Map.entry("tuition_payment_confirmation", "TUITION_PAYMENT_CONFIRMATION"),
            Map.entry("final_education_certificate", "FINAL_EDUCATION_CERTIFICATE"),
            Map.entry("final_education_transcript", "FINAL_TRANSCRIPT"),
            Map.entry("language_school_enrollment_certificate", "LANGUAGE_SCHOOL_ENROLLMENT"),
            Map.entry("language_school_transcript", "LANGUAGE_SCHOOL_TRANSCRIPT")
    );

    private final UploadBatchJpaRepository uploadBatchJpaRepository;
    private final ProcessingJobJpaRepository processingJobJpaRepository;
    private final AppUserJpaRepository appUserJpaRepository;
    private final OrganizationJpaRepository organizationJpaRepository;
    private final StudentJpaRepository studentJpaRepository;
    private final ApplicationCaseJpaRepository applicationCaseJpaRepository;
    private final VisaTypeJpaRepository visaTypeJpaRepository;
    private final DocumentTypeJpaRepository documentTypeJpaRepository;
    private final CaseDocumentRequirementJpaRepository caseDocumentRequirementJpaRepository;
    private final PythonOcrClient pythonOcrClient;

    @Value("${app.storage.root-path:data/uploads}")
    private String storageRootPath;

    @Value("${app.python-ocr.enabled:true}")
    private boolean pythonOcrEnabled;

    @Value("${app.backend.callback-base-url:http://localhost:8080}")
    private String backendCallbackBaseUrl;

    @Value("${INTERNAL_API_KEY:}")
    private String internalApiKey;

    @Value("${APP_OCR_PROVIDER:azure}")
    private String ocrProvider;

    public AgencyCommandService(
            UploadBatchJpaRepository uploadBatchJpaRepository,
            ProcessingJobJpaRepository processingJobJpaRepository,
            AppUserJpaRepository appUserJpaRepository,
            OrganizationJpaRepository organizationJpaRepository,
            StudentJpaRepository studentJpaRepository,
            ApplicationCaseJpaRepository applicationCaseJpaRepository,
            VisaTypeJpaRepository visaTypeJpaRepository,
            DocumentTypeJpaRepository documentTypeJpaRepository,
            CaseDocumentRequirementJpaRepository caseDocumentRequirementJpaRepository,
            PythonOcrClient pythonOcrClient
    ) {
        this.uploadBatchJpaRepository = uploadBatchJpaRepository;
        this.processingJobJpaRepository = processingJobJpaRepository;
        this.appUserJpaRepository = appUserJpaRepository;
        this.organizationJpaRepository = organizationJpaRepository;
        this.studentJpaRepository = studentJpaRepository;
        this.applicationCaseJpaRepository = applicationCaseJpaRepository;
        this.visaTypeJpaRepository = visaTypeJpaRepository;
        this.documentTypeJpaRepository = documentTypeJpaRepository;
        this.caseDocumentRequirementJpaRepository = caseDocumentRequirementJpaRepository;
        this.pythonOcrClient = pythonOcrClient;
    }

    public UploadBatchCommandResult createUploadBatch(String username, CreateUploadBatchCommand command) {
        AppUserEntity uploadedByUser = findActiveUser(username);
        ArtifactStorageType rawZipStorageType = ArtifactStorageType.from(command.rawZipStorageType());
        validateNonNegative(command.rawZipSizeBytes(), "rawZipSizeBytes");
        LocalDateTime now = LocalDateTime.now();

        UploadBatchEntity uploadBatch = new UploadBatchEntity();
        uploadBatch.setExternalId(nextExternalId("BATCH", now));
        uploadBatch.setUploadedByUser(uploadedByUser);
        uploadBatch.setOriginalFilename(normalizeRequired(command.originalFilename(), "originalFilename"));
        uploadBatch.setRawZipStorageType(rawZipStorageType.name());
        uploadBatch.setRawZipLocation(normalizeRequired(command.rawZipLocation(), "rawZipLocation"));
        uploadBatch.setRawZipChecksum(blankToNull(command.rawZipChecksum()));
        uploadBatch.setRawZipSizeBytes(command.rawZipSizeBytes());
        uploadBatch.setStatus(UploadBatchStatus.UPLOADED.name());
        uploadBatch.setDetectedStudentCount(0);
        uploadBatch.setNote(blankToNull(command.note()));
        uploadBatch.setVisaTypeCode(blankToNull(command.visaTypeCode()));
        uploadBatch.setUploadedAt(now);
        uploadBatch.setCreatedAt(now);
        uploadBatch.setUpdatedAt(now);

        // 학교 연결 (선택)
        if (!isBlank(command.schoolId())) {
            organizationJpaRepository.findById(Long.parseLong(command.schoolId()))
                    .ifPresent(uploadBatch::setSchoolOrganization);
        }

        UploadBatchEntity savedBatch = uploadBatchJpaRepository.save(uploadBatch);

        ProcessingJobEntity processingJob = new ProcessingJobEntity();
        processingJob.setUploadBatch(savedBatch);
        processingJob.setExternalId(nextExternalId("JOB", now));
        processingJob.setJobType(ProcessingJobType.OCR_BATCH.name());
        processingJob.setStatus(ProcessingJobStatus.QUEUED.name());
        processingJob.setAttemptNo(1);
        processingJob.setFileCount(0);
        processingJob.setCaseCount(0);
        processingJob.setErrorCount(0);
        processingJob.setCreatedAt(now);
        processingJob.setUpdatedAt(now);
        savedBatch.getProcessingJobs().add(processingJob);

        ProcessingJobEntity savedJob = processingJobJpaRepository.save(processingJob);

        // ZIP 압축 해제 및 Python 트리거
        if (pythonOcrEnabled && rawZipStorageType == ArtifactStorageType.LOCAL_FILE) {
            try {
                Path imagesDir = extractZip(savedBatch.getRawZipLocation(), savedBatch.getExternalId());
                Path outputDir = Paths.get(storageRootPath, "batches", savedBatch.getExternalId(), "output");
                Files.createDirectories(outputDir);

                savedBatch.setImagesDir(imagesDir.toAbsolutePath().toString());
                savedBatch.setOutputDir(outputDir.toAbsolutePath().toString());

                int fileCount = (int) Files.list(imagesDir)
                        .filter(p -> isAllowedImageFile(p.getFileName().toString()))
                        .count();
                savedJob.setFileCount(fileCount);
                savedJob.setStatus(ProcessingJobStatus.RUNNING.name());
                savedJob.setStartedAt(LocalDateTime.now());
                savedJob.setUpdatedAt(LocalDateTime.now());

                String callbackUrl = backendCallbackBaseUrl
                        + "/api/v1/internal/upload-batches/"
                        + savedBatch.getExternalId()
                        + "/python-results";

                pythonOcrClient.submitBatch(new PythonOcrClient.BatchRequest(
                        savedBatch.getExternalId(),
                        savedJob.getExternalId(),
                        imagesDir.toAbsolutePath().toString(),
                        outputDir.toAbsolutePath().toString(),
                        callbackUrl,
                        isBlank(internalApiKey) ? "" : internalApiKey,
                        ocrProvider,
                        null,
                        null,
                        60.0
                ));
            } catch (Exception e) {
                log.error("[createUploadBatch] ZIP 처리 또는 Python 트리거 실패 batch={}: {}",
                        savedBatch.getExternalId(), e.getMessage(), e);
                savedJob.setStatus(ProcessingJobStatus.FAILED.name());
                savedJob.setErrorCode("PYTHON_TRIGGER_FAILED");
                savedJob.setErrorMessage(e.getMessage());
                savedJob.setUpdatedAt(LocalDateTime.now());
                savedBatch.setStatus(UploadBatchStatus.FAILED.name());
                savedBatch.setFailureReason("Python OCR 서비스 연결 실패: " + e.getMessage());
                savedBatch.setUpdatedAt(LocalDateTime.now());
            }
        }

        return new UploadBatchCommandResult(
                savedBatch.getExternalId(),
                savedJob.getExternalId(),
                savedBatch.getStatus(),
                savedJob.getStatus(),
                savedBatch.getUploadedAt()
        );
    }

    public PythonResultIngestResult recordPythonResult(String uploadBatchId, RecordPythonBatchResultCommand command) {
        UploadBatchEntity uploadBatch = uploadBatchJpaRepository.findByExternalId(uploadBatchId)
                .orElseThrow(() -> new IllegalArgumentException("Upload batch not found: " + uploadBatchId));
        ProcessingJobEntity processingJob = processingJobJpaRepository.findByExternalId(
                        normalizeRequired(command.processingJobId(), "processingJobId")
                )
                .orElseThrow(() -> new IllegalArgumentException("Processing job not found: " + command.processingJobId()));

        if (!processingJob.getUploadBatch().getId().equals(uploadBatch.getId())) {
            throw new IllegalArgumentException("Processing job does not belong to upload batch: " + uploadBatchId);
        }

        ProcessingJobStatus processingJobStatus = ProcessingJobStatus.from(command.status());
        if (!processingJobStatus.isTerminalCallbackStatus()) {
            throw new BadRequestException("Python callback status must be SUCCEEDED, PARTIAL_SUCCESS, or FAILED");
        }

        validateArtifactReferences(processingJobStatus, command);
        validateNonNegative(command.fileCount(), "fileCount");
        validateNonNegative(command.caseCount(), "caseCount");
        validateNonNegative(command.errorCount(), "errorCount");

        try {
            if (processingJobStatus != ProcessingJobStatus.FAILED) {
                validateManifestAndCasesConsistency(command);
            }
        } catch (BadRequestException validationException) {
            log.error("[recordPythonResult] Manifest/cases validation failed for batch={}: {}",
                    uploadBatchId, validationException.getMessage());
            LocalDateTime failedAt = LocalDateTime.now();
            processingJob.setStatus(ProcessingJobStatus.FAILED.name());
            processingJob.setErrorCode("MANIFEST_VALIDATION_FAILED");
            processingJob.setErrorMessage(validationException.getMessage());
            processingJob.setUpdatedAt(failedAt);
            uploadBatch.setStatus(UploadBatchStatus.FAILED.name());
            uploadBatch.setFailureReason(validationException.getMessage());
            uploadBatch.setCompletedAt(failedAt);
            uploadBatch.setUpdatedAt(failedAt);
            return buildIngestResult(uploadBatch, processingJob);
        } catch (Exception unexpectedException) {
            log.error("[recordPythonResult] Unexpected error during Python result processing for batch={}: {}",
                    uploadBatchId, unexpectedException.getMessage(), unexpectedException);
            LocalDateTime failedAt = LocalDateTime.now();
            processingJob.setStatus(ProcessingJobStatus.FAILED.name());
            processingJob.setErrorCode("INTERNAL_PROCESSING_ERROR");
            processingJob.setErrorMessage("Unexpected error during result processing: " + unexpectedException.getMessage());
            processingJob.setUpdatedAt(failedAt);
            uploadBatch.setStatus(UploadBatchStatus.FAILED.name());
            uploadBatch.setFailureReason("Unexpected error: " + unexpectedException.getMessage());
            uploadBatch.setCompletedAt(failedAt);
            uploadBatch.setUpdatedAt(failedAt);
            return buildIngestResult(uploadBatch, processingJob);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startedAt = toLocalDateTime(command.startedAt());
        LocalDateTime finishedAt = command.finishedAt() == null ? now : toLocalDateTime(command.finishedAt());

        processingJob.setStatus(processingJobStatus.name());
        processingJob.setProvider(blankToNull(command.provider()));
        processingJob.setExternalJobId(blankToNull(command.externalJobId()));
        processingJob.setManifestStorageType(nullableStorageType(command.manifestStorageType()));
        processingJob.setManifestLocation(blankToNull(command.manifestLocation()));
        processingJob.setCasesStorageType(nullableStorageType(command.casesStorageType()));
        processingJob.setCasesLocation(blankToNull(command.casesLocation()));
        processingJob.setFileCount(command.fileCount());
        processingJob.setCaseCount(command.caseCount());
        processingJob.setErrorCount(command.errorCount());
        processingJob.setStartedAt(startedAt);
        processingJob.setFinishedAt(finishedAt);
        processingJob.setErrorCode(blankToNull(command.errorCode()));
        processingJob.setErrorMessage(blankToNull(command.errorMessage()));
        processingJob.setUpdatedAt(now);

        applyUploadBatchResult(uploadBatch, processingJobStatus, command, finishedAt, now);

        // cases.json 기반 학생/케이스 생성
        if (processingJobStatus != ProcessingJobStatus.FAILED
                && !isBlank(command.casesLocation())
                && uploadBatch.getSchoolOrganization() != null) {
            try {
                createStudentsAndCasesFromJson(uploadBatch, command.casesLocation());
            } catch (Exception e) {
                log.warn("[recordPythonResult] Student/case creation failed for batch={}: {}",
                        uploadBatchId, e.getMessage(), e);
            }
        } else if (uploadBatch.getSchoolOrganization() == null) {
            log.info("[recordPythonResult] school_id 미설정 - 학생/케이스 자동 생성 건너뜀 batch={}", uploadBatchId);
        }

        return buildIngestResult(uploadBatch, processingJob);
    }

    // ────────────────────────────────────────────────────────────────────────

    private Path extractZip(String zipLocation, String batchExternalId) throws IOException {
        Path zipPath = Paths.get(zipLocation);
        Path imagesDir = Paths.get(storageRootPath, "batches", batchExternalId, "images");
        Files.createDirectories(imagesDir);

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }
                String name = Paths.get(entry.getName()).getFileName().toString();
                if (!isAllowedImageFile(name)) {
                    zis.closeEntry();
                    continue;
                }
                Path targetPath = imagesDir.resolve(name);
                Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                zis.closeEntry();
            }
        }

        long count = Files.list(imagesDir)
                .filter(p -> isAllowedImageFile(p.getFileName().toString()))
                .count();
        log.info("[extractZip] batch={} → {} 이미지 파일 추출 완료: {}", batchExternalId, count, imagesDir);
        return imagesDir;
    }

    private boolean isAllowedImageFile(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        return ALLOWED_IMAGE_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    private void createStudentsAndCasesFromJson(UploadBatchEntity uploadBatch, String casesLocation) throws IOException {
        JsonNode casesRoot = objectMapper.readTree(Files.readAllBytes(Paths.get(casesLocation)));
        JsonNode casesList = casesRoot.path("cases");
        if (!casesList.isArray()) return;

        LocalDateTime now = LocalDateTime.now();
        // 서류 타입 캐시 (N+1 방지)
        Map<String, DocumentTypeEntity> docTypeCache = documentTypeJpaRepository.findAll()
                .stream().collect(Collectors.toMap(DocumentTypeEntity::getCode, e -> e));

        for (JsonNode caseNode : casesList) {
            try {
                JsonNode profile = caseNode.path("student_profile");
                String studentName = profile.path("student_name").asText("").trim();
                String nationality = profile.path("nationality").asText("").trim();
                String birthDateStr = profile.path("date_of_birth").asText("").trim();
                String passportNumber = profile.path("passport_number").asText("").trim();
                String alienRegNo = profile.path("alien_registration_number").asText("").trim();
                String phoneNumber = profile.path("phone_number").asText("").trim();
                String address = profile.path("address").asText("").trim();

                if (isBlank(studentName) || isBlank(nationality) || isBlank(birthDateStr)) {
                    log.warn("[createStudentsAndCases] 필수 필드 누락 (name/nationality/birthDate) - case={} 건너뜀",
                            caseNode.path("case_id").asText());
                    continue;
                }

                LocalDate birthDate = parseDate(birthDateStr);
                if (birthDate == null) {
                    log.warn("[createStudentsAndCases] 생년월일 파싱 실패 '{}' - case={} 건너뜀",
                            birthDateStr, caseNode.path("case_id").asText());
                    continue;
                }

                StudentEntity student = new StudentEntity();
                student.setExternalId(nextExternalId("STU", now));
                student.setSchoolOrganization(uploadBatch.getSchoolOrganization());
                student.setAgencyOrganization(
                        uploadBatch.getUploadedByUser() != null
                                ? uploadBatch.getUploadedByUser().getOrganization()
                                : null
                );
                student.setName(studentName);
                student.setNationality(nationality);
                student.setBirthDate(birthDate);
                student.setPassportNumber(isBlank(passportNumber) ? "UNKNOWN" : passportNumber);
                student.setAlienRegistrationNumber(isBlank(alienRegNo) ? null : alienRegNo);
                student.setPhoneNumber(isBlank(phoneNumber) ? null : phoneNumber);
                student.setAddress(isBlank(address) ? null : address);
                student.setCreatedAt(now);
                student.setUpdatedAt(now);

                StudentEntity savedStudent = studentJpaRepository.save(student);

                // Python 코드(소문자) → Java 코드(대문자)로 정규화
                JsonNode applicationTypeNode = caseNode.path("application_type");
                String pythonTypeCode = applicationTypeNode.path("code").asText("").trim();
                String javaTypeCode = normalizeAppTypeCode(pythonTypeCode);

                // Python 추론 실패 시 배치에서 선택한 타입 사용
                if (isBlank(javaTypeCode) || "UNKNOWN_APPLICATION_TYPE".equals(javaTypeCode)) {
                    javaTypeCode = blankToNull(uploadBatch.getVisaTypeCode());
                }

                Optional<VisaTypeEntity> visaTypeOpt = isBlank(javaTypeCode)
                        ? Optional.empty()
                        : visaTypeJpaRepository.findByCode(javaTypeCode);

                if (visaTypeOpt.isEmpty()) {
                    log.info("[createStudentsAndCases] visa_type '{}' 찾을 수 없음 - case={} ApplicationCase 생성 건너뜀",
                            javaTypeCode, caseNode.path("case_id").asText());
                    continue;
                }

                // 제출 서류 코드 목록 (Python → Java 변환)
                Set<String> submittedJavaCodes = new HashSet<>();
                JsonNode documents = caseNode.path("documents");
                if (documents.isArray()) {
                    for (JsonNode doc : documents) {
                        String pythonDocCode = doc.path("document_code").asText("").trim();
                        String javaDocCode = toJavaDocCode(pythonDocCode);
                        if (!isBlank(javaDocCode)) submittedJavaCodes.add(javaDocCode);
                    }
                }

                // missing_required_documents 목록 (Python → Java 변환)
                List<String> missingJavaCodes = new ArrayList<>();
                JsonNode missingNodes = applicationTypeNode.path("missing_required_documents");
                if (missingNodes.isArray()) {
                    for (JsonNode m : missingNodes) {
                        String javaDocCode = toJavaDocCode(m.asText("").trim());
                        if (!isBlank(javaDocCode)) missingJavaCodes.add(javaDocCode);
                    }
                }

                int submittedCount = submittedJavaCodes.size();
                int missingCount = missingJavaCodes.size();

                ApplicationCaseEntity applicationCase = new ApplicationCaseEntity();
                applicationCase.setExternalId(nextExternalId("CASE", now));
                applicationCase.setStudent(savedStudent);
                applicationCase.setSchoolOrganization(uploadBatch.getSchoolOrganization());
                applicationCase.setAgencyOrganization(savedStudent.getAgencyOrganization());
                applicationCase.setVisaType(visaTypeOpt.get());
                applicationCase.setApplicationKind(inferApplicationKind(javaTypeCode).name());
                applicationCase.setStatus(ApplicationCaseStatus.DRAFT.name());
                applicationCase.setApplicationDate(uploadBatch.getUploadedAt().toLocalDate());
                applicationCase.setSubmittedDocumentCount(submittedCount);
                applicationCase.setMissingDocumentCount(missingCount);
                applicationCase.setIntakeBatch(uploadBatch.getExternalId());
                applicationCase.setCreatedAt(now);
                applicationCase.setUpdatedAt(now);

                ApplicationCaseEntity savedCase = applicationCaseJpaRepository.save(applicationCase);

                // 제출 서류 요건 엔티티 생성
                int order = 1;
                for (String javaDocCode : submittedJavaCodes) {
                    DocumentTypeEntity docType = docTypeCache.get(javaDocCode);
                    if (docType == null) continue;
                    CaseDocumentRequirementEntity req = new CaseDocumentRequirementEntity();
                    req.setApplicationCase(savedCase);
                    req.setDocumentType(docType);
                    req.setRequired(true);
                    req.setDisplayOrder(order++);
                    req.setStatus(CaseDocumentStatus.SUBMITTED.name());
                    req.setSubmittedAt(uploadBatch.getUploadedAt().toLocalDate());
                    req.setCreatedAt(now);
                    req.setUpdatedAt(now);
                    caseDocumentRequirementJpaRepository.save(req);
                }
                // 누락 서류 요건 엔티티 생성
                for (String javaDocCode : missingJavaCodes) {
                    DocumentTypeEntity docType = docTypeCache.get(javaDocCode);
                    if (docType == null) continue;
                    CaseDocumentRequirementEntity req = new CaseDocumentRequirementEntity();
                    req.setApplicationCase(savedCase);
                    req.setDocumentType(docType);
                    req.setRequired(true);
                    req.setDisplayOrder(order++);
                    req.setStatus(CaseDocumentStatus.NOT_SUBMITTED.name());
                    req.setCreatedAt(now);
                    req.setUpdatedAt(now);
                    caseDocumentRequirementJpaRepository.save(req);
                }

                log.info("[createStudentsAndCases] 학생/케이스 생성 완료 student={} case={} 제출={} 누락={}",
                        savedStudent.getExternalId(), savedCase.getExternalId(), submittedCount, missingCount);

            } catch (Exception e) {
                log.warn("[createStudentsAndCases] case={} 처리 중 오류: {}",
                        caseNode.path("case_id").asText(), e.getMessage());
            }
        }
    }

    private String normalizeAppTypeCode(String pythonCode) {
        if (isBlank(pythonCode)) return null;
        return pythonCode.trim().toUpperCase(Locale.ROOT);
    }

    private String toJavaDocCode(String pythonDocCode) {
        if (isBlank(pythonDocCode)) return null;
        return PYTHON_TO_JAVA_DOC_CODE.getOrDefault(pythonDocCode.trim().toLowerCase(Locale.ROOT), null);
    }

    private ApplicationKind inferApplicationKind(String applicationTypeCode) {
        if (isBlank(applicationTypeCode)) return ApplicationKind.NEW;
        String code = applicationTypeCode.toLowerCase(Locale.ROOT);
        if (code.contains("change_and_extension") || code.contains("change_extension")) return ApplicationKind.CHANGE_AND_EXTENSION;
        if (code.contains("change")) return ApplicationKind.CHANGE;
        if (code.contains("extension")) return ApplicationKind.EXTENSION;
        return ApplicationKind.NEW;
    }

    private LocalDate parseDate(String dateStr) {
        if (isBlank(dateStr)) return null;
        try {
            return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    // ────────────────────────────────────────────────────────────────────────

    private void validateManifestAndCasesConsistency(RecordPythonBatchResultCommand command) {
        String manifestLocation = blankToNull(command.manifestLocation());
        String casesLocation = blankToNull(command.casesLocation());

        if (manifestLocation == null || casesLocation == null) return;

        String manifestStorageType = blankToNull(command.manifestStorageType());
        if (!"LOCAL".equalsIgnoreCase(manifestStorageType)) return;

        try {
            JsonNode manifest = objectMapper.readTree(Files.readAllBytes(Paths.get(manifestLocation)));
            JsonNode cases = objectMapper.readTree(Files.readAllBytes(Paths.get(casesLocation)));

            int manifestFileCount = manifest.path("file_count").asInt(-1);
            int casesGlobalDocumentCount = cases.path("global_document_count").asInt(-1);

            if (manifestFileCount >= 0 && casesGlobalDocumentCount >= 0
                    && manifestFileCount != casesGlobalDocumentCount) {
                throw new BadRequestException(
                        "Manifest/cases mismatch: manifest.file_count=" + manifestFileCount
                                + " but cases.global_document_count=" + casesGlobalDocumentCount
                );
            }

            Set<Integer> manifestOrders = new HashSet<>();
            JsonNode manifestResults = manifest.path("results");
            if (manifestResults.isArray()) {
                for (JsonNode result : manifestResults) {
                    JsonNode orderNode = result.path("global_order");
                    if (orderNode.isMissingNode()) {
                        orderNode = result.path("order");
                    }
                    if (!orderNode.isMissingNode()) {
                        manifestOrders.add(orderNode.asInt());
                    }
                }
            }

            if (!manifestOrders.isEmpty()) {
                JsonNode casesList = cases.path("cases");
                if (casesList.isArray()) {
                    for (JsonNode caseNode : casesList) {
                        JsonNode documents = caseNode.path("documents");
                        if (documents.isArray()) {
                            for (JsonNode doc : documents) {
                                JsonNode globalOrderNode = doc.path("global_order");
                                if (!globalOrderNode.isMissingNode()) {
                                    int globalOrder = globalOrderNode.asInt();
                                    if (!manifestOrders.contains(globalOrder)) {
                                        throw new BadRequestException(
                                                "cases.json document global_order=" + globalOrder
                                                        + " not found in manifest.results"
                                        );
                                    }
                                }
                            }
                        }
                    }
                }
            }

        } catch (BadRequestException exception) {
            throw exception;
        } catch (IOException ioException) {
            log.warn("[validateManifestAndCasesConsistency] Could not read artifact files, skipping validation: {}",
                    ioException.getMessage());
        }
    }

    private void applyUploadBatchResult(
            UploadBatchEntity uploadBatch,
            ProcessingJobStatus processingJobStatus,
            RecordPythonBatchResultCommand command,
            LocalDateTime finishedAt,
            LocalDateTime updatedAt
    ) {
        switch (processingJobStatus) {
            case SUCCEEDED -> {
                uploadBatch.setStatus(UploadBatchStatus.RESULT_UPLOADED.name());
                uploadBatch.setFailureReason(null);
            }
            case PARTIAL_SUCCESS -> {
                uploadBatch.setStatus(UploadBatchStatus.NEEDS_REVIEW.name());
                uploadBatch.setFailureReason(null);
            }
            case FAILED -> {
                uploadBatch.setStatus(UploadBatchStatus.FAILED.name());
                uploadBatch.setFailureReason(
                        blankToNull(command.errorMessage()) == null
                                ? "Python batch callback reported FAILED"
                                : command.errorMessage().trim()
                );
            }
            default -> throw new IllegalArgumentException("Unsupported processing job status: " + processingJobStatus.name());
        }

        if (processingJobStatus != ProcessingJobStatus.FAILED) {
            uploadBatch.setDetectedStudentCount(command.caseCount());
        }

        uploadBatch.setCompletedAt(finishedAt);
        uploadBatch.setUpdatedAt(updatedAt);
    }

    private PythonResultIngestResult buildIngestResult(UploadBatchEntity batch, ProcessingJobEntity job) {
        return new PythonResultIngestResult(
                batch.getExternalId(),
                job.getExternalId(),
                batch.getStatus(),
                job.getStatus(),
                batch.getCompletedAt()
        );
    }

    private void validateArtifactReferences(ProcessingJobStatus status, RecordPythonBatchResultCommand command) {
        if (status == ProcessingJobStatus.FAILED) return;

        if (isBlank(command.manifestStorageType())
                || isBlank(command.manifestLocation())
                || isBlank(command.casesStorageType())
                || isBlank(command.casesLocation())) {
            throw new BadRequestException(
                    "Manifest and cases artifact references are required unless status is FAILED"
            );
        }
    }

    private AppUserEntity findActiveUser(String username) {
        return appUserJpaRepository.findByUsername(username)
                .filter(user -> "ACTIVE".equalsIgnoreCase(user.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("Active user not found: " + username));
    }

    private String nullableStorageType(String value) {
        return isBlank(value) ? null : ArtifactStorageType.from(value).name();
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private String normalizeRequired(String value, String fieldName) {
        if (isBlank(value)) throw new BadRequestException(fieldName + " is required");
        return value.trim();
    }

    private void validateNonNegative(long value, String fieldName) {
        if (value < 0) throw new BadRequestException(fieldName + " must be zero or greater");
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String nextExternalId(String prefix, LocalDateTime timestamp) {
        int randomSuffix = ThreadLocalRandom.current().nextInt(0x100000, 0x1000000);
        return (prefix + "-" + ID_TIME_FORMAT.format(timestamp) + "-" + Integer.toHexString(randomSuffix))
                .toUpperCase(Locale.ROOT);
    }
}
