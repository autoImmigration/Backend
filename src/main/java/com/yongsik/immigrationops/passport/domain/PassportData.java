package com.yongsik.immigrationops.passport.domain;

public record PassportData(
        String documentCode,
        String issuingCountry,
        String surname,
        String givenNames,
        String passportNumber,
        String nationality,
        String birthDate,
        String expiryDate
) {
}

