package com.yongsik.immigrationops.casework.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "processing_job")
public class ProcessingJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_batch_id", nullable = false)
    private UploadBatchEntity uploadBatch;

    @Column(name = "external_id", nullable = false, unique = true, length = 64)
    private String externalId;

    @Column(name = "job_type", nullable = false, length = 32)
    private String jobType;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(length = 64)
    private String provider;

    @Column(name = "external_job_id", length = 255)
    private String externalJobId;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Column(name = "manifest_storage_type", length = 32)
    private String manifestStorageType;

    @Column(name = "manifest_location", length = 500)
    private String manifestLocation;

    @Column(name = "cases_storage_type", length = 32)
    private String casesStorageType;

    @Column(name = "cases_location", length = 500)
    private String casesLocation;

    @Column(name = "file_count", nullable = false)
    private int fileCount;

    @Column(name = "case_count", nullable = false)
    private int caseCount;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ProcessingJobEntity() {
    }

    public Long getId() {
        return id;
    }

    public UploadBatchEntity getUploadBatch() {
        return uploadBatch;
    }

    public void setUploadBatch(UploadBatchEntity uploadBatch) {
        this.uploadBatch = uploadBatch;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getExternalJobId() {
        return externalJobId;
    }

    public void setExternalJobId(String externalJobId) {
        this.externalJobId = externalJobId;
    }

    public int getAttemptNo() {
        return attemptNo;
    }

    public void setAttemptNo(int attemptNo) {
        this.attemptNo = attemptNo;
    }

    public String getManifestStorageType() {
        return manifestStorageType;
    }

    public void setManifestStorageType(String manifestStorageType) {
        this.manifestStorageType = manifestStorageType;
    }

    public String getManifestLocation() {
        return manifestLocation;
    }

    public void setManifestLocation(String manifestLocation) {
        this.manifestLocation = manifestLocation;
    }

    public String getCasesStorageType() {
        return casesStorageType;
    }

    public void setCasesStorageType(String casesStorageType) {
        this.casesStorageType = casesStorageType;
    }

    public String getCasesLocation() {
        return casesLocation;
    }

    public void setCasesLocation(String casesLocation) {
        this.casesLocation = casesLocation;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public int getCaseCount() {
        return caseCount;
    }

    public void setCaseCount(int caseCount) {
        this.caseCount = caseCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
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
}
