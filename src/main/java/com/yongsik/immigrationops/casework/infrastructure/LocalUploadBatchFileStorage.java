package com.yongsik.immigrationops.casework.infrastructure;

import com.yongsik.immigrationops.casework.application.StoredUploadBatchFile;
import com.yongsik.immigrationops.casework.application.UploadBatchFileStorage;
import com.yongsik.immigrationops.casework.domain.ArtifactStorageType;
import com.yongsik.immigrationops.common.BadRequestException;
import com.yongsik.immigrationops.security.SecurityProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LocalUploadBatchFileStorage implements UploadBatchFileStorage {

    private static final DateTimeFormatter DIRECTORY_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final Path rootPath;

    public LocalUploadBatchFileStorage(SecurityProperties properties) {
        this.rootPath = Path.of(properties.storage().rootPath())
                .toAbsolutePath()
                .normalize()
                .resolve("upload-batches");
    }

    @Override
    public StoredUploadBatchFile store(MultipartFile file) {
        String originalFilename = normalizeZipFilename(file.getOriginalFilename());

        try {
            Files.createDirectories(rootPath);
            Path datedDirectory = rootPath.resolve(DIRECTORY_FORMAT.format(LocalDate.now()));
            Files.createDirectories(datedDirectory);

            String storedFilename = UUID.randomUUID() + "-" + sanitizeFilename(originalFilename);
            Path target = datedDirectory.resolve(storedFilename).normalize();
            if (!target.startsWith(rootPath)) {
                throw new IllegalStateException("Resolved upload path escaped storage root");
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long sizeBytes;
            try (InputStream inputStream = new DigestInputStream(file.getInputStream(), digest)) {
                sizeBytes = Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return new StoredUploadBatchFile(
                    originalFilename,
                    ArtifactStorageType.LOCAL_FILE.name(),
                    target.toString(),
                    "sha256:" + HexFormat.of().formatHex(digest.digest()),
                    sizeBytes
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store upload batch ZIP locally", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }

    private String normalizeZipFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("ZIP filename is required");
        }

        String safeFilename = Path.of(originalFilename).getFileName().toString().trim();
        if (safeFilename.isBlank()) {
            throw new BadRequestException("ZIP filename is required");
        }
        if (!safeFilename.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new BadRequestException("Only .zip files are supported");
        }
        return safeFilename;
    }

    private String sanitizeFilename(String originalFilename) {
        String sanitized = originalFilename.replaceAll("[^A-Za-z0-9._-]", "_");
        return sanitized.isBlank() ? "upload.zip" : sanitized;
    }
}
