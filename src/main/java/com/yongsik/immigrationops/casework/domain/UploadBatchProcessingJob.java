package com.yongsik.immigrationops.casework.domain;

import java.time.LocalDateTime;

public record UploadBatchProcessingJob(
        String id,
        ProcessingJobType type,
        ProcessingJobStatus status,
        int attemptNo,
        String provider,
        String externalJobId,
        int fileCount,
        int caseCount,
        int errorCount,
        String errorCode,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
