package com.yongsik.immigrationops.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record SecurityProperties(
        Cors cors,
        Storage storage,
        Credentials security
) {

    public record Cors(List<String> allowedOrigins) {
    }

    public record Storage(String rootPath) {
    }

    public record Credentials(String username, String password) {
    }
}

