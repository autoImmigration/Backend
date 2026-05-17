alter table upload_batch
    add column uploaded_by_user_id bigint references app_user(id),
    add column raw_zip_storage_type varchar(32),
    add column raw_zip_location varchar(500),
    add column raw_zip_checksum varchar(120),
    add column raw_zip_size_bytes bigint;

create table processing_job (
    id bigserial primary key,
    upload_batch_id bigint not null references upload_batch(id),
    external_id varchar(64) not null unique,
    job_type varchar(32) not null,
    status varchar(32) not null,
    provider varchar(64),
    external_job_id varchar(255),
    attempt_no integer not null,
    manifest_storage_type varchar(32),
    manifest_location varchar(500),
    cases_storage_type varchar(32),
    cases_location varchar(500),
    file_count integer not null default 0,
    case_count integer not null default 0,
    error_count integer not null default 0,
    error_code varchar(64),
    error_message text,
    started_at timestamp,
    finished_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint uk_processing_job_batch_attempt unique (upload_batch_id, attempt_no)
);

create index idx_processing_job_batch_attempt on processing_job (upload_batch_id, attempt_no);
