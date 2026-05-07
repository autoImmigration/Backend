package com.yongsik.immigrationops.ocr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ocr")
public record OcrProperties(
        String provider,
        OpenAi openai,
        GoogleVision googleVision
) {

    public record OpenAi(
            String apiKey,
            String model,
            String baseUrl,
            int timeoutSeconds
    ) {
    }

    public record GoogleVision(
            String credentialsPath,
            String parent,
            int maxPages
    ) {
    }
}
