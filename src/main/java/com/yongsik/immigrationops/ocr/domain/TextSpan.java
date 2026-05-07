package com.yongsik.immigrationops.ocr.domain;

public record TextSpan(
        String text,
        int x,
        int y,
        int width,
        int height,
        double confidence
) {
}

