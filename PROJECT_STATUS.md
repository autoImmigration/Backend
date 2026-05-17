# Immigration Ops Backend Status

Updated: 2026-05-12

## Purpose

This file is the working source of truth for `immigration-ops-backend`.

Every time project work changes, update this file as part of the same task.

- Move completed items into the done section.
- Remove obsolete TODOs.
- Shrink the remaining work list instead of rewriting it loosely.
- On every future request for this project, read this file first.
- Write updates so another engineer can continue from this file alone without needing prior chat context.
- Record not only what changed, but also why it changed, why unfinished items remain, and why the next priority is the next priority.

Required context order for future work:

1. `PROJECT_STATUS.md`
2. `기획서.md`
3. `DB_ERD.md`
4. If needed: infra architecture notes and frontend project files

## Product Direction Locked In

- The core business unit is `application_case`, not just `student`.
- One student can have multiple application cases.
- Visa requirements and document requirements are reference data.
- Submitted documents and required documents must stay separate.
- Spring owns upload batch intake, persistence, status calculation, and query APIs.
- Python owns OCR, document classification, MRZ extraction, field extraction, and manifest generation.
- The current phase is: keep everything working without Python OCR, while shaping Spring around the future manifest flow.

## Done

### Backend foundation

- Spring Boot 3.3.5 / Java 21 project is in place.
- Basic Auth is in place.
- `GET /api/v1/auth/me` exists.
- Global API exception handling exists.
- Docker-based local PostgreSQL runtime is in place through `docker-compose.yml`.
- Spring Data JPA and Flyway are now part of the backend runtime.

### Casework read model

- Added `casework` module split by domain/application/infrastructure/presentation.
- Added APIs:
  - `POST /api/v1/student-access/lookup`
  - `GET /api/v1/school/students`
  - `GET /api/v1/agency/application-cases`
  - `GET /api/v1/agency/application-cases/{caseId}`
  - `GET /api/v1/agency/upload-batches`
  - `GET /api/v1/agency/upload-batches/{batchId}`
- Student lookup is public; the rest of `/api/**` remains behind Basic Auth.
- Added service-level tests for casework queries.
- The active `CaseworkQueryRepository` is now DB-backed.
- The old in-memory casework repository remains only as a local seed fixture and unit-test helper.

### Database persistence completed for current read scope

- Added PostgreSQL schema with Flyway for:
  - `organization`
  - `app_user`
  - `student`
  - `visa_type`
  - `document_type`
  - `visa_document_requirement`
  - `application_case`
  - `case_document_requirement`
  - `upload_batch`
  - `upload_batch_preview_file`
- Added local development seed initializer that loads the current dummy casework data into PostgreSQL on first boot.
- Added DB-backed auth users for local testing:
  - `admin / change-me`
  - `school-admin / demo1234`
  - `agency-ops / demo1234`
- The current backend now reads student lookup, school list, agency case list/detail, and upload batch list/detail from PostgreSQL.

### Spring OCR cleanup completed in active path

- Removed the old Spring OCR HTTP entry path:
  - `/api/v1/documents`
- Removed the old document processing flow classes that backed that route.
- Default OCR provider is now `stub`, so Spring does not expect a live OpenAI OCR path by default.
- Current active backend flow no longer depends on Spring OCR request handling.

### Frontend integration completed for current read flows

- Frontend now calls the real backend instead of only local mock lists for:
  - student lookup
  - school student list
  - agency application list/detail
  - agency upload batch list/detail
- Frontend API base URL is standardized to `http://localhost:8080/api/v1`.
- Added local frontend `.env` with `VITE_API_BASE_URL=http://localhost:8080/api/v1`.
- Frontend demo defaults now use role-specific backend users again:
  - school: `school-admin / demo1234`
  - agency: `agency-ops / demo1234`
- Backend CORS now explicitly allows local Vite origins:
  - `http://localhost:5173`
  - `http://127.0.0.1:5173`
  - `http://localhost:4173`
  - `http://127.0.0.1:4173`
- Security now permits CORS preflight requests, so browser calls to protected APIs can complete.
- `application-local.yml` is aligned with the non-Python path:
  - local OCR provider is `stub`
  - local CORS origins match the frontend dev and preview ports
  - local OpenAI key is no longer hardcoded
- Fixed frontend state recalculation bug after API login:
  - school list filters now recompute when `schoolStudents` changes
  - agency dashboard filters now recompute when `agencyApplications` changes

### Local runtime contract

- Local Docker PostgreSQL is exposed on `localhost:5433`.
- Default Spring datasource points to `jdbc:postgresql://localhost:5433/immigration_ops`.
- Local startup path is:
  1. `docker compose up -d postgres`
  2. `./gradlew bootRun`

### Verification completed

- `./gradlew test` passes.
- `npm run build` passes.
- Docker PostgreSQL starts successfully and becomes healthy.
- Flyway migration succeeds against the Docker PostgreSQL instance.
- Seeded DB row counts were verified:
  - `organization`: 6
  - `app_user`: 3
  - `student`: 5
  - `application_case`: 7
  - `case_document_requirement`: 68
  - `upload_batch`: 3
- Runtime API smoke tests against `localhost:8080` succeed for:
  - `POST /api/v1/student-access/lookup`
  - `GET /api/v1/auth/me`
  - `GET /api/v1/school/students`
  - `GET /api/v1/agency/application-cases`
  - `GET /api/v1/agency/upload-batches`

### Python integration context confirmed

- Reviewed the current `immigration-doc-classifier` project structure.
- Confirmed the current Python project has two separate integration shapes:
  - single-file APIs for classification and OCR/classification
  - batch CLI flow for `manifest.json` generation and `cases.json` generation
- Confirmed the Spring `upload_batch` API should keep the same external contract in local and AWS.
- Confirmed the infrastructure behind that API will differ by environment:
  - local: local file/process driven
  - AWS: S3 + queue/worker driven
- Confirmed the current Spring write-side design must align to actual Python outputs, not only the original ERD assumption.

### Python handoff contract fixed for v1

- Added `PYTHON_BATCH_HANDOFF_CONTRACT.md`.
- Locked the near-term handoff decision:
  - Spring creates `upload_batch`
  - Python keeps emitting `manifest.json` and `cases.json`
  - Spring ingests both artifacts in one callback flow
- Kept the merged-artifact approach as a later v2 option instead of a blocker
  for the current write-side work.

### Upload batch command path implemented

- Added `POST /api/v1/agency/upload-batches`.
- Added `POST /api/v1/agency/upload-batches/file`.
- The JSON create API still accepts raw ZIP storage metadata directly:
  - `originalFilename`
  - `rawZipStorageType`
  - `rawZipLocation`
  - `rawZipChecksum`
  - `rawZipSizeBytes`
- The new multipart API accepts `multipart/form-data` with:
  - `file`
  - optional `note`
- Multipart ZIP uploads are now validated for:
  - empty file
  - `.zip` filename extension
- Accepted ZIP files are stored on the server local filesystem under the
  configured `app.storage.root-path`.
- Spring computes and persists raw ZIP metadata from that saved file:
  - local storage type
  - local storage location
  - SHA-256 checksum
  - byte size
- New `upload_batch` rows are now created through the active backend path.
- An initial `processing_job` row is created together with each new batch.
- Newly created upload batches are visible through the existing DB-backed
  agency batch list/detail GET flows.

### Python callback skeleton implemented

- Added `POST /api/v1/internal/upload-batches/{uploadBatchId}/python-results`.
- The callback now stores:
  - Python job status
  - artifact storage types and locations for `manifest.json` and `cases.json`
  - provider metadata
  - file/case/error counts
  - started/finished timestamps
- Batch status now moves from `UPLOADED` into:
  - `RESULT_UPLOADED` on `SUCCEEDED`
  - `NEEDS_REVIEW` on `PARTIAL_SUCCESS`
  - `FAILED` on `FAILED`
- This is still a write-side skeleton only:
  - Spring does not yet parse and persist normalized manifest/case records
  - Spring does not yet create `submitted_document`, `ocr_result`, or
    `review_task` rows from the callback

### Write-side persistence foundation added

- Added Flyway `V2__add_upload_batch_write_side.sql`.
- `upload_batch` now stores raw ZIP metadata fields for the current command API.
- Added `processing_job` persistence for queue/result tracking.
- Local seed upload batches now populate the new raw ZIP metadata fields.

### Verification completed for upload-batch command slice

- Added `AgencyCommandServiceTest`.
- Added `AgencyUploadBatchCommandControllerTest`.
- Added `LocalUploadBatchFileStorageTest`.
- Added `UploadBatchWriteReadBridgeTest`.
- `./gradlew test` passes after the multipart upload slice changes.

## Current State

What works now, excluding Python OCR:

- Student can look up their application cases.
- School can read student case lists.
- Agency can read case lists, case detail, upload batch lists, and upload batch detail.
- Frontend is wired to the backend through `VITE_API_BASE_URL`.
- Local browser CORS is set for Vite dev and preview ports.
- Data is now persisted in Docker PostgreSQL instead of staying only in memory.
- Local auth is now DB-backed through `app_user`.
- The current Python side already has a concrete batch result shape:
  - `manifest.json` for per-file OCR/classification output
  - `cases.json` for grouped application-case output
- The backend can now create new upload batches and record Python callback
  metadata for them.
- The backend still does not ingest `manifest.json` and `cases.json` into the
  normalized review/document tables.

How the current data actually works:

- The frontend is not reading old frontend mock lists for the active read flows.
- The frontend calls real Spring APIs, and Spring returns real controller/service/repository responses.
- The current PostgreSQL data is still local dummy data, not user-created production data.
- On local startup, if the DB is empty, Spring seeds PostgreSQL from the old in-memory fixture data.
- This means the current runtime path is:
  - frontend request
  - Spring controller/service
  - JPA repository
  - PostgreSQL
  - seeded dummy rows returned to the client
- So the read path is implemented, but the source rows are still seeded dummy data.

What is not active anymore:

- The old Spring-driven `/api/v1/documents` OCR request flow.

What still exists as legacy code but is not the active product path:

- `ocr/*`
- `classification/*`
- `passport/*`
- `review/*`
- `template/*`
- old `document/domain/*` model types

Those packages should be treated as legacy and removed or replaced only in line with the new `upload_batch + manifest` design.

## Remaining Work

### Persistence and DB

- Expand persistence from the current read scope into the remaining write/processing scope.
- Add the rest of the ERD tables needed for Python manifest ingestion and review workflows.

### Reference data

- Build real `case_document_requirement` snapshot creation logic from `visa_document_requirement` during case creation.
- Add admin/update flows for reference data changes when dummy data is replaced.

### Write-side application flow

- Create application case write flows.
- Create student and master-data correction flows.
- Separate real roles and permissions for school and agency users.
- The current backend does not yet have real write-side APIs for:
  - application case creation
  - user-driven master data updates

### Upload batch and Python handoff

- Keep the v1 contract in `PYTHON_BATCH_HANDOFF_CONTRACT.md`.
- Keep current Python outputs as-is:
  - batch OCR/classification emits `manifest.json`
  - case grouping emits `cases.json`
- Extend the current callback from metadata capture into full normalized ingest.
- Create storage and persistence for:
  - `upload_batch_segment`
  - `submitted_document`
  - `document_file`
  - `ocr_result`
  - `ocr_extracted_field`
  - `review_task`
  - `review_correction`
  - `audit_event`

### Infra

- S3 integration
- SQS and worker split
- ECS and Fargate deployment
- secret management

## Priority Order From Here

1. Add the remaining persistence for:
   - `submitted_document`
   - `document_file`
   - `ocr_result`
   - `ocr_extracted_field`
   - `review_task`
   - `review_correction`
   - `audit_event`
   Next backend target:
   - parse `manifest.json` and `cases.json`
   - write normalized batch, document, OCR, and review records
2. Implement application case write flows and requirement snapshot generation.
3. Separate real authorization rules by organization and role.
4. Remove the remaining legacy Spring OCR modules once the manifest path is real.

## Working Rules For Future Requests

- Read `PYTHON_BATCH_HANDOFF_CONTRACT.md` before changing upload-batch or Python-ingest behavior.
- Read this file first on every task for this project.
- Then read `기획서.md` and `DB_ERD.md` before changing behavior.
- Reason in terms of `application_case`, not just `student`.
- Keep Python and Spring responsibilities separate.
- Update this file every time project state changes.
- When updating this file, write for handoff, not memory:
  - describe the current runtime behavior concretely
  - state whether the current data is real, seeded, mocked, or inferred
  - state which paths are read-only, which paths support writes, and which paths are still planned
  - name the main code entry points when they matter for the next task
  - explain decision reasons, not only decision results
  - explain why unfinished work is still unfinished
  - explain why the listed next step should come before the others
