package com.yongsik.immigrationops.casework.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadBatchJpaRepository extends JpaRepository<UploadBatchEntity, Long> {

    List<UploadBatchEntity> findAllByOrderByUploadedAtDesc();

    Optional<UploadBatchEntity> findByExternalId(String externalId);
}
