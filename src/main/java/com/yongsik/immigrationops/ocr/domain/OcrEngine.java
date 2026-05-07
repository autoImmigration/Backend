package com.yongsik.immigrationops.ocr.domain;

import com.yongsik.immigrationops.storage.domain.StoredFile;
import java.io.IOException;

public interface OcrEngine {

    OcrResult extract(StoredFile storedFile) throws IOException;
}

