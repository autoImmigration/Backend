package com.yongsik.immigrationops.casework.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisaTypeJpaRepository extends JpaRepository<VisaTypeEntity, Long> {

    Optional<VisaTypeEntity> findByCode(String code);
}
