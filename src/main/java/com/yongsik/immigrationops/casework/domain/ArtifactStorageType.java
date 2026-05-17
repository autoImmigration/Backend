package com.yongsik.immigrationops.casework.domain;

import com.yongsik.immigrationops.common.BadRequestException;
import java.util.Locale;

public enum ArtifactStorageType {
    LOCAL_FILE,
    S3_OBJECT;

    public static ArtifactStorageType from(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Artifact storage type is required");
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Unsupported artifact storage type: " + value);
        }
    }
}
