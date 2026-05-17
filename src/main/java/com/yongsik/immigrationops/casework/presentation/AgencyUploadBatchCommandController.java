package com.yongsik.immigrationops.casework.presentation;

import com.yongsik.immigrationops.casework.application.AgencyCommandService;
import com.yongsik.immigrationops.casework.application.CreateUploadBatchCommand;
import com.yongsik.immigrationops.casework.application.StoredUploadBatchFile;
import com.yongsik.immigrationops.casework.application.UploadBatchCommandResult;
import com.yongsik.immigrationops.casework.application.UploadBatchFileStorage;
import com.yongsik.immigrationops.common.BadRequestException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/agency/upload-batches")
public class AgencyUploadBatchCommandController {

    private final AgencyCommandService agencyCommandService;
    private final UploadBatchFileStorage uploadBatchFileStorage;

    public AgencyUploadBatchCommandController(
            AgencyCommandService agencyCommandService,
            UploadBatchFileStorage uploadBatchFileStorage
    ) {
        this.agencyCommandService = agencyCommandService;
        this.uploadBatchFileStorage = uploadBatchFileStorage;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AgencyUploadBatchCreatedResponse createUploadBatch(
            Authentication authentication,
            @Valid @RequestBody AgencyUploadBatchCreateRequest request
    ) {
        UploadBatchCommandResult result = agencyCommandService.createUploadBatch(
                authentication.getName(),
                new CreateUploadBatchCommand(
                        request.originalFilename(),
                        request.rawZipStorageType(),
                        request.rawZipLocation(),
                        request.rawZipChecksum(),
                        request.rawZipSizeBytes(),
                        request.note(),
                        request.schoolId()
                )
        );

        return toCreatedResponse(result);
    }

    @PostMapping(path = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    AgencyUploadBatchCreatedResponse createUploadBatchFromFile(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "schoolId", required = false) String schoolId
    ) {
        validateZipFile(file);
        StoredUploadBatchFile storedFile = uploadBatchFileStorage.store(file);

        UploadBatchCommandResult result = agencyCommandService.createUploadBatch(
                authentication.getName(),
                new CreateUploadBatchCommand(
                        storedFile.originalFilename(),
                        storedFile.storageType(),
                        storedFile.location(),
                        storedFile.checksum(),
                        storedFile.sizeBytes(),
                        note,
                        schoolId
                )
        );

        return toCreatedResponse(result);
    }

    private AgencyUploadBatchCreatedResponse toCreatedResponse(UploadBatchCommandResult result) {
        return new AgencyUploadBatchCreatedResponse(
                result.uploadBatchId(),
                result.processingJobId(),
                result.uploadBatchStatus(),
                result.processingJobStatus(),
                result.uploadedAt()
        );
    }

    private void validateZipFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("ZIP file must not be empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("ZIP filename is required");
        }
        if (!originalFilename.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new BadRequestException("Only .zip files are supported");
        }
    }
}

record AgencyUploadBatchCreateRequest(
        @NotBlank String originalFilename,
        @NotBlank String rawZipStorageType,
        @NotBlank String rawZipLocation,
        String rawZipChecksum,
        @NotNull @PositiveOrZero Long rawZipSizeBytes,
        String note,
        String schoolId
) {
}

record AgencyUploadBatchCreatedResponse(
        String uploadBatchId,
        String processingJobId,
        String uploadBatchStatus,
        String processingJobStatus,
        LocalDateTime uploadedAt
) {
}
