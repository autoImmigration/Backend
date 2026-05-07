package com.yongsik.immigrationops.passport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yongsik.immigrationops.passport.application.PassportMrzExtractor;
import org.junit.jupiter.api.Test;

class PassportMrzExtractorTest {

    @Test
    void extractsPassportFieldsFromMrz() {
        PassportMrzExtractor extractor = new PassportMrzExtractor();
        String mrz = """
                P<KORKIM<<MINSU<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                M123456783KOR9001017M3001012<<<<<<<<<<<<<<02
                """;

        var result = extractor.extract(mrz);

        assertTrue(result.isPresent());
        assertEquals("M12345678", result.get().passportNumber());
        assertEquals("KOR", result.get().nationality());
        assertEquals("900101", result.get().birthDate());
        assertEquals("300101", result.get().expiryDate());
    }
}
