package com.yongsik.immigrationops.casework.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "application_case")
public class ApplicationCaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true, length = 64)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private OrganizationEntity schoolOrganization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    private OrganizationEntity agencyOrganization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visa_type_id", nullable = false)
    private VisaTypeEntity visaType;

    @Column(name = "application_kind", nullable = false, length = 64)
    private String applicationKind;

    @Column(nullable = false, length = 64)
    private String status;

    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;

    @Column(length = 160)
    private String lane;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "intake_batch", length = 160)
    private String intakeBatch;

    @Column(name = "coordinator_name", length = 120)
    private String coordinatorName;

    @Column(name = "submitted_document_count", nullable = false)
    private int submittedDocumentCount;

    @Column(name = "missing_document_count", nullable = false)
    private int missingDocumentCount;

    @Column(name = "other_document_filenames", columnDefinition = "text")
    private String otherDocumentFilenames;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "applicationCase")
    @OrderBy("displayOrder ASC")
    private List<CaseDocumentRequirementEntity> documents = new ArrayList<>();

    public ApplicationCaseEntity() {
    }

    public Long getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public StudentEntity getStudent() {
        return student;
    }

    public void setStudent(StudentEntity student) {
        this.student = student;
    }

    public OrganizationEntity getSchoolOrganization() {
        return schoolOrganization;
    }

    public void setSchoolOrganization(OrganizationEntity schoolOrganization) {
        this.schoolOrganization = schoolOrganization;
    }

    public OrganizationEntity getAgencyOrganization() {
        return agencyOrganization;
    }

    public void setAgencyOrganization(OrganizationEntity agencyOrganization) {
        this.agencyOrganization = agencyOrganization;
    }

    public VisaTypeEntity getVisaType() {
        return visaType;
    }

    public void setVisaType(VisaTypeEntity visaType) {
        this.visaType = visaType;
    }

    public String getApplicationKind() {
        return applicationKind;
    }

    public void setApplicationKind(String applicationKind) {
        this.applicationKind = applicationKind;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(LocalDate applicationDate) {
        this.applicationDate = applicationDate;
    }

    public String getLane() {
        return lane;
    }

    public void setLane(String lane) {
        this.lane = lane;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getIntakeBatch() {
        return intakeBatch;
    }

    public void setIntakeBatch(String intakeBatch) {
        this.intakeBatch = intakeBatch;
    }

    public String getCoordinatorName() {
        return coordinatorName;
    }

    public void setCoordinatorName(String coordinatorName) {
        this.coordinatorName = coordinatorName;
    }

    public int getSubmittedDocumentCount() {
        return submittedDocumentCount;
    }

    public void setSubmittedDocumentCount(int submittedDocumentCount) {
        this.submittedDocumentCount = submittedDocumentCount;
    }

    public int getMissingDocumentCount() {
        return missingDocumentCount;
    }

    public void setMissingDocumentCount(int missingDocumentCount) {
        this.missingDocumentCount = missingDocumentCount;
    }

    public String getOtherDocumentFilenames() {
        return otherDocumentFilenames;
    }

    public void setOtherDocumentFilenames(String otherDocumentFilenames) {
        this.otherDocumentFilenames = otherDocumentFilenames;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<CaseDocumentRequirementEntity> getDocuments() {
        return documents;
    }
}
