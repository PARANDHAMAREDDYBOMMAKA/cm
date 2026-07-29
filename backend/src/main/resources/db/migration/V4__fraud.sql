create extension if not exists vector;

alter table claim_document add column content_sha256 varchar(64);
alter table claim_document add column perceptual_hash bigint;
alter table claim_document add column device_fingerprint varchar(128);

create index idx_claim_document_sha on claim_document (content_sha256);
create index idx_claim_document_phash on claim_document (perceptual_hash);
create index idx_claim_document_fingerprint on claim_document (device_fingerprint);

create table document_forensics (
    id uuid primary key,
    document_id uuid not null unique references claim_document (id) on delete cascade,
    has_metadata boolean not null,
    software varchar(255),
    camera_make varchar(128),
    camera_model varchar(128),
    source_created_at timestamptz,
    source_modified_at timestamptz,
    producer varchar(255),
    ai_generated_score numeric(5, 4),
    ai_detector varchar(128),
    attributes jsonb,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table document_embedding (
    document_id uuid primary key references claim_document (id) on delete cascade,
    claim_id uuid not null references claim (id) on delete cascade,
    model varchar(128) not null,
    embedding vector(768) not null,
    created_at timestamptz not null
);

create index idx_document_embedding_claim on document_embedding (claim_id);

create table fraud_signal (
    id uuid primary key,
    claim_id uuid not null references claim (id) on delete cascade,
    document_id uuid references claim_document (id) on delete set null,
    type varchar(64) not null,
    severity varchar(16) not null,
    weight int not null,
    message varchar(1000) not null,
    details jsonb,
    created_at timestamptz not null
);

create index idx_fraud_signal_claim on fraud_signal (claim_id);

create table claim_risk (
    claim_id uuid primary key references claim (id) on delete cascade,
    score int not null,
    band varchar(16) not null,
    signal_count int not null,
    assessed_at timestamptz not null
);
