package com.yongsik.immigrationops.classification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yongsik.immigrationops.classification.infrastructure.KeywordDocumentClassifier;
import com.yongsik.immigrationops.document.domain.DocumentType;
import org.junit.jupiter.api.Test;

class KeywordDocumentClassifierTest {

    @Test
    void classifiesPassportWhenMrzExists() {
        KeywordDocumentClassifier classifier = new KeywordDocumentClassifier();

        var result = classifier.classify("scan.jpg", "P<KORKIM<<MINSU<<<<<<<<<<<<<<<<<<<<<<");

        assertEquals(DocumentType.PASSPORT, result.documentType());
    }
}

