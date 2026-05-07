package com.yongsik.immigrationops.storage.domain;

import com.yongsik.immigrationops.document.domain.DocumentId;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {

    StoredFile store(DocumentId documentId, MultipartFile file) throws IOException;
}

