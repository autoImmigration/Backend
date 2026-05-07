package com.yongsik.immigrationops.document.domain;

import java.util.UUID;

public record DocumentId(String value) {

    public static DocumentId newId() {
        return new DocumentId(UUID.randomUUID().toString());
    }
}

