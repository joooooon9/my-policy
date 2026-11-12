-- Policy notices: one row per notice, found again by its source and the source's own id.
create table policy (
    id                  bigserial primary key,
    source              varchar(50)   not null,
    source_id           varchar(200)  not null,
    region              varchar(20)   not null,
    title               varchar(300)  not null,
    summary             text          not null,
    benefit             varchar(200),
    min_age             integer,
    max_age             integer,
    employed_only       boolean       not null default false,
    small_business_only boolean       not null default false,
    apply_start         date,
    apply_end           date,
    -- only when the official notice states a closing time; never filled in by guess
    apply_end_time      time,
    official_url        varchar(1000),
    apply_url           varchar(1000),
    status              varchar(20)   not null,
    last_checked_at     timestamptz,
    created_at          timestamptz   not null,
    updated_at          timestamptz   not null,
    constraint uq_policy_source unique (source, source_id),
    constraint ck_policy_age check (min_age is null or max_age is null or min_age <= max_age)
);

create index ix_policy_region on policy (region);
create index ix_policy_apply_end on policy (apply_end);

-- Conditions the matcher cannot decide from the search form (income, tenure, ...): shown, never used to exclude.
create table policy_check_note (
    policy_id bigint       not null references policy (id) on delete cascade,
    position  integer      not null,
    note      varchar(200) not null,
    primary key (policy_id, position)
);
