package com.yongsik.immigrationops.casework.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationJpaRepository extends JpaRepository<OrganizationEntity, Long> {

    Optional<OrganizationEntity> findByTypeAndName(OrganizationType type, String name);
}
