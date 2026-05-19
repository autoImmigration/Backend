package com.yongsik.immigrationops.casework.infrastructure.persistence;

import com.yongsik.immigrationops.casework.domain.ApplicationCase;
import com.yongsik.immigrationops.casework.domain.ApplicationCaseStatus;
import com.yongsik.immigrationops.casework.domain.ApplicationKind;
import com.yongsik.immigrationops.casework.domain.CaseDocument;
import com.yongsik.immigrationops.casework.domain.CaseDocumentStatus;
import com.yongsik.immigrationops.casework.domain.CaseDocumentType;
import com.yongsik.immigrationops.casework.domain.ProcessingJobStatus;
import com.yongsik.immigrationops.casework.domain.ProcessingJobType;
import com.yongsik.immigrationops.casework.domain.StudentRecord;
import com.yongsik.immigrationops.casework.domain.UploadBatch;
import com.yongsik.immigrationops.casework.domain.UploadBatchProcessingJob;
import com.yongsik.immigrationops.casework.domain.UploadBatchPreviewFile;
import com.yongsik.immigrationops.casework.domain.UploadBatchStatus;
import com.yongsik.immigrationops.casework.domain.VisaType;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class CaseworkJpaMapper {

    StudentRecord toStudentRecord(StudentEntity entity) {
        return new StudentRecord(
                entity.getExternalId(),
                entity.getName(),
                entity.getNationality(),
                entity.getPassportNumber(),
                entity.getBirthDate(),
                entity.getSchoolOrganization().getName(),
                entity.getSchoolDepartment(),
                entity.getAgencyOrganization() == null ? null : entity.getAgencyOrganization().getName(),
                entity.getTerm()
        );
    }

    ApplicationCase toApplicationCase(ApplicationCaseEntity entity) {
        return new ApplicationCase(
                entity.getExternalId(),
                toStudentRecord(entity.getStudent()),
                ApplicationKind.valueOf(entity.getApplicationKind()),
                VisaType.valueOf(entity.getVisaType().getCode()),
                entity.getApplicationDate(),
                ApplicationCaseStatus.valueOf(entity.getStatus()),
                entity.getLane(),
                entity.getNote(),
                entity.getSchoolOrganization().getName(),
                entity.getAgencyOrganization() == null ? null : entity.getAgencyOrganization().getName(),
                entity.getCoordinatorName(),
                entity.getIntakeBatch(),
                entity.getSubmittedDocumentCount(),
                entity.getMissingDocumentCount(),
                entity.getUpdatedAt(),
                entity.getDocuments().stream().map(this::toCaseDocument).toList()
        );
    }

    UploadBatch toUploadBatch(UploadBatchEntity entity) {
        return new UploadBatch(
                entity.getExternalId(),
                entity.getOriginalFilename(),
                entity.getUploadedAt(),
                entity.getDetectedStudentCount(),
                UploadBatchStatus.valueOf(entity.getStatus()),
                entity.getNote() == null ? entity.getFailureReason() : entity.getNote(),
                entity.getProcessingJobs().isEmpty() ? null : toProcessingJob(entity.getProcessingJobs().getFirst()),
                entity.getPreviewFiles().stream().map(this::toPreviewFile).toList()
        );
    }

    private CaseDocument toCaseDocument(CaseDocumentRequirementEntity entity) {
        CaseDocumentType documentType = CaseDocumentType.valueOf(entity.getDocumentType().getCode());

        return new CaseDocument(
                documentType,
                CaseDocumentStatus.valueOf(entity.getStatus()),
                entity.getSubmittedAt(),
                entity.getNote(),
                entity.getPreviewText() == null ? defaultPreview(documentType) : entity.getPreviewText(),
                entity.getSourceFilename()
        );
    }

    private UploadBatchPreviewFile toPreviewFile(UploadBatchPreviewFileEntity entity) {
        return new UploadBatchPreviewFile(
                entity.getExternalId(),
                entity.getStudentName(),
                entity.getDocumentName(),
                entity.getPageRange(),
                entity.getNote()
        );
    }

    private UploadBatchProcessingJob toProcessingJob(ProcessingJobEntity entity) {
        return new UploadBatchProcessingJob(
                entity.getExternalId(),
                ProcessingJobType.valueOf(entity.getJobType()),
                ProcessingJobStatus.valueOf(entity.getStatus()),
                entity.getAttemptNo(),
                entity.getProvider(),
                entity.getExternalJobId(),
                entity.getFileCount(),
                entity.getCaseCount(),
                entity.getErrorCount(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getStartedAt(),
                entity.getFinishedAt()
        );
    }

    private String defaultPreview(CaseDocumentType documentType) {
        return documentType.displayName() + " 미리보기와 OCR 요약이 여기에 표시됩니다.";
    }
}
