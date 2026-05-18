package com.yongsik.immigrationops.casework.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationCaseJpaRepository extends JpaRepository<ApplicationCaseEntity, Long> {

    @EntityGraph(attributePaths = {
            "student",
            "student.schoolOrganization",
            "student.agencyOrganization",
            "schoolOrganization",
            "agencyOrganization",
            "visaType",
            "documents",
            "documents.documentType"
    })
    List<ApplicationCaseEntity> findAllByStudentExternalIdOrderByApplicationDateDesc(String studentExternalId);

    @EntityGraph(attributePaths = {
            "student",
            "student.schoolOrganization",
            "student.agencyOrganization",
            "schoolOrganization",
            "agencyOrganization",
            "visaType",
            "documents",
            "documents.documentType"
    })
    List<ApplicationCaseEntity> findAllByOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = {
            "student",
            "student.schoolOrganization",
            "student.agencyOrganization",
            "schoolOrganization",
            "agencyOrganization",
            "visaType",
            "documents",
            "documents.documentType"
    })
    Optional<ApplicationCaseEntity> findByExternalId(String externalId);

    @EntityGraph(attributePaths = {
            "student",
            "student.schoolOrganization",
            "student.agencyOrganization",
            "schoolOrganization",
            "agencyOrganization",
            "visaType",
            "documents",
            "documents.documentType"
    })
    List<ApplicationCaseEntity> findAllByIntakeBatchOrderByCreatedAtAsc(String intakeBatch);
}
