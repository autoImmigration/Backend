package com.yongsik.immigrationops.classification.infrastructure;

import com.yongsik.immigrationops.classification.domain.ClassificationResult;
import com.yongsik.immigrationops.classification.domain.DocumentClassifier;
import com.yongsik.immigrationops.document.domain.DocumentType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class KeywordDocumentClassifier implements DocumentClassifier {

    private final Map<DocumentType, List<String>> rules = new LinkedHashMap<>();

    public KeywordDocumentClassifier() {
        rules.put(DocumentType.PASSPORT, List.of("passport", "p<", "<<"));
        rules.put(DocumentType.APPLICATION_FORM, List.of("application", "신청", "type"));
        rules.put(DocumentType.ENROLLMENT_CERTIFICATE, List.of("enrollment", "재학", "certificate"));
        rules.put(DocumentType.ADMISSION_LETTER, List.of("admission", "합격", "letter"));
        rules.put(DocumentType.TUITION_RECEIPT, List.of("tuition", "receipt", "납입"));
        rules.put(DocumentType.ID_PHOTO_FORM, List.of("photo", "사진", "portrait"));
        rules.put(DocumentType.FINANCIAL_STATEMENT, List.of("balance", "statement", "잔고"));
        rules.put(DocumentType.RESIDENCE_APPLICATION, List.of("residence", "체류", "application"));
        rules.put(DocumentType.CONSENT_FORM, List.of("consent", "동의", "signature"));
    }

    @Override
    public ClassificationResult classify(String filename, String extractedText) {
        String normalized = (filename + " " + extractedText).toLowerCase(Locale.ROOT);

        DocumentType bestType = DocumentType.UNKNOWN;
        int bestScore = 0;
        List<String> evidence = List.of();

        for (Map.Entry<DocumentType, List<String>> entry : rules.entrySet()) {
            List<String> matches = new ArrayList<>();
            for (String keyword : entry.getValue()) {
                if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                    matches.add(keyword);
                }
            }
            if (matches.size() > bestScore) {
                bestScore = matches.size();
                bestType = entry.getKey();
                evidence = matches;
            }
        }

        if (bestType == DocumentType.UNKNOWN) {
            return new ClassificationResult(DocumentType.UNKNOWN, 0.25, List.of("no keyword match"));
        }

        double confidence = Math.min(0.55 + (bestScore * 0.15), 0.95);
        return new ClassificationResult(bestType, confidence, evidence);
    }
}

