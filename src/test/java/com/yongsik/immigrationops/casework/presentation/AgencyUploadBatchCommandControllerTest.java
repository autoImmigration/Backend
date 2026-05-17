package com.yongsik.immigrationops.casework.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yongsik.immigrationops.casework.application.AgencyCommandService;
import com.yongsik.immigrationops.casework.application.CreateUploadBatchCommand;
import com.yongsik.immigrationops.casework.application.StoredUploadBatchFile;
import com.yongsik.immigrationops.casework.application.UploadBatchCommandResult;
import com.yongsik.immigrationops.casework.application.UploadBatchFileStorage;
import com.yongsik.immigrationops.common.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class AgencyUploadBatchCommandControllerTest {

    @Mock
    private AgencyCommandService agencyCommandService;

    @Mock
    private UploadBatchFileStorage uploadBatchFileStorage;

    @InjectMocks
    private AgencyUploadBatchCommandController controller;

    @Test
    void createUploadBatchFromFileStoresZipAndCreatesBatch() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "agency-batch.ZIP",
                "application/zip",
                "zip-data".getBytes(StandardCharsets.UTF_8)
        );
        StoredUploadBatchFile storedFile = new StoredUploadBatchFile(
                "agency-batch.ZIP",
                "LOCAL_FILE",
                "C:/uploads/upload-batches/20260512/agency-batch.ZIP",
                "sha256:abc123",
                8L
        );
        LocalDateTime uploadedAt = LocalDateTime.of(2026, 5, 12, 12, 45);

        when(uploadBatchFileStorage.store(file)).thenReturn(storedFile);
        when(agencyCommandService.createUploadBatch(eq("agency-ops"), eq(new CreateUploadBatchCommand(
                "agency-batch.ZIP",
                "LOCAL_FILE",
                "C:/uploads/upload-batches/20260512/agency-batch.ZIP",
                "sha256:abc123",
                8L,
                "night batch"
        )))).thenReturn(new UploadBatchCommandResult(
                "BATCH-20260512-ABC123",
                "JOB-20260512-DEF456",
                "UPLOADED",
                "QUEUED",
                uploadedAt
        ));

        AgencyUploadBatchCreatedResponse response = controller.createUploadBatchFromFile(
                new TestingAuthenticationToken("agency-ops", null),
                file,
                "night batch"
        );

        ArgumentCaptor<MultipartFile> fileCaptor = ArgumentCaptor.forClass(MultipartFile.class);
        verify(uploadBatchFileStorage).store(fileCaptor.capture());
        assertEquals("agency-batch.ZIP", fileCaptor.getValue().getOriginalFilename());
        assertEquals("BATCH-20260512-ABC123", response.uploadBatchId());
        assertEquals("JOB-20260512-DEF456", response.processingJobId());
        assertEquals(uploadedAt, response.uploadedAt());
    }

    @Test
    void createUploadBatchFromFileRejectsEmptyZip() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.zip", "application/zip", new byte[0]);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> controller.createUploadBatchFromFile(new TestingAuthenticationToken("agency-ops", null), file, null)
        );

        assertEquals("ZIP file must not be empty", exception.getMessage());
    }

    @Test
    void createUploadBatchFromFileRejectsNonZipExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "batch.pdf",
                "application/pdf",
                "not-a-zip".getBytes(StandardCharsets.UTF_8)
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> controller.createUploadBatchFromFile(new TestingAuthenticationToken("agency-ops", null), file, null)
        );

        assertEquals("Only .zip files are supported", exception.getMessage());
    }
}
