alter table upload_batch
    add column school_id        bigint references organization(id),
    add column images_dir       varchar(500),
    add column output_dir       varchar(500);
