package com.yongsik.immigrationops.casework.application;

import org.springframework.web.multipart.MultipartFile;

public interface UploadBatchFileStorage {

    StoredUploadBatchFile store(MultipartFile file);
}
