package com.yongsik.immigrationops.document.domain;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository {

    SubmissionDocument save(SubmissionDocument document);

    Optional<SubmissionDocument> findById(DocumentId id);

    List<SubmissionDocument> findAll();
}

