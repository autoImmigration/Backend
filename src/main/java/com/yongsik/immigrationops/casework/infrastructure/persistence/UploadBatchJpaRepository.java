package com.yongsik.immigrationops.casework.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadBatchJpaRepository extends JpaRepository<UploadBatchEntity, Long> {

    @EntityGraph(attributePaths = {"previewFiles", "processingJobs"})
    List<UploadBatchEntity> findAllByOrderByUploadedAtDesc();

    @EntityGraph(attributePaths = {"previewFiles", "processingJobs"})
    Optional<UploadBatchEntity> findByExternalId(String externalId);
}
