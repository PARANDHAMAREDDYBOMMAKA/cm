create table document_extraction (
    id uuid primary key,
    document_id uuid not null unique references claim_document (id) on delete cascade,
    status varchar(32) not null,
    model varchar(128),
    document_type varchar(64),

    patient_name varchar(255),
    patient_age varchar(32),
    patient_gender varchar(32),
    patient_id varchar(128),

    provider_name varchar(255),
    provider_address varchar(512),

    diagnosis varchar(1024),
    procedures jsonb,

    admission_date date,
    discharge_date date,
    invoice_number varchar(128),
    invoice_date date,

    total_amount numeric(14, 2),
    currency varchar(8),

    confidence jsonb,
    edited_fields jsonb,
    raw_response jsonb,
    error varchar(2000),

    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table extraction_line_item (
    id uuid primary key,
    extraction_id uuid not null references document_extraction (id) on delete cascade,
    position int not null,
    description varchar(512),
    code varchar(64),
    quantity numeric(12, 3),
    unit_amount numeric(14, 2),
    amount numeric(14, 2)
);

create index idx_extraction_line_item_extraction_id on extraction_line_item (extraction_id);
