package com.yongsik.immigrationops.casework.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.yongsik.immigrationops.casework.application.AgencyCommandService;
import com.yongsik.immigrationops.casework.application.CreateUploadBatchCommand;
import com.yongsik.immigrationops.security.AppUserEntity;
import com.yongsik.immigrationops.security.AppUserJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UploadBatchWriteReadBridgeTest {

    @Mock
    private UploadBatchJpaRepository uploadBatchJpaRepository;

    @Mock
    private ProcessingJobJpaRepository processingJobJpaRepository;

    @Mock
    private AppUserJpaRepository appUserJpaRepository;

    @Mock
    private StudentJpaRepository studentJpaRepository;

    @Mock
    private ApplicationCaseJpaRepository applicationCaseJpaRepository;

    @Test
    void newlyCreatedBatchIsVisibleThroughDatabaseQueryRepository() {
        AtomicReference<UploadBatchEntity> storedBatch = new AtomicReference<>();

        when(appUserJpaRepository.findByUsername("agency-ops")).thenReturn(Optional.of(activeUser("agency-ops")));
        when(uploadBatchJpaRepository.save(any(UploadBatchEntity.class))).thenAnswer(invocation -> {
            UploadBatchEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 101L);
            storedBatch.set(entity);
            return entity;
        });
        when(processingJobJpaRepository.save(any(ProcessingJobEntity.class))).thenAnswer(invocation -> {
            ProcessingJobEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 202L);
            return entity;
        });
        when(uploadBatchJpaRepository.findAllByOrderByUploadedAtDesc()).thenAnswer(invocation -> {
            UploadBatchEntity entity = storedBatch.get();
            return entity == null ? List.of() : List.of(entity);
        });
        when(uploadBatchJpaRepository.findByExternalId(anyString())).thenAnswer(invocation -> {
            UploadBatchEntity entity = storedBatch.get();
            String externalId = invocation.getArgument(0);
            if (entity == null || !entity.getExternalId().equals(externalId)) {
                return Optional.empty();
            }
            return Optional.of(entity);
        });

        AgencyCommandService agencyCommandService = new AgencyCommandService(
                uploadBatchJpaRepository,
                processingJobJpaRepository,
                appUserJpaRepository
        );
        DatabaseCaseworkQueryRepository repository = new DatabaseCaseworkQueryRepository(
                studentJpaRepository,
                applicationCaseJpaRepository,
                uploadBatchJpaRepository,
                new CaseworkJpaMapper()
        );

        var created = agencyCommandService.createUploadBatch(
                "agency-ops",
                new CreateUploadBatchCommand(
                        "bridge-batch.zip",
                        "LOCAL_FILE",
                        "C:/uploads/bridge-batch.zip",
                        "sha256:bridge",
                        2048L,
                        "bridge test"
                )
        );

        var uploadBatches = repository.findUploadBatches();
        var uploadBatch = repository.findUploadBatchById(created.uploadBatchId()).orElseThrow();

        assertEquals(1, uploadBatches.size());
        assertEquals(created.uploadBatchId(), uploadBatch.id());
        assertEquals("bridge-batch.zip", uploadBatch.fileName());
        assertEquals("UPLOADED", uploadBatch.status().name());
        assertEquals(0, uploadBatch.studentCount());
        assertEquals(created.processingJobId(), uploadBatch.processingJob().id());
        assertEquals("QUEUED", uploadBatch.processingJob().status().name());
        assertTrue(uploadBatch.previewFiles().isEmpty());
    }

    private AppUserEntity activeUser(String username) {
        AppUserEntity user = new AppUserEntity();
        user.setUsername(username);
        user.setStatus("ACTIVE");
        return user;
    }
}
