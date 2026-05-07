package com.yongsik.immigrationops.ocr.domain;

import java.util.List;

public record OcrResult(
        String fullText,
        double confidence,
        List<TextSpan> spans
) {
}

