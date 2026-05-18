package com.yongsik.immigrationops.casework.presentation;

import com.yongsik.immigrationops.casework.application.AgencyQueryService;
import com.yongsik.immigrationops.casework.domain.ApplicationCase;
import com.yongsik.immigrationops.casework.domain.CaseDocument;
import com.yongsik.immigrationops.casework.domain.UploadBatch;
import com.yongsik.immigrationops.casework.domain.UploadBatchProcessingJob;
import com.yongsik.immigrationops.casework.domain.UploadBatchPreviewFile;
import com.yongsik.immigrationops.casework.infrastructure.PythonOcrClient;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agency")
public class AgencyOperationsController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final AgencyQueryService agencyQueryService;
    private final PythonOcrClient pythonOcrClient;

    public AgencyOperationsController(AgencyQueryService agencyQueryService, PythonOcrClient pythonOcrClient) {
        this.agencyQueryService = agencyQueryService;
        this.pythonOcrClient = pythonOcrClient;
    }

    @GetMapping("/schools")
    List<AgencyQueryService.SchoolSummary> listSchools() {
        return agencyQueryService.findSchools();
    }

    @GetMapping("/application-cases")
    List<AgencyApplicationSummaryResponse> listCases(
            @RequestParam(defaultValue = "studentName") String searchField,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "전체") String status
    ) {
        return agencyQueryService.findCases(searchField, search, status).stream()
                .map(this::toSummary)
                .toList();
    }

    @GetMapping("/application-cases/{caseId}")
    AgencyApplicationDetailResponse getCase(@PathVariable String caseId) {
        return toDetail(agencyQueryService.getCase(caseId));
    }

    @GetMapping("/upload-batches")
    List<AgencyUploadBatchSummaryResponse> listUploadBatches() {
        return agencyQueryService.findUploadBatches().stream()
                .map(this::toBatchSummary)
                .toList();
    }

    @GetMapping("/upload-batches/{batchId}/ocr-progress")
    PythonOcrClient.BatchProgress getOcrProgress(@PathVariable String batchId) {
        return pythonOcrClient.getBatchProgress(batchId);
    }

    @GetMapping("/upload-batches/{batchId}")
    AgencyUploadBatchDetailResponse getUploadBatch(@PathVariable String batchId) {
        return toBatchDetail(
                agencyQueryService.getUploadBatch(batchId),
                agencyQueryService.findCasesByBatchId(batchId)
        );
    }

    private AgencyApplicationSummaryResponse toSummary(ApplicationCase applicationCase) {
        return new AgencyApplicationSummaryResponse(
                applicationCase.id(),
                applicationCase.student().name(),
                applicationCase.student().nationality(),
                applicationCase.schoolName(),
                applicationCase.visaType().displayName(),
                applicationCase.applicationKind().displayName(),
                DATE_FORMAT.format(applicationCase.applicationDate()),
                applicationCase.status().displayName(),
                applicationCase.coordinatorName(),
                DATE_TIME_FORMAT.format(applicationCase.updatedAt()),
                applicationCase.intakeBatch(),
                applicationCase.submittedDocumentCount(),
                applicationCase.missingDocumentCount()
        );
    }

    private AgencyApplicationDetailResponse toDetail(ApplicationCase applicationCase) {
        return new AgencyApplicationDetailResponse(
                applicationCase.id(),
                applicationCase.student().name(),
                applicationCase.student().nationality(),
                applicationCase.student().birthDate().toString(),
                applicationCase.schoolName(),
                applicationCase.visaType().displayName(),
                applicationCase.applicationKind().displayName(),
                DATE_FORMAT.format(applicationCase.applicationDate()),
                applicationCase.status().displayName(),
                applicationCase.coordinatorName(),
                DATE_TIME_FORMAT.format(applicationCase.updatedAt()),
                applicationCase.intakeBatch(),
                applicationCase.submittedDocumentCount(),
                applicationCase.missingDocumentCount(),
                applicationCase.documents().stream().map(this::toDocument).toList()
        );
    }

    private AgencyCaseDocumentResponse toDocument(CaseDocument document) {
        return new AgencyCaseDocumentResponse(
                document.type().code(),
                document.type().displayName(),
                document.type().category(),
                document.type().reviewRule(),
                document.status().displayName(),
                document.submittedAt() == null ? "-" : DATE_FORMAT.format(document.submittedAt()),
                document.note(),
                document.preview()
        );
    }

    private AgencyUploadBatchSummaryResponse toBatchSummary(UploadBatch uploadBatch) {
        return new AgencyUploadBatchSummaryResponse(
                uploadBatch.id(),
                uploadBatch.fileName(),
                uploadBatch.status().name(),
                DATE_TIME_FORMAT.format(uploadBatch.uploadedAt()),
                uploadBatch.studentCount(),
                uploadBatch.status().displayName(),
                uploadBatch.note(),
                toProcessingJob(uploadBatch.processingJob())
        );
    }

    private AgencyUploadBatchDetailResponse toBatchDetail(UploadBatch uploadBatch, List<ApplicationCase> cases) {
        return new AgencyUploadBatchDetailResponse(
                uploadBatch.id(),
                uploadBatch.fileName(),
                uploadBatch.status().name(),
                DATE_TIME_FORMAT.format(uploadBatch.uploadedAt()),
                uploadBatch.studentCount(),
                uploadBatch.status().displayName(),
                uploadBatch.note(),
                toProcessingJob(uploadBatch.processingJob()),
                uploadBatch.previewFiles().stream().map(this::toPreviewFile).toList(),
                cases.stream().map(this::toBatchCaseResult).toList()
        );
    }

    private AgencyBatchCaseResultResponse toBatchCaseResult(ApplicationCase applicationCase) {
        return new AgencyBatchCaseResultResponse(
                applicationCase.id(),
                applicationCase.student().name(),
                applicationCase.student().nationality(),
                applicationCase.visaType().displayName(),
                applicationCase.submittedDocumentCount(),
                applicationCase.missingDocumentCount(),
                applicationCase.documents().stream().map(this::toDocument).toList()
        );
    }

    private AgencyUploadBatchPreviewFileResponse toPreviewFile(UploadBatchPreviewFile previewFile) {
        return new AgencyUploadBatchPreviewFileResponse(
                previewFile.id(),
                previewFile.documentName(),
                previewFile.studentName(),
                previewFile.pageRange(),
                previewFile.note()
        );
    }

    private AgencyUploadBatchProcessingJobResponse toProcessingJob(UploadBatchProcessingJob processingJob) {
        if (processingJob == null) {
            return null;
        }

        return new AgencyUploadBatchProcessingJobResponse(
                processingJob.id(),
                processingJob.type().name(),
                processingJob.status().name(),
                processingJob.attemptNo(),
                processingJob.provider(),
                processingJob.externalJobId(),
                processingJob.fileCount(),
                processingJob.caseCount(),
                processingJob.errorCount(),
                processingJob.errorCode(),
                processingJob.errorMessage(),
                formatDateTime(processingJob.createdAt()),
                formatDateTime(processingJob.startedAt()),
                formatDateTime(processingJob.finishedAt())
        );
    }

    private String formatDateTime(java.time.LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMAT.format(value);
    }
}

record AgencyApplicationSummaryResponse(
        String id,
        String studentName,
        String nationality,
        String schoolName,
        String visaType,
        String applicationType,
        String applicationDate,
        String status,
        String coordinator,
        String updatedAt,
        String intakeBatch,
        int submittedCount,
        int missingCount
) {
}

record AgencyApplicationDetailResponse(
        String id,
        String studentName,
        String nationality,
        String birthDate,
        String schoolName,
        String visaType,
        String applicationType,
        String applicationDate,
        String status,
        String coordinator,
        String updatedAt,
        String intakeBatch,
        int submittedCount,
        int missingCount,
        List<AgencyCaseDocumentResponse> documents
) {
}

record AgencyCaseDocumentResponse(
        String code,
        String name,
        String category,
        String rule,
        String status,
        String submittedAt,
        String note,
        String preview
) {
}

record AgencyUploadBatchSummaryResponse(
        String id,
        String fileName,
        String uploadBatchStatus,
        String uploadedAt,
        int studentCount,
        String status,
        String note,
        AgencyUploadBatchProcessingJobResponse processingJob
) {
}

record AgencyUploadBatchDetailResponse(
        String id,
        String fileName,
        String uploadBatchStatus,
        String uploadedAt,
        int studentCount,
        String status,
        String note,
        AgencyUploadBatchProcessingJobResponse processingJob,
        List<AgencyUploadBatchPreviewFileResponse> previewFiles,
        List<AgencyBatchCaseResultResponse> cases
) {
}

record AgencyBatchCaseResultResponse(
        String id,
        String studentName,
        String nationality,
        String applicationType,
        int submittedCount,
        int missingCount,
        List<AgencyCaseDocumentResponse> documents
) {
}

record AgencyUploadBatchPreviewFileResponse(
        String id,
        String documentName,
        String studentName,
        String pageRange,
        String note
) {
}

record AgencyUploadBatchProcessingJobResponse(
        String id,
        String type,
        String status,
        int attemptNo,
        String provider,
        String externalJobId,
        int fileCount,
        int caseCount,
        int errorCount,
        String errorCode,
        String errorMessage,
        String createdAt,
        String startedAt,
        String finishedAt
) {
}
