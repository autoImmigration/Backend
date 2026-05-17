create table organization (
    id bigserial primary key,
    type varchar(32) not null,
    name varchar(120) not null,
    business_registration_no varchar(32),
    contact_email varchar(120),
    contact_phone varchar(40),
    created_at timestamp not null,
    updated_at timestamp not null
);

create table app_user (
    id bigserial primary key,
    organization_id bigint references organization(id),
    role varchar(64) not null,
    username varchar(80) not null unique,
    password_hash varchar(255) not null,
    display_name varchar(120) not null,
    status varchar(32) not null,
    last_login_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table visa_type (
    id bigserial primary key,
    code varchar(64) not null unique,
    name varchar(120) not null,
    description text,
    active boolean not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table document_type (
    id bigserial primary key,
    code varchar(64) not null unique,
    name varchar(120) not null,
    category varchar(80) not null,
    review_rule text,
    active boolean not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table student (
    id bigserial primary key,
    external_id varchar(64) not null unique,
    school_id bigint not null references organization(id),
    agency_id bigint references organization(id),
    name varchar(120) not null,
    nationality varchar(80) not null,
    birth_date date not null,
    passport_number varchar(64) not null,
    alien_registration_number varchar(64),
    phone_number varchar(64),
    address text,
    school_department varchar(120),
    term varchar(120),
    created_at timestamp not null,
    updated_at timestamp not null
);

create table application_case (
    id bigserial primary key,
    external_id varchar(64) not null unique,
    student_id bigint not null references student(id),
    school_id bigint not null references organization(id),
    agency_id bigint references organization(id),
    visa_type_id bigint not null references visa_type(id),
    application_kind varchar(64) not null,
    status varchar(64) not null,
    application_date date not null,
    lane varchar(160),
    note text,
    intake_batch varchar(160),
    coordinator_name varchar(120),
    submitted_document_count integer not null,
    missing_document_count integer not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table visa_document_requirement (
    id bigserial primary key,
    visa_type_id bigint not null references visa_type(id),
    document_type_id bigint not null references document_type(id),
    required boolean not null,
    display_order integer not null,
    note text,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint uk_visa_document_requirement unique (visa_type_id, document_type_id)
);

create table case_document_requirement (
    id bigserial primary key,
    application_case_id bigint not null references application_case(id),
    document_type_id bigint not null references document_type(id),
    source_requirement_id bigint references visa_document_requirement(id),
    required boolean not null,
    display_order integer not null,
    status varchar(64) not null,
    submitted_at date,
    note text,
    preview_text text,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint uk_case_document_requirement unique (application_case_id, document_type_id)
);

create table upload_batch (
    id bigserial primary key,
    external_id varchar(64) not null unique,
    original_filename varchar(255) not null,
    status varchar(64) not null,
    detected_student_count integer not null,
    note text,
    failure_reason text,
    uploaded_at timestamp not null,
    completed_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table upload_batch_preview_file (
    id bigserial primary key,
    external_id varchar(64) not null unique,
    upload_batch_id bigint not null references upload_batch(id),
    display_order integer not null,
    student_name varchar(120) not null,
    document_name varchar(120) not null,
    page_range varchar(80) not null,
    note text,
    created_at timestamp not null
);

create index idx_student_lookup on student (passport_number, birth_date);
create index idx_application_case_student_status on application_case (student_id, status);
create index idx_application_case_agency_status_updated on application_case (agency_id, status, updated_at);
create index idx_upload_batch_status_created_at on upload_batch (status, created_at);
create index idx_case_document_requirement_case_order on case_document_requirement (application_case_id, display_order);
create index idx_upload_batch_preview_file_batch_order on upload_batch_preview_file (upload_batch_id, display_order);
