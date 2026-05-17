package com.yongsik.immigrationops.casework.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessingJobJpaRepository extends JpaRepository<ProcessingJobEntity, Long> {

    Optional<ProcessingJobEntity> findByExternalId(String externalId);
}
