create table claim (
    id uuid primary key,
    reference varchar(64) not null,
    status varchar(32) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table claim_document (
    id uuid primary key,
    claim_id uuid not null references claim (id) on delete cascade,
    original_filename varchar(512) not null,
    content_type varchar(255),
    size_bytes bigint not null,
    storage_key varchar(1024) not null,
    created_at timestamptz not null
);

create index idx_claim_document_claim_id on claim_document (claim_id);
