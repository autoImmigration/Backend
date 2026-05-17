package com.yongsik.immigrationops.casework.application;

public record CreateUploadBatchCommand(
        String originalFilename,
        String rawZipStorageType,
        String rawZipLocation,
        String rawZipChecksum,
        long rawZipSizeBytes,
        String note,
        String schoolId
) {
    public CreateUploadBatchCommand(
            String originalFilename,
            String rawZipStorageType,
            String rawZipLocation,
            String rawZipChecksum,
            long rawZipSizeBytes,
            String note
    ) {
        this(originalFilename, rawZipStorageType, rawZipLocation, rawZipChecksum, rawZipSizeBytes, note, null);
    }
}
