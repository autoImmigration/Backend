package com.yongsik.immigrationops.casework.application;

import java.time.LocalDateTime;

public record PythonResultIngestResult(
        String uploadBatchId,
        String processingJobId,
        String uploadBatchStatus,
        String processingJobStatus,
        LocalDateTime completedAt
) {
}
