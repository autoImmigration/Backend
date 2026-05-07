# OCR Internal Ops System, Phase 1 Architecture Plan

## Recommendation

Phase 1 is an internal system. Keep the architecture boring.

- one admin web
- one admin API
- one OCR worker
- one managed PostgreSQL
- one async queue
- object storage for raw and derived files

Do not split into many services yet. The real problem is reliable upload, OCR, classification, field extraction, and review routing.

## Target Topology

```text
Agency Staff
  -> SSO / internal login
  -> Admin Web
  -> Admin API
  -> signed upload or backend upload
  -> raw document storage

Raw upload event
  -> async queue
  -> OCR worker
  -> preprocess
  -> classify
  -> extract fields
  -> validate
  -> confidence score
  -> READY or REVIEW_REQUIRED

Reviewer
  -> Admin Web
  -> review queue
  -> correction
  -> final normalized record
```

## Environment Topology

- separate `dev`, `staging`, `prod`
- single region in phase 1
- internal-only exposure
- service-to-service access should stay private where possible
- keep web, API, worker as separate deploy units

## Core Components

### Web and API

- Admin Web: React
- Admin API: Spring Boot
- Security: internal login first, SSO later

### Data

- managed PostgreSQL for metadata
- object storage for raw files and derived artifacts
- separate storage access policy for raw files vs extracted structured data

### Async Processing

- upload creates a document record
- upload completion emits a queue message
- OCR worker processes one document at a time
- retries with backoff
- dead-letter queue after repeated failure

## Storage Layout

- `raw-documents`: original uploads
- `derived-artifacts`: previews, page images, crops, OCR snapshots
- `quarantine`: corrupt or blocked files

## High-Level Data Model

- `document_batch`
- `document`
- `processing_job`
- `document_type_candidate`
- `extraction_result`
- `review_task`
- `review_correction`
- `audit_event`
- `template_definition`

## Pipeline Stages

1. ingest
2. preprocess
3. coarse classification
4. OCR extraction
5. validation
6. confidence scoring
7. routing

## Processing Rules

- document-level jobs first, not page-level parallelism
- OCR provider must stay behind an adapter boundary
- human correction data should be stored as future quality data
- review queue is part of the main design, not a later patch

## Security Boundaries

- no anonymous access
- short-lived signed URLs or backend proxy for preview/download
- least-privilege service accounts
- secrets manager for OCR keys
- audit logs for read, download, approval, and reprocess actions
- do not log passport number or student PII directly

## Observability

- upload success rate
- queue lag
- processing latency by stage
- retry rate
- dead-letter count
- review-required rate
- auto-accept rate
- correction rate by document type

## Rollout

### Phase 0

- environments
- storage
- database
- queue
- secrets
- CI/CD

### Phase 1

- internal upload
- OCR
- classification
- field extraction
- validation
- review queue

### Phase 1.5

- reprocess tools
- retries and DLQ hardening
- dashboards
- backup restore test

### Not In Scope Yet

- external portals
- customer APIs
- multi-region active-active
- full training platform
