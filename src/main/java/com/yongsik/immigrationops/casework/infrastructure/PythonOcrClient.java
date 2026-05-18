package com.yongsik.immigrationops.casework.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PythonOcrClient {

    private static final Logger log = LoggerFactory.getLogger(PythonOcrClient.class);

    private final String pythonBaseUrl;
    private final RestClient restClient;

    public PythonOcrClient(
            @Value("${app.python-ocr.base-url:http://localhost:8001}") String pythonBaseUrl
    ) {
        this.pythonBaseUrl = pythonBaseUrl;
        this.restClient = RestClient.create();
    }

    public record BatchRequest(
            String upload_batch_id,
            String processing_job_id,
            String images_dir,
            String output_dir,
            String callback_url,
            String internal_api_key,
            String provider,
            String model,
            String base_url,
            double timeout_seconds
    ) {}

    public record BatchProgress(String batch_id, int processed, int total, String status) {}

    public BatchProgress getBatchProgress(String batchId) {
        try {
            return restClient.get()
                    .uri(pythonBaseUrl + "/batches/" + batchId + "/progress")
                    .retrieve()
                    .body(BatchProgress.class);
        } catch (Exception e) {
            return new BatchProgress(batchId, 0, 0, "unknown");
        }
    }

    public void submitBatch(BatchRequest request) {
        log.info("[PythonOcrClient] Submitting batch upload_batch_id={} to {}", request.upload_batch_id(), pythonBaseUrl);
        try {
            restClient.post()
                    .uri(pythonBaseUrl + "/batches")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[PythonOcrClient] Batch accepted by Python service upload_batch_id={}", request.upload_batch_id());
        } catch (Exception e) {
            log.error("[PythonOcrClient] Failed to submit batch upload_batch_id={}: {}", request.upload_batch_id(), e.getMessage());
            throw new RuntimeException("Python OCR service unavailable: " + e.getMessage(), e);
        }
    }
}
