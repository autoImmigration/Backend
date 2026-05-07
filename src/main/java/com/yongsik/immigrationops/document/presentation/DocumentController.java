package com.yongsik.immigrationops.document.presentation;

import com.yongsik.immigrationops.document.application.DocumentProcessingResult;
import com.yongsik.immigrationops.document.application.DocumentProcessingService;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentProcessingService documentProcessingService;

    public DocumentController(DocumentProcessingService documentProcessingService) {
        this.documentProcessingService = documentProcessingService;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    DocumentProcessingResult upload(@RequestPart("file") MultipartFile file) throws IOException {
        return documentProcessingService.process(file);
    }

    @GetMapping
    List<DocumentProcessingResult> list() {
        return documentProcessingService.getAll();
    }

    @GetMapping("/{id}")
    DocumentProcessingResult get(@PathVariable String id) {
        return documentProcessingService.getById(id);
    }
}

