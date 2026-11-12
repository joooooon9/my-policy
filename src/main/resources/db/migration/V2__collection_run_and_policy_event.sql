-- One row per collection of one source. A source's first successful run fills the list without new-notice events.
create table collection_run (
    id          bigserial    primary key,
    source      varchar(50)  not null,
    started_at  timestamptz  not null,
    finished_at timestamptz,
    status      varchar(20)  not null,
    first_run   boolean      not null default false,
    fetched     integer      not null default 0,
    created     integer      not null default 0,
    updated     integer      not null default 0,
    cancelled   integer      not null default 0,
    error       text
);

create index ix_collection_run_source_status on collection_run (source, status);

-- What changed in a notice, for the notifications to read later (new notice, deadline change, cancellation).
create table policy_event (
    id                      bigserial    primary key,
    policy_id               bigint       not null references policy (id) on delete cascade,
    type                    varchar(30)  not null,
    previous_apply_end      date,
    previous_apply_end_time time,
    apply_end               date,
    apply_end_time          time,
    collection_run_id       bigint       references collection_run (id),
    created_at              timestamptz  not null
);

create index ix_policy_event_policy on policy_event (policy_id);
create index ix_policy_event_created on policy_event (created_at);

-- A notice is new once and cancelled once, however often a collection or a retry runs.
create unique index ux_policy_event_new_once on policy_event (policy_id) where type = 'NEW';
create unique index ux_policy_event_cancelled_once on policy_event (policy_id) where type = 'CANCELLED';
