package com.yongsik.immigrationops.passport.application;

import com.yongsik.immigrationops.passport.domain.PassportData;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PassportMrzExtractor {

    public Optional<PassportData> extract(String text) {
        String[] lines = Arrays.stream(text.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toArray(String[]::new);

        for (int index = 0; index < lines.length - 1; index++) {
            String first = normalize(lines[index]);
            String second = normalize(lines[index + 1]);
            if (first.startsWith("P<") && first.length() >= 40 && second.length() >= 40) {
                String namesPart = first.substring(5);
                String[] names = namesPart.split("<<");
                String surname = sanitizeName(names.length > 0 ? names[0] : "");
                String givenNames = sanitizeName(names.length > 1 ? names[1] : "");

                return Optional.of(new PassportData(
                        first.substring(0, 2),
                        safeSlice(first, 2, 5),
                        surname,
                        givenNames,
                        safeSlice(second, 0, 9),
                        safeSlice(second, 10, 13),
                        safeSlice(second, 13, 19),
                        safeSlice(second, 21, 27)
                ));
            }
        }
        return Optional.empty();
    }

    private String normalize(String value) {
        return value.replace(" ", "").toUpperCase(Locale.ROOT);
    }

    private String sanitizeName(String value) {
        return value.replace('<', ' ').trim().replaceAll("\\s+", " ");
    }

    private String safeSlice(String value, int start, int end) {
        if (value.length() < end) {
            return "";
        }
        return value.substring(start, end).replace("<", "").trim();
    }
}

