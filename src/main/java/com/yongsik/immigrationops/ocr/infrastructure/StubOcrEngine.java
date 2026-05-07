package com.yongsik.immigrationops.ocr.infrastructure;

import com.yongsik.immigrationops.ocr.domain.OcrEngine;
import com.yongsik.immigrationops.ocr.domain.OcrResult;
import com.yongsik.immigrationops.ocr.domain.TextSpan;
import com.yongsik.immigrationops.storage.domain.StoredFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.ocr", name = "provider", havingValue = "stub", matchIfMissing = true)
public class StubOcrEngine implements OcrEngine {

    @Override
    public OcrResult extract(StoredFile storedFile) throws IOException {
        String filename = storedFile.originalFilename() == null ? "unknown" : storedFile.originalFilename();
        String mediaType = storedFile.mediaType() == null ? "application/octet-stream" : storedFile.mediaType();

        if (mediaType.startsWith("text/") || filename.endsWith(".txt")) {
            String text = Files.readString(storedFile.path(), StandardCharsets.UTF_8);
            return new OcrResult(text, 0.98, List.of(new TextSpan(text, 0, 0, 0, 0, 0.98)));
        }

        String fallbackText = """
                OCR provider is not configured yet.
                The pipeline is active and ready for provider integration.
                Original filename: %s
                Media type: %s
                Add a real OCR adapter by replacing StubOcrEngine.
                """.formatted(filename, mediaType).trim();

        return new OcrResult(fallbackText, 0.35, List.of(new TextSpan(filename, 0, 0, 0, 0, 0.35)));
    }
}

