package com.yongsik.immigrationops.casework.domain;

import java.time.LocalDate;

public record StudentRecord(
        String id,
        String name,
        String nationality,
        String passportNumber,
        LocalDate birthDate,
        String schoolName,
        String schoolDepartment,
        String agencyName,
        String term
) {
}
