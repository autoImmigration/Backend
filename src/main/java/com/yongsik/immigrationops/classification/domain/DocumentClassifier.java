package com.yongsik.immigrationops.classification.domain;

public interface DocumentClassifier {

    ClassificationResult classify(String filename, String extractedText);
}

