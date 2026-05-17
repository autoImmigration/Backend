# Python Batch Handoff Contract

Updated: 2026-05-12

## Purpose

This document fixes the near-term contract between:

- `immigration-ops-backend` as the system-of-record API
- `immigration-doc-classifier` as the OCR/classification batch worker

The goal is to unblock the backend write side without first rewriting the
current Python batch outputs.

## Decision Summary

The contract for v1 is:

1. Spring creates and owns `upload_batch`.
2. Python processes the uploaded ZIP.
3. Python keeps producing the current two artifacts:
   - `manifest.json`
   - `cases.json`
4. Spring ingests both artifacts in one callback flow.
5. Spring persists normalized DB records from those artifacts.

This is intentionally a v1 decision for the current codebase.

- It matches the Python project as it exists today.
- It avoids blocking backend work on a Python artifact redesign.
- It still leaves room for a future v2 merged artifact after the write path is real.

## Why We Are Not Choosing A Merged Artifact First

The backend ERD already sketches a future single segmented manifest shape.
That is still a reasonable v2 target.

However, the current Python code already emits:

- `manifest.json` from `batch_ocr_sort.py`
- `cases.json` from `group_cases.py`

The backend should not wait for Python to collapse those into one file before
the write path begins.

## Artifact Ownership

### `manifest.json` is authoritative for

- file order
- per-file OCR text and provider metadata
- per-file classification result
- per-file extracted fields
- raw low-level processing status

Current Python shape to preserve:

- top-level `input_dir`, `output_dir`, `provider`, `file_count`, `results`
- `results[].order`
- `results[].filename`
- `results[].status`
- `results[].ocr`
- `results[].classification`
- `results[].extracted_fields`

### `cases.json` is authoritative for

- case and segment boundaries
- case-level document grouping
- application type inference
- normalized `student_profile`
- matched or missing required documents
- case-level `other_documents`

Current Python shape to preserve:

- top-level `source_manifests`, `global_document_count`, `case_count`, `cases`
- `cases[].case_id`
- `cases[].start_order`
- `cases[].end_order`
- `cases[].application_type`
- `cases[].student_profile`
- `cases[].documents[].global_order`

## Conflict Rules

Spring must validate both artifacts before persistence.

Validation rules:

1. Every `cases[].documents[].global_order` must exist in `manifest.results[].order`.
2. `cases.global_document_count` must equal `manifest.file_count`.
3. If the same file appears with different document codes between the two
   artifacts, Spring treats that as ingest validation failure.
4. If validation fails, Spring marks the processing job as failed and does not
   write partial normalized case data.

Conflict resolution rule:

- `manifest.json` wins for per-file OCR/classification details.
- `cases.json` wins for segment boundaries and case-level grouping.

## Storage Contract

The callback payload must not assume only one storage backend.

Spring will support these artifact reference types:

- `LOCAL_FILE`
- `S3_OBJECT`

Artifact references:

- `manifest_location`
- `cases_location`

Examples:

- local: `C:/.../processed/BATCH-001/manifest.json`
- aws: `processed/BATCH-001/manifest.json`

The same callback DTO shape must be used in local and AWS.

## Callback Contract

Recommended internal endpoint:

- `POST /api/v1/internal/upload-batches/{uploadBatchId}/python-results`

Recommended request body:

```json
{
  "processingJobId": "job-local-20260512-001",
  "status": "SUCCEEDED",
  "provider": "openai",
  "manifestStorageType": "LOCAL_FILE",
  "manifestLocation": "C:/work/processed/BATCH-001/manifest.json",
  "casesStorageType": "LOCAL_FILE",
  "casesLocation": "C:/work/processed/BATCH-001/cases.json",
  "fileCount": 18,
  "caseCount": 2,
  "errorCount": 0,
  "startedAt": "2026-05-12T10:15:30Z",
  "finishedAt": "2026-05-12T10:18:04Z"
}
```

Status values for v1:

- `SUCCEEDED`
- `PARTIAL_SUCCESS`
- `FAILED`

Notes:

- `PARTIAL_SUCCESS` means Python produced artifacts, but one or more files are
  in `error` status inside `manifest.json`.
- Spring still validates and ingests `PARTIAL_SUCCESS` batches.
- `FAILED` means Spring should record job failure metadata and skip normalized
  ingest.

## Spring Persistence Mapping

### Spring writes immediately on upload creation

- `upload_batch`
- initial `processing_job`

### Spring writes after Python callback validation

- `processing_job` result fields
- `upload_batch_segment`
- `submitted_document`
- `document_file`
- `ocr_result`
- `ocr_extracted_field`
- `review_task`

### Initial review-task generation rules

Generate `review_task` records for:

- `other_document`
- low classification confidence
- low extraction confidence when present
- missing required documents from `cases.json`
- student match failures or ambiguous matching

## Ingest Sequence

1. Spring receives ZIP upload and creates `upload_batch`.
2. Spring starts a `processing_job`.
3. Python processes the ZIP and writes `manifest.json`.
4. Python derives `cases.json`.
5. Python sends one callback with both artifact references.
6. Spring loads both artifacts from the declared storage backend.
7. Spring validates cross-file consistency.
8. Spring persists normalized records in one transaction boundary per batch
   ingest step.
9. Spring updates `upload_batch.status` and `processing_job.status`.

## What This Unblocks Next

Once this document is accepted, backend work should proceed in this order:

1. Add callback DTOs and storage resolver interfaces.
2. Add `upload_batch` create API.
3. Add write-side tables for batch segments and submitted documents.
4. Add artifact ingest service for `manifest.json` and `cases.json`.
5. Add first-pass review task generation.

## Future v2 Direction

After the write path is working end-to-end, the system may move to a merged
artifact. If that happens:

- keep the Spring callback endpoint stable
- keep the upload batch external API stable
- change only the Python-produced artifact contract behind the callback

Until then, v1 is the locked implementation target.
