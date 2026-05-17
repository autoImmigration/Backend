package com.yongsik.immigrationops.casework.infrastructure.persistence;

import com.yongsik.immigrationops.security.AppUserEntity;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "upload_batch")
public class UploadBatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true, length = 64)
    private String externalId;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id")
    private AppUserEntity uploadedByUser;

    @Column(name = "raw_zip_storage_type", length = 32)
    private String rawZipStorageType;

    @Column(name = "raw_zip_location", length = 500)
    private String rawZipLocation;

    @Column(name = "raw_zip_checksum", length = 120)
    private String rawZipChecksum;

    @Column(name = "raw_zip_size_bytes")
    private Long rawZipSizeBytes;

    @Column(nullable = false, length = 64)
    private String status;

    @Column(name = "detected_student_count", nullable = false)
    private int detectedStudentCount;

    @Column(columnDefinition = "text")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private OrganizationEntity schoolOrganization;

    @Column(name = "images_dir", length = 500)
    private String imagesDir;

    @Column(name = "output_dir", length = 500)
    private String outputDir;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "uploadBatch")
    @OrderBy("displayOrder ASC")
    private List<UploadBatchPreviewFileEntity> previewFiles = new ArrayList<>();

    @OneToMany(mappedBy = "uploadBatch")
    @OrderBy("attemptNo DESC, createdAt DESC")
    private List<ProcessingJobEntity> processingJobs = new ArrayList<>();

    public UploadBatchEntity() {
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

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public AppUserEntity getUploadedByUser() {
        return uploadedByUser;
    }

    public void setUploadedByUser(AppUserEntity uploadedByUser) {
        this.uploadedByUser = uploadedByUser;
    }

    public String getRawZipStorageType() {
        return rawZipStorageType;
    }

    public void setRawZipStorageType(String rawZipStorageType) {
        this.rawZipStorageType = rawZipStorageType;
    }

    public String getRawZipLocation() {
        return rawZipLocation;
    }

    public void setRawZipLocation(String rawZipLocation) {
        this.rawZipLocation = rawZipLocation;
    }

    public String getRawZipChecksum() {
        return rawZipChecksum;
    }

    public void setRawZipChecksum(String rawZipChecksum) {
        this.rawZipChecksum = rawZipChecksum;
    }

    public Long getRawZipSizeBytes() {
        return rawZipSizeBytes;
    }

    public void setRawZipSizeBytes(Long rawZipSizeBytes) {
        this.rawZipSizeBytes = rawZipSizeBytes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getDetectedStudentCount() {
        return detectedStudentCount;
    }

    public void setDetectedStudentCount(int detectedStudentCount) {
        this.detectedStudentCount = detectedStudentCount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
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

    public OrganizationEntity getSchoolOrganization() {
        return schoolOrganization;
    }

    public void setSchoolOrganization(OrganizationEntity schoolOrganization) {
        this.schoolOrganization = schoolOrganization;
    }

    public String getImagesDir() {
        return imagesDir;
    }

    public void setImagesDir(String imagesDir) {
        this.imagesDir = imagesDir;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public List<UploadBatchPreviewFileEntity> getPreviewFiles() {
        return previewFiles;
    }

    public List<ProcessingJobEntity> getProcessingJobs() {
        return processingJobs;
    }
}
