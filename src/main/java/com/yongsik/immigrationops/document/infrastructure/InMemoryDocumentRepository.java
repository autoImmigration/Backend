package com.yongsik.immigrationops.document.infrastructure;

import com.yongsik.immigrationops.document.domain.DocumentId;
import com.yongsik.immigrationops.document.domain.DocumentRepository;
import com.yongsik.immigrationops.document.domain.SubmissionDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryDocumentRepository implements DocumentRepository {

    private final ConcurrentHashMap<String, SubmissionDocument> storage = new ConcurrentHashMap<>();

    @Override
    public SubmissionDocument save(SubmissionDocument document) {
        storage.put(document.id().value(), document);
        return document;
    }

    @Override
    public Optional<SubmissionDocument> findById(DocumentId id) {
        return Optional.ofNullable(storage.get(id.value()));
    }

    @Override
    public List<SubmissionDocument> findAll() {
        return new ArrayList<>(storage.values());
    }
}

