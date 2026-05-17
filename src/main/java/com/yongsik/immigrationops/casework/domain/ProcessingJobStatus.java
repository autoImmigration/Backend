package com.yongsik.immigrationops.casework.domain;

import com.yongsik.immigrationops.common.BadRequestException;
import java.util.Locale;

public enum ProcessingJobStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    PARTIAL_SUCCESS,
    FAILED;

    public static ProcessingJobStatus from(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Processing job status is required");
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Unsupported processing job status: " + value);
        }
    }

    public boolean isTerminalCallbackStatus() {
        return this == SUCCEEDED || this == PARTIAL_SUCCESS || this == FAILED;
    }
}
