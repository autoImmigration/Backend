package com.yongsik.immigrationops.casework.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yongsik.immigrationops.security.SecurityProperties;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalUploadBatchFileStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void storePersistsZipAndReturnsMetadata() throws Exception {
        LocalUploadBatchFileStorage storage = new LocalUploadBatchFileStorage(new SecurityProperties(
                new SecurityProperties.Cors(List.of()),
                new SecurityProperties.Storage(tempDir.toString()),
                new SecurityProperties.Credentials("agency-ops", "demo1234")
        ));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "agency batch.zip",
                "application/zip",
                "abc".getBytes(StandardCharsets.UTF_8)
        );

        var storedFile = storage.store(file);
        Path storedPath = Path.of(storedFile.location());

        assertEquals("agency batch.zip", storedFile.originalFilename());
        assertEquals("LOCAL_FILE", storedFile.storageType());
        assertEquals("sha256:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", storedFile.checksum());
        assertEquals(3L, storedFile.sizeBytes());
        assertTrue(storedPath.startsWith(tempDir.toAbsolutePath().normalize().resolve("upload-batches")));
        assertTrue(Files.exists(storedPath));
        assertEquals("abc", Files.readString(storedPath));
    }
}
