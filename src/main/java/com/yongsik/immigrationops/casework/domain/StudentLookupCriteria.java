package com.yongsik.immigrationops.casework.domain;

import java.time.LocalDate;

public record StudentLookupCriteria(
        String nationality,
        String passportNumber,
        LocalDate birthDate
) {
    public StudentLookupCriteria normalized() {
        return new StudentLookupCriteria(
                normalize(nationality),
                normalize(passportNumber),
                birthDate
        );
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
