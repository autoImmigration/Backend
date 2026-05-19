ALTER TABLE case_document_requirement
    ADD COLUMN IF NOT EXISTS source_filename VARCHAR(255);
