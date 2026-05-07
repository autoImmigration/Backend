package com.yongsik.immigrationops.storage.domain;

import java.nio.file.Path;

public record StoredFile(
        String originalFilename,
        String mediaType,
        Path path
) {
}

