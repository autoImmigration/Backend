package com.yongsik.immigrationops.casework.application;

import java.time.Instant;

public record RecordPythonBatchResultCommand(
        String processingJobId,
        String status,
        String provider,
        String externalJobId,
        String manifestStorageType,
        String manifestLocation,
        String casesStorageType,
        String casesLocation,
        int fileCount,
        int caseCount,
        int errorCount,
        Instant startedAt,
        Instant finishedAt,
        String errorCode,
        String errorMessage
) {
}
