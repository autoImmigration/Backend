package com.yongsik.immigrationops.storage.infrastructure;

import com.yongsik.immigrationops.document.domain.DocumentId;
import com.yongsik.immigrationops.security.SecurityProperties;
import com.yongsik.immigrationops.storage.domain.FileStorage;
import com.yongsik.immigrationops.storage.domain.StoredFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LocalFileStorage implements FileStorage {

    private final Path rootPath;

    public LocalFileStorage(SecurityProperties properties) {
        this.rootPath = Path.of(properties.storage().rootPath()).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(DocumentId documentId, MultipartFile file) throws IOException {
        Files.createDirectories(rootPath);
        String safeFilename = file.getOriginalFilename() == null ? "upload.bin" : Path.of(file.getOriginalFilename()).getFileName().toString();
        Path target = rootPath.resolve(documentId.value() + "-" + safeFilename);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
        String mediaType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        return new StoredFile(safeFilename, mediaType, target);
    }
}

