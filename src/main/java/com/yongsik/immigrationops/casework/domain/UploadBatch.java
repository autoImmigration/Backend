package com.yongsik.immigrationops.casework.domain;

import java.time.LocalDateTime;
import java.util.List;

public record UploadBatch(
        String id,
        String fileName,
        LocalDateTime uploadedAt,
        int studentCount,
        UploadBatchStatus status,
        String note,
        UploadBatchProcessingJob processingJob,
        List<UploadBatchPreviewFile> previewFiles
) {
    public UploadBatch {
        previewFiles = List.copyOf(previewFiles);
    }

    public UploadBatch(
            String id,
            String fileName,
            LocalDateTime uploadedAt,
            int studentCount,
            UploadBatchStatus status,
            String note,
            List<UploadBatchPreviewFile> previewFiles
    ) {
        this(id, fileName, uploadedAt, studentCount, status, note, null, previewFiles);
    }
}
