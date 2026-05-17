package com.yongsik.immigrationops.casework.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentTypeJpaRepository extends JpaRepository<DocumentTypeEntity, Long> {

    Optional<DocumentTypeEntity> findByCode(String code);
}
