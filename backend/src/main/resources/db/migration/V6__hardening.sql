alter table claim add column owner_subject varchar(255);
alter table claim add column owner_org varchar(255);

create index idx_claim_owner_subject on claim (owner_subject);
create index idx_claim_owner_org on claim (owner_org);
create index idx_claim_created_at on claim (created_at desc);

with ranked as (
    select id, reference, row_number() over (partition by reference order by created_at, id) as position
    from claim
)
update claim c
set reference = left(c.reference, 54) || '-' || substr(replace(c.id::text, '-', ''), 1, 8)
from ranked r
where c.id = r.id and r.position > 1;

create unique index uq_claim_reference on claim (reference);

alter table claim_document add column fingerprinted_at timestamptz;

update claim_document set fingerprinted_at = created_at where content_sha256 is not null;

alter table document_extraction add column attempts int not null default 0;
alter table document_extraction add column lease_owner varchar(128);
alter table document_extraction add column lease_expires_at timestamptz;
alter table document_extraction add column next_attempt_at timestamptz;

create index idx_document_extraction_status on document_extraction (status);
create index idx_document_extraction_invoice on document_extraction (lower(invoice_number));

create table audit_checkpoint (
    id varchar(16) primary key,
    verified_seq bigint not null,
    verified_hash varchar(64) not null,
    intact boolean not null,
    detail varchar(1000),
    broken_seq bigint,
    verified_at timestamptz not null
);

insert into audit_checkpoint (id, verified_seq, verified_hash, intact, detail, broken_seq, verified_at)
values ('GLOBAL', 0, repeat('0', 64), true, 'No audit entries verified yet.', null, now());

create index idx_audit_event_created_at on audit_event (created_at desc);
