create table claim_decision (
    claim_id uuid primary key references claim (id) on delete cascade,
    outcome varchar(32) not null,
    automatic boolean not null,
    decided_by varchar(255) not null,
    note varchar(2000),
    reasons jsonb,
    decided_at timestamptz not null
);

create table audit_head (
    id varchar(16) primary key,
    seq bigint not null,
    hash varchar(64) not null
);

insert into audit_head (id, seq, hash) values ('GLOBAL', 0, repeat('0', 64));

create table audit_event (
    seq bigint primary key,
    id uuid not null unique,
    claim_id uuid,
    claim_reference varchar(64),
    actor varchar(255) not null,
    action varchar(64) not null,
    summary varchar(1000) not null,
    details jsonb,
    previous_hash varchar(64) not null,
    hash varchar(64) not null,
    created_at timestamptz not null
);

create index idx_audit_event_claim on audit_event (claim_id, seq);

create or replace function audit_event_append_only() returns trigger as $$
begin
    raise exception 'audit_event is append-only';
end;
$$ language plpgsql;

create trigger audit_event_no_change
    before update or delete on audit_event
    for each row execute function audit_event_append_only();
