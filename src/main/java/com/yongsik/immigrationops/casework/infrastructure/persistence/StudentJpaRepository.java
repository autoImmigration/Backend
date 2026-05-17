package com.yongsik.immigrationops.casework.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentJpaRepository extends JpaRepository<StudentEntity, Long> {

    @EntityGraph(attributePaths = {"schoolOrganization", "agencyOrganization"})
    @Query("""
            select s
            from StudentEntity s
            where lower(s.nationality) = :nationality
              and lower(s.passportNumber) = :passportNumber
              and s.birthDate = :birthDate
            """)
    Optional<StudentEntity> findForLookup(
            @Param("nationality") String nationality,
            @Param("passportNumber") String passportNumber,
            @Param("birthDate") LocalDate birthDate
    );
}
