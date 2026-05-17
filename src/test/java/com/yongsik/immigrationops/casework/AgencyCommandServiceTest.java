package com.yongsik.immigrationops.casework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yongsik.immigrationops.common.BadRequestException;
import com.yongsik.immigrationops.casework.application.AgencyCommandService;
import com.yongsik.immigrationops.casework.application.CreateUploadBatchCommand;
import com.yongsik.immigrationops.casework.application.RecordPythonBatchResultCommand;
import com.yongsik.immigrationops.casework.domain.UploadBatchStatus;
import com.yongsik.immigrationops.casework.infrastructure.persistence.ProcessingJobEntity;
import com.yongsik.immigrationops.casework.infrastructure.persistence.ProcessingJobJpaRepository;
import com.yongsik.immigrationops.casework.infrastructure.persistence.UploadBatchEntity;
import com.yongsik.immigrationops.casework.infrastructure.persistence.UploadBatchJpaRepository;
import com.yongsik.immigrationops.security.AppUserEntity;
import com.yongsik.immigrationops.security.AppUserJpaRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AgencyCommandServiceTest {

    @Mock
    private UploadBatchJpaRepository uploadBatchJpaRepository;

    @Mock
    private ProcessingJobJpaRepository processingJobJpaRepository;

    @Mock
    private AppUserJpaRepository appUserJpaRepository;

    @InjectMocks
    private AgencyCommandService agencyCommandService;

    @Test
    void createUploadBatchCreatesQueuedProcessingJob() {
        AppUserEntity user = activeUser("agency-ops");
        when(appUserJpaRepository.findByUsername("agency-ops")).thenReturn(Optional.of(user));
        when(uploadBatchJpaRepository.save(any(UploadBatchEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(processingJobJpaRepository.save(any(ProcessingJobEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = agencyCommandService.createUploadBatch(
                "agency-ops",
                new CreateUploadBatchCommand(
                        "batch-a.zip",
                        "LOCAL_FILE",
                        "C:/tmp/batch-a.zip",
                        "sha256:abc",
                        1234L,
                        "test batch"
                )
        );

        ArgumentCaptor<UploadBatchEntity> batchCaptor = ArgumentCaptor.forClass(UploadBatchEntity.class);
        verify(uploadBatchJpaRepository).save(batchCaptor.capture());
        UploadBatchEntity savedBatch = batchCaptor.getValue();

        ArgumentCaptor<ProcessingJobEntity> jobCaptor = ArgumentCaptor.forClass(ProcessingJobEntity.class);
        verify(processingJobJpaRepository).save(jobCaptor.capture());
        ProcessingJobEntity savedJob = jobCaptor.getValue();

        assertTrue(result.uploadBatchId().startsWith("BATCH-"));
        assertTrue(result.processingJobId().startsWith("JOB-"));
        assertEquals(UploadBatchStatus.UPLOADED.name(), result.uploadBatchStatus());
        assertEquals("QUEUED", result.processingJobStatus());
        assertEquals(user, savedBatch.getUploadedByUser());
        assertEquals("LOCAL_FILE", savedBatch.getRawZipStorageType());
        assertEquals("C:/tmp/batch-a.zip", savedBatch.getRawZipLocation());
        assertEquals(1234L, savedBatch.getRawZipSizeBytes());
        assertEquals(savedBatch, savedJob.getUploadBatch());
        assertEquals("OCR_BATCH", savedJob.getJobType());
        assertEquals("QUEUED", savedJob.getStatus());
        assertNotNull(result.uploadedAt());
    }

    @Test
    void recordPythonResultStoresArtifactReferencesAndUpdatesBatchStatus() {
        UploadBatchEntity batch = new UploadBatchEntity();
        ReflectionTestUtils.setField(batch, "id", 11L);
        batch.setExternalId("BATCH-20260512-AAA111");
        batch.setStatus(UploadBatchStatus.UPLOADED.name());

        ProcessingJobEntity job = new ProcessingJobEntity();
        ReflectionTestUtils.setField(job, "id", 22L);
        job.setExternalId("JOB-20260512-BBB222");
        job.setUploadBatch(batch);
        job.setStatus("QUEUED");

        when(uploadBatchJpaRepository.findByExternalId("BATCH-20260512-AAA111")).thenReturn(Optional.of(batch));
        when(processingJobJpaRepository.findByExternalId("JOB-20260512-BBB222")).thenReturn(Optional.of(job));

        Instant startedAt = Instant.parse("2026-05-12T01:15:30Z");
        Instant finishedAt = Instant.parse("2026-05-12T01:18:04Z");

        var result = agencyCommandService.recordPythonResult(
                "BATCH-20260512-AAA111",
                new RecordPythonBatchResultCommand(
                        "JOB-20260512-BBB222",
                        "SUCCEEDED",
                        "openai",
                        "job-local-001",
                        "LOCAL_FILE",
                        "C:/tmp/manifest.json",
                        "LOCAL_FILE",
                        "C:/tmp/cases.json",
                        18,
                        3,
                        0,
                        startedAt,
                        finishedAt,
                        null,
                        null
                )
        );

        assertEquals("SUCCEEDED", job.getStatus());
        assertEquals("LOCAL_FILE", job.getManifestStorageType());
        assertEquals("C:/tmp/manifest.json", job.getManifestLocation());
        assertEquals("LOCAL_FILE", job.getCasesStorageType());
        assertEquals("C:/tmp/cases.json", job.getCasesLocation());
        assertEquals(18, job.getFileCount());
        assertEquals(3, job.getCaseCount());
        assertEquals(UploadBatchStatus.RESULT_UPLOADED.name(), batch.getStatus());
        assertEquals(3, batch.getDetectedStudentCount());
        assertEquals("JOB-20260512-BBB222", result.processingJobId());
        assertEquals(UploadBatchStatus.RESULT_UPLOADED.name(), result.uploadBatchStatus());
        assertNotNull(result.completedAt());
        assertNull(batch.getFailureReason());
    }

    @Test
    void recordPythonResultRejectsMissingArtifactsForPartialSuccess() {
        UploadBatchEntity batch = new UploadBatchEntity();
        ReflectionTestUtils.setField(batch, "id", 11L);
        batch.setExternalId("BATCH-20260512-AAA111");

        ProcessingJobEntity job = new ProcessingJobEntity();
        ReflectionTestUtils.setField(job, "id", 22L);
        job.setExternalId("JOB-20260512-BBB222");
        job.setUploadBatch(batch);

        when(uploadBatchJpaRepository.findByExternalId("BATCH-20260512-AAA111")).thenReturn(Optional.of(batch));
        when(processingJobJpaRepository.findByExternalId("JOB-20260512-BBB222")).thenReturn(Optional.of(job));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> agencyCommandService.recordPythonResult(
                        "BATCH-20260512-AAA111",
                        new RecordPythonBatchResultCommand(
                                "JOB-20260512-BBB222",
                                "PARTIAL_SUCCESS",
                                "openai",
                                null,
                                null,
                                null,
                                null,
                                null,
                                18,
                                2,
                                1,
                                null,
                                null,
                                null,
                                "one file failed"
                        )
                )
        );

        assertTrue(exception.getMessage().contains("Manifest and cases artifact references are required"));
    }

    private AppUserEntity activeUser(String username) {
        AppUserEntity user = new AppUserEntity();
        user.setUsername(username);
        user.setStatus("ACTIVE");
        return user;
    }
}
