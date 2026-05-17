package com.yongsik.immigrationops.casework.application;

public record StoredUploadBatchFile(
        String originalFilename,
        String storageType,
        String location,
        String checksum,
        long sizeBytes
) {
}
