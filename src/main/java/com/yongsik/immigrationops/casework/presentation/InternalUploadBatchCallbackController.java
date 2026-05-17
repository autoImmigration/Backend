package com.yongsik.immigrationops.casework.presentation;

import com.yongsik.immigrationops.casework.application.AgencyCommandService;
import com.yongsik.immigrationops.casework.application.PythonResultIngestResult;
import com.yongsik.immigrationops.casework.application.RecordPythonBatchResultCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/internal/upload-batches")
public class InternalUploadBatchCallbackController {

    private final AgencyCommandService agencyCommandService;
    private final String internalApiKey;

    public InternalUploadBatchCallbackController(
            AgencyCommandService agencyCommandService,
            @Value("${INTERNAL_API_KEY:}") String internalApiKey
    ) {
        this.agencyCommandService = agencyCommandService;
        this.internalApiKey = internalApiKey;
    }

    @PostMapping("/{uploadBatchId}/python-results")
    PythonBatchResultCallbackResponse recordPythonResult(
            @PathVariable String uploadBatchId,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String providedApiKey,
            @Valid @RequestBody PythonBatchResultCallbackRequest request
    ) {
        if (internalApiKey == null || internalApiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Internal API key is not configured on the server");
        }
        if (!internalApiKey.equals(providedApiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing X-Internal-Api-Key header");
        }

        PythonResultIngestResult result = agencyCommandService.recordPythonResult(
                uploadBatchId,
                new RecordPythonBatchResultCommand(
                        request.processingJobId(),
                        request.status(),
                        request.provider(),
                        request.externalJobId(),
                        request.manifestStorageType(),
                        request.manifestLocation(),
                        request.casesStorageType(),
                        request.casesLocation(),
                        request.fileCount(),
                        request.caseCount(),
                        request.errorCount(),
                        request.startedAt(),
                        request.finishedAt(),
                        request.errorCode(),
                        request.errorMessage()
                )
        );

        return new PythonBatchResultCallbackResponse(
                result.uploadBatchId(),
                result.processingJobId(),
                result.uploadBatchStatus(),
                result.processingJobStatus(),
                result.completedAt()
        );
    }
}

record PythonBatchResultCallbackRequest(
        @NotBlank String processingJobId,
        @NotBlank String status,
        String provider,
        String externalJobId,
        String manifestStorageType,
        String manifestLocation,
        String casesStorageType,
        String casesLocation,
        @NotNull @PositiveOrZero Integer fileCount,
        @NotNull @PositiveOrZero Integer caseCount,
        @NotNull @PositiveOrZero Integer errorCount,
        Instant startedAt,
        Instant finishedAt,
        String errorCode,
        String errorMessage
) {
}

record PythonBatchResultCallbackResponse(
        String uploadBatchId,
        String processingJobId,
        String uploadBatchStatus,
        String processingJobStatus,
        LocalDateTime completedAt
) {
}
