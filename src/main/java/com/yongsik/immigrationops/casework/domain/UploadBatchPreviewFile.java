package com.yongsik.immigrationops.casework.domain;

public record UploadBatchPreviewFile(
        String id,
        String studentName,
        String documentName,
        String pageRange,
        String note
) {
}
