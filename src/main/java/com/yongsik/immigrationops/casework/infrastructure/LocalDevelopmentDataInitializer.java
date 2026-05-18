package com.yongsik.immigrationops.casework.infrastructure;

import com.yongsik.immigrationops.casework.domain.ApplicationCase;
import com.yongsik.immigrationops.casework.domain.CaseDocument;
import com.yongsik.immigrationops.casework.domain.CaseDocumentType;
import com.yongsik.immigrationops.casework.domain.StudentRecord;
import com.yongsik.immigrationops.casework.domain.UploadBatch;
import com.yongsik.immigrationops.casework.domain.UploadBatchPreviewFile;
import com.yongsik.immigrationops.casework.domain.VisaType;
import com.yongsik.immigrationops.casework.infrastructure.persistence.ApplicationCaseEntity;
import com.yongsik.immigrationops.casework.infrastructure.persistence.ApplicationCaseJpaRepository;
import com.yongsik.immigrationops.casework.infrastructure.persistence.CaseDocumentRequirementEntity;
import com.yongsik.immigrationops.casework.infrastructure.persistence.CaseDocumentRequirementJpaRepository;
import com.yongsik.immigrationops.casework.infrastructure.persistence.DocumentTypeEntity;
import com.yongsik.immigrationops.casework.infrastructure.persistence.DocumentTypeJpaRepository;
import com.yongsik.immigrationops.casework.infrastructure.persistence.OrganizationEntity;
import com.yongsik.immigrationops.casework.infrastructure.persistence.OrganizationJpaRepository;
import com.yongsik.immigrationops.casework.infrastructure.persistence.OrganizationType;
import com.yongsik.immigrationops.casework.infrastructure.persistence.StudentEntity;
import com.yongsik.immigrationops.casework.infrastructure.persistence.StudentJpaRepository;
import com.yongsik.immigrationops.casework.infrastructure.persistence.UploadBatchEntity;
import com.yongsik.immigrationops.casework.infrastructure.persistence.UploadBatchJpaRepository;
import com.yongsik.immigrationops.casework.infrastructure.persistence.UploadBatchPreviewFileEntity;
import com.yongsik.immigrationops.casework.infrastructure.persistence.UploadBatchPreviewFileJpaRepository;
import com.yongsik.immigrationops.casework.infrastructure.persistence.VisaDocumentRequirementEntity;
import com.yongsik.immigrationops.casework.infrastructure.persistence.VisaDocumentRequirementJpaRepository;
import com.yongsik.immigrationops.casework.infrastructure.persistence.VisaTypeEntity;
import com.yongsik.immigrationops.casework.infrastructure.persistence.VisaTypeJpaRepository;
import com.yongsik.immigrationops.security.AppUserEntity;
import com.yongsik.immigrationops.security.AppUserJpaRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
public class LocalDevelopmentDataInitializer implements ApplicationRunner {

    private final OrganizationJpaRepository organizationJpaRepository;
    private final AppUserJpaRepository appUserJpaRepository;
    private final VisaTypeJpaRepository visaTypeJpaRepository;
    private final DocumentTypeJpaRepository documentTypeJpaRepository;
    private final VisaDocumentRequirementJpaRepository visaDocumentRequirementJpaRepository;
    private final StudentJpaRepository studentJpaRepository;
    private final ApplicationCaseJpaRepository applicationCaseJpaRepository;
    private final CaseDocumentRequirementJpaRepository caseDocumentRequirementJpaRepository;
    private final UploadBatchJpaRepository uploadBatchJpaRepository;
    private final UploadBatchPreviewFileJpaRepository uploadBatchPreviewFileJpaRepository;

    public LocalDevelopmentDataInitializer(
            OrganizationJpaRepository organizationJpaRepository,
            AppUserJpaRepository appUserJpaRepository,
            VisaTypeJpaRepository visaTypeJpaRepository,
            DocumentTypeJpaRepository documentTypeJpaRepository,
            VisaDocumentRequirementJpaRepository visaDocumentRequirementJpaRepository,
            StudentJpaRepository studentJpaRepository,
            ApplicationCaseJpaRepository applicationCaseJpaRepository,
            CaseDocumentRequirementJpaRepository caseDocumentRequirementJpaRepository,
            UploadBatchJpaRepository uploadBatchJpaRepository,
            UploadBatchPreviewFileJpaRepository uploadBatchPreviewFileJpaRepository
    ) {
        this.organizationJpaRepository = organizationJpaRepository;
        this.appUserJpaRepository = appUserJpaRepository;
        this.visaTypeJpaRepository = visaTypeJpaRepository;
        this.documentTypeJpaRepository = documentTypeJpaRepository;
        this.visaDocumentRequirementJpaRepository = visaDocumentRequirementJpaRepository;
        this.studentJpaRepository = studentJpaRepository;
        this.applicationCaseJpaRepository = applicationCaseJpaRepository;
        this.caseDocumentRequirementJpaRepository = caseDocumentRequirementJpaRepository;
        this.uploadBatchJpaRepository = uploadBatchJpaRepository;
        this.uploadBatchPreviewFileJpaRepository = uploadBatchPreviewFileJpaRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureDefaultSchools();

        if (applicationCaseJpaRepository.count() > 0 || uploadBatchJpaRepository.count() > 0) {
            return;
        }

        InMemoryCaseworkQueryRepository seedRepository = new InMemoryCaseworkQueryRepository();
        List<ApplicationCase> cases = seedRepository.findAllCases();
        List<UploadBatch> uploadBatches = seedRepository.findUploadBatches();

        Map<String, OrganizationEntity> schoolOrganizations = seedSchoolOrganizations(cases);
        Map<String, OrganizationEntity> agencyOrganizations = seedAgencyOrganizations(cases);
        seedUsers(schoolOrganizations, agencyOrganizations);

        Map<String, VisaTypeEntity> visaTypes = seedVisaTypes();
        Map<String, DocumentTypeEntity> documentTypes = seedDocumentTypes();
        Map<String, VisaDocumentRequirementEntity> visaRequirements = seedVisaRequirements(cases, visaTypes, documentTypes);
        Map<String, StudentEntity> students = seedStudents(cases, schoolOrganizations, agencyOrganizations);
        seedApplicationCases(cases, students, schoolOrganizations, agencyOrganizations, visaTypes, visaRequirements);
        seedUploadBatches(uploadBatches);
    }

    private void ensureDefaultSchools() {
        List<String> defaultSchools = List.of("Sejong University", "Inha University");
        for (String name : defaultSchools) {
            organizationJpaRepository.findByTypeAndName(OrganizationType.SCHOOL, name)
                    .orElseGet(() -> saveOrganization(OrganizationType.SCHOOL, name));
        }
    }

    private Map<String, OrganizationEntity> seedSchoolOrganizations(List<ApplicationCase> cases) {
        Map<String, OrganizationEntity> organizations = new LinkedHashMap<>();

        cases.stream()
                .map(ApplicationCase::schoolName)
                .distinct()
                .sorted()
                .forEach(name -> organizations.put(name, saveOrganization(OrganizationType.SCHOOL, name)));

        return organizations;
    }

    private Map<String, OrganizationEntity> seedAgencyOrganizations(List<ApplicationCase> cases) {
        Map<String, OrganizationEntity> organizations = new LinkedHashMap<>();

        cases.stream()
                .map(ApplicationCase::agencyName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .sorted()
                .forEach(name -> organizations.put(name, saveOrganization(OrganizationType.AGENCY, name)));

        return organizations;
    }

    private OrganizationEntity saveOrganization(OrganizationType type, String name) {
        OrganizationEntity entity = new OrganizationEntity();
        LocalDateTime timestamp = LocalDateTime.now();
        entity.setType(type);
        entity.setName(name);
        entity.setCreatedAt(timestamp);
        entity.setUpdatedAt(timestamp);
        return organizationJpaRepository.save(entity);
    }

    private void seedUsers(
            Map<String, OrganizationEntity> schoolOrganizations,
            Map<String, OrganizationEntity> agencyOrganizations
    ) {
        if (appUserJpaRepository.count() > 0) {
            return;
        }

        OrganizationEntity defaultSchool = schoolOrganizations.values().stream().findFirst().orElse(null);
        OrganizationEntity defaultAgency = agencyOrganizations.values().stream().findFirst().orElse(null);

        appUserJpaRepository.save(buildUser(null, "SYSTEM_ADMIN", "admin", "{noop}change-me", "System Admin"));
        appUserJpaRepository.save(buildUser(defaultSchool, "SCHOOL_ADMIN", "school-admin", "{noop}demo1234", "School Admin"));
        appUserJpaRepository.save(buildUser(defaultAgency, "AGENCY_ADMIN", "agency-ops", "{noop}demo1234", "Agency Ops"));
    }

    private AppUserEntity buildUser(
            OrganizationEntity organization,
            String role,
            String username,
            String passwordHash,
            String displayName
    ) {
        AppUserEntity entity = new AppUserEntity();
        LocalDateTime timestamp = LocalDateTime.now();
        entity.setOrganization(organization);
        entity.setRole(role);
        entity.setUsername(username);
        entity.setPasswordHash(passwordHash);
        entity.setDisplayName(displayName);
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(timestamp);
        entity.setUpdatedAt(timestamp);
        return entity;
    }

    private Map<String, VisaTypeEntity> seedVisaTypes() {
        Map<String, VisaTypeEntity> visaTypes = new LinkedHashMap<>();

        for (VisaType visaType : VisaType.values()) {
            VisaTypeEntity entity = new VisaTypeEntity();
            LocalDateTime timestamp = LocalDateTime.now();
            entity.setCode(visaType.name());
            entity.setName(visaType.displayName());
            entity.setDescription(visaType.displayName() + " case type");
            entity.setActive(true);
            entity.setCreatedAt(timestamp);
            entity.setUpdatedAt(timestamp);
            visaTypes.put(visaType.name(), visaTypeJpaRepository.save(entity));
        }

        return visaTypes;
    }

    private Map<String, DocumentTypeEntity> seedDocumentTypes() {
        Map<String, DocumentTypeEntity> documentTypes = new LinkedHashMap<>();

        for (CaseDocumentType documentType : CaseDocumentType.values()) {
            DocumentTypeEntity entity = new DocumentTypeEntity();
            LocalDateTime timestamp = LocalDateTime.now();
            entity.setCode(documentType.name());
            entity.setName(documentType.displayName());
            entity.setCategory(documentType.category());
            entity.setReviewRule(documentType.reviewRule());
            entity.setActive(true);
            entity.setCreatedAt(timestamp);
            entity.setUpdatedAt(timestamp);
            documentTypes.put(documentType.name(), documentTypeJpaRepository.save(entity));
        }

        return documentTypes;
    }

    private Map<String, VisaDocumentRequirementEntity> seedVisaRequirements(
            List<ApplicationCase> cases,
            Map<String, VisaTypeEntity> visaTypes,
            Map<String, DocumentTypeEntity> documentTypes
    ) {
        Map<String, VisaDocumentRequirementEntity> requirements = new LinkedHashMap<>();
        Map<VisaType, ApplicationCase> representativeCases = new LinkedHashMap<>();

        cases.stream()
                .sorted(Comparator.comparing(ApplicationCase::applicationDate))
                .forEach(applicationCase -> representativeCases.putIfAbsent(applicationCase.visaType(), applicationCase));

        for (Map.Entry<VisaType, ApplicationCase> entry : representativeCases.entrySet()) {
            List<CaseDocument> documents = entry.getValue().documents();

            for (int index = 0; index < documents.size(); index++) {
                CaseDocument document = documents.get(index);
                VisaDocumentRequirementEntity entity = new VisaDocumentRequirementEntity();
                LocalDateTime timestamp = LocalDateTime.now();
                entity.setVisaType(visaTypes.get(entry.getKey().name()));
                entity.setDocumentType(documentTypes.get(document.type().name()));
                entity.setRequired(true);
                entity.setDisplayOrder(index + 1);
                entity.setNote(document.type().reviewRule());
                entity.setCreatedAt(timestamp);
                entity.setUpdatedAt(timestamp);

                VisaDocumentRequirementEntity saved = visaDocumentRequirementJpaRepository.save(entity);
                requirements.put(requirementKey(entry.getKey().name(), document.type().name()), saved);
            }
        }

        return requirements;
    }

    private Map<String, StudentEntity> seedStudents(
            List<ApplicationCase> cases,
            Map<String, OrganizationEntity> schoolOrganizations,
            Map<String, OrganizationEntity> agencyOrganizations
    ) {
        Map<String, StudentRecord> uniqueStudents = new LinkedHashMap<>();

        for (ApplicationCase applicationCase : cases) {
            uniqueStudents.putIfAbsent(applicationCase.student().id(), applicationCase.student());
        }

        Map<String, StudentEntity> savedStudents = new LinkedHashMap<>();
        for (StudentRecord student : uniqueStudents.values()) {
            StudentEntity entity = new StudentEntity();
            LocalDateTime timestamp = LocalDateTime.now();
            entity.setExternalId(student.id());
            entity.setSchoolOrganization(schoolOrganizations.get(student.schoolName()));
            entity.setAgencyOrganization(student.agencyName() == null ? null : agencyOrganizations.get(student.agencyName()));
            entity.setName(student.name());
            entity.setNationality(student.nationality());
            entity.setBirthDate(student.birthDate());
            entity.setPassportNumber(student.passportNumber());
            entity.setSchoolDepartment(student.schoolDepartment());
            entity.setTerm(student.term());
            entity.setCreatedAt(timestamp);
            entity.setUpdatedAt(timestamp);

            savedStudents.put(student.id(), studentJpaRepository.save(entity));
        }

        return savedStudents;
    }

    private void seedApplicationCases(
            List<ApplicationCase> cases,
            Map<String, StudentEntity> students,
            Map<String, OrganizationEntity> schoolOrganizations,
            Map<String, OrganizationEntity> agencyOrganizations,
            Map<String, VisaTypeEntity> visaTypes,
            Map<String, VisaDocumentRequirementEntity> visaRequirements
    ) {
        for (ApplicationCase applicationCase : cases) {
            ApplicationCaseEntity entity = new ApplicationCaseEntity();
            entity.setExternalId(applicationCase.id());
            entity.setStudent(students.get(applicationCase.student().id()));
            entity.setSchoolOrganization(schoolOrganizations.get(applicationCase.schoolName()));
            entity.setAgencyOrganization(applicationCase.agencyName() == null ? null : agencyOrganizations.get(applicationCase.agencyName()));
            entity.setVisaType(visaTypes.get(applicationCase.visaType().name()));
            entity.setApplicationKind(applicationCase.applicationKind().name());
            entity.setStatus(applicationCase.status().name());
            entity.setApplicationDate(applicationCase.applicationDate());
            entity.setLane(applicationCase.lane());
            entity.setNote(applicationCase.note());
            entity.setIntakeBatch(applicationCase.intakeBatch());
            entity.setCoordinatorName(applicationCase.coordinatorName());
            entity.setSubmittedDocumentCount(applicationCase.submittedDocumentCount());
            entity.setMissingDocumentCount(applicationCase.missingDocumentCount());
            entity.setCreatedAt(applicationCase.updatedAt().minusDays(1));
            entity.setUpdatedAt(applicationCase.updatedAt());

            ApplicationCaseEntity savedCase = applicationCaseJpaRepository.save(entity);
            List<CaseDocumentRequirementEntity> requirementEntities = new ArrayList<>();

            for (int index = 0; index < applicationCase.documents().size(); index++) {
                CaseDocument document = applicationCase.documents().get(index);
                CaseDocumentRequirementEntity requirementEntity = new CaseDocumentRequirementEntity();
                requirementEntity.setApplicationCase(savedCase);
                requirementEntity.setDocumentType(documentTypeJpaRepository.findByCode(document.type().name()).orElseThrow());
                requirementEntity.setSourceRequirement(visaRequirements.get(requirementKey(applicationCase.visaType().name(), document.type().name())));
                requirementEntity.setRequired(true);
                requirementEntity.setDisplayOrder(index + 1);
                requirementEntity.setStatus(document.status().name());
                requirementEntity.setSubmittedAt(document.submittedAt());
                requirementEntity.setNote(document.note());
                requirementEntity.setPreviewText(document.preview());
                requirementEntity.setCreatedAt(applicationCase.updatedAt().minusDays(1));
                requirementEntity.setUpdatedAt(applicationCase.updatedAt());
                requirementEntities.add(requirementEntity);
            }

            caseDocumentRequirementJpaRepository.saveAll(requirementEntities);
        }
    }

    private void seedUploadBatches(List<UploadBatch> uploadBatches) {
        AppUserEntity uploadedByUser = appUserJpaRepository.findByUsername("agency-ops").orElse(null);

        for (UploadBatch uploadBatch : uploadBatches) {
            UploadBatchEntity entity = new UploadBatchEntity();
            entity.setExternalId(uploadBatch.id());
            entity.setUploadedByUser(uploadedByUser);
            entity.setOriginalFilename(uploadBatch.fileName());
            entity.setRawZipStorageType("LOCAL_FILE");
            entity.setRawZipLocation("seed/" + uploadBatch.id() + "/" + uploadBatch.fileName());
            entity.setRawZipChecksum("seed-" + uploadBatch.id().toLowerCase());
            entity.setRawZipSizeBytes((long) uploadBatch.previewFiles().size());
            entity.setStatus(uploadBatch.status().name());
            entity.setDetectedStudentCount(uploadBatch.studentCount());
            entity.setNote(uploadBatch.note());
            entity.setUploadedAt(uploadBatch.uploadedAt());
            entity.setCompletedAt(uploadBatch.status().name().equals("COMPLETED") ? uploadBatch.uploadedAt().plusMinutes(10) : null);
            entity.setCreatedAt(uploadBatch.uploadedAt());
            entity.setUpdatedAt(uploadBatch.uploadedAt());

            UploadBatchEntity savedBatch = uploadBatchJpaRepository.save(entity);
            List<UploadBatchPreviewFileEntity> previewFiles = new ArrayList<>();

            for (int index = 0; index < uploadBatch.previewFiles().size(); index++) {
                UploadBatchPreviewFile previewFile = uploadBatch.previewFiles().get(index);
                UploadBatchPreviewFileEntity previewEntity = new UploadBatchPreviewFileEntity();
                previewEntity.setExternalId(previewFile.id());
                previewEntity.setUploadBatch(savedBatch);
                previewEntity.setDisplayOrder(index + 1);
                previewEntity.setStudentName(previewFile.studentName());
                previewEntity.setDocumentName(previewFile.documentName());
                previewEntity.setPageRange(previewFile.pageRange());
                previewEntity.setNote(previewFile.note());
                previewEntity.setCreatedAt(uploadBatch.uploadedAt());
                previewFiles.add(previewEntity);
            }

            uploadBatchPreviewFileJpaRepository.saveAll(previewFiles);
        }
    }

    private String requirementKey(String visaTypeCode, String documentTypeCode) {
        return visaTypeCode + "::" + documentTypeCode;
    }
}
