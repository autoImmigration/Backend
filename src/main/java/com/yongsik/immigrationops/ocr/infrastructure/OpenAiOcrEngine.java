package com.yongsik.immigrationops.ocr.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yongsik.immigrationops.ocr.config.OcrProperties;
import com.yongsik.immigrationops.ocr.domain.OcrEngine;
import com.yongsik.immigrationops.ocr.domain.OcrResult;
import com.yongsik.immigrationops.ocr.domain.TextSpan;
import com.yongsik.immigrationops.storage.domain.StoredFile;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.ocr", name = "provider", havingValue = "openai")
public class OpenAiOcrEngine implements OcrEngine {

    private static final String OCR_INSTRUCTION = """
            Extract all visible text from the provided document as accurately as possible.
            Preserve natural line breaks.
            Do not summarize.
            Do not explain.
            Output only the extracted text.
            """.trim();

    private final OcrProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiOcrEngine(OcrProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.openai().timeoutSeconds()))
                .build();
    }

    @Override
    public OcrResult extract(StoredFile storedFile) throws IOException {
        validateConfiguration();

        String mediaType = storedFile.mediaType();
        if (mediaType == null || mediaType.isBlank()) {
            throw new IOException("Missing media type for OCR request.");
        }

        if (mediaType.startsWith("text/")) {
            String text = Files.readString(storedFile.path());
            return new OcrResult(text, 0.99, List.of(new TextSpan(text, 0, 0, 0, 0, 0.99)));
        }

        byte[] bytes = Files.readAllBytes(storedFile.path());
        String requestBody = buildRequestBody(storedFile, bytes);
        JsonNode response = sendRequest(requestBody);
        String extractedText = extractOutputText(response);

        if (extractedText.isBlank()) {
            throw new IOException("OpenAI OCR returned empty text.");
        }

        return new OcrResult(
                extractedText,
                0.88,
                List.of(new TextSpan(extractedText, 0, 0, 0, 0, 0.88))
        );
    }

    private void validateConfiguration() throws IOException {
        if (properties.openai() == null || properties.openai().apiKey() == null || properties.openai().apiKey().isBlank()) {
            throw new IOException("OPENAI_API_KEY is required when app.ocr.provider=openai");
        }
    }

    private String buildRequestBody(StoredFile storedFile, byte[] bytes) throws IOException {
        String base64 = Base64.getEncoder().encodeToString(bytes);
        ObjectNode contentNode;

        if ("application/pdf".equalsIgnoreCase(storedFile.mediaType())) {
            contentNode = objectMapper.createObjectNode();
            contentNode.put("type", "input_file");
            contentNode.put("filename", storedFile.originalFilename());
            contentNode.put("file_data", base64);
        } else if (storedFile.mediaType().startsWith("image/")) {
            String dataUrl = "data:%s;base64,%s".formatted(storedFile.mediaType(), base64);
            contentNode = objectMapper.createObjectNode();
            contentNode.put("type", "input_image");
            contentNode.put("detail", "high");
            contentNode.put("image_url", dataUrl);
        } else {
            throw new IOException("Unsupported OCR media type for OpenAI adapter: " + storedFile.mediaType());
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.openai().model());

        ArrayNode content = objectMapper.createArrayNode();
        content.add(objectMapper.createObjectNode()
                .put("type", "input_text")
                .put("text", OCR_INSTRUCTION));
        content.add(contentNode);

        ObjectNode inputMessage = objectMapper.createObjectNode();
        inputMessage.put("role", "user");
        inputMessage.set("content", content);

        ArrayNode input = objectMapper.createArrayNode();
        input.add(inputMessage);
        body.set("input", input);

        return objectMapper.writeValueAsString(body);
    }

    private JsonNode sendRequest(String requestBody) throws IOException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.openai().baseUrl() + "/v1/responses"))
                    .timeout(Duration.ofSeconds(properties.openai().timeoutSeconds()))
                    .header("Authorization", "Bearer " + properties.openai().apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IOException("OpenAI OCR request failed: HTTP " + response.statusCode() + " - " + response.body());
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("OpenAI OCR request interrupted", exception);
        }
    }

    private String extractOutputText(JsonNode response) {
        StringBuilder builder = new StringBuilder();
        JsonNode output = response.path("output");
        if (!output.isArray()) {
            return "";
        }

        for (JsonNode outputItem : output) {
            JsonNode content = outputItem.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode contentItem : content) {
                if ("output_text".equals(contentItem.path("type").asText())) {
                    if (builder.length() > 0) {
                        builder.append(System.lineSeparator());
                    }
                    builder.append(contentItem.path("text").asText(""));
                }
            }
        }
        return builder.toString().trim();
    }

}
