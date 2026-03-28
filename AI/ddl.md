create table public.account
(
    id                     varchar(30)           not null
        primary key,
    created_at             timestamp(6)          not null,
    is_deleted             boolean default false not null,
    updated_at             timestamp(6)          not null,
    account_no             varchar(20)           not null,
    account_type_unique_no varchar(30),
    alias                  varchar(30),
    bank_code              varchar(30)           not null,
    bank_name              varchar(30)           not null,
    is_verified            boolean default false not null,
    users_id               integer               not null
        constraint fkjhlf5avsj15dfty4q7loqt7i4
            references ??? (),
    account_name           varchar(60),
    account_type_name      varchar(30),
    currency               varchar(10)
);

alter table public.account
    owner to d101;

create table public.account_transaction_override
(
    transaction_after_balance bigint      not null,
    transaction_summary       varchar(30) not null,
    transaction_type          integer     not null,
    transaction_type_name     varchar(20) not null,
    transaction_unique_no     bigint      not null,
    id                        integer     not null
        primary key
        constraint fk6koryu4m2twkma5lew1mphbf1
            references ??? (),
    account_id                varchar(30) not null
        constraint fkq1ot1uk7yfqm56nes5e6exblp
            references public.account
);

alter table public.account_transaction_override
    owner to d101;

create table public.budget_month
(
    id                              integer                                        not null
        primary key,
    created_at                      timestamp(6)                                   not null,
    is_deleted                      boolean      default false                     not null,
    updated_at                      timestamp(6)                                   not null,
    base_date                       date                                           not null,
    memo                            varchar(255),
    status                          varchar(255) default 'OPEN'::character varying not null
        constraint budget_month_status_check
            check ((status)::text = ANY
                   ((ARRAY ['OPEN'::character varying, 'CLOSED'::character varying, 'LOCKED'::character varying])::text[])),
    total_fixed_essential_amount    bigint       default 0                         not null,
    total_income_amount             bigint       default 0                         not null,
    total_variable_essential_amount bigint       default 0                         not null,
    users_id                        integer                                        not null
        constraint fknbnp7vfhr6u49g22664uav7x4
            references ??? ()
);

alter table public.budget_month
    owner to d101;

create table public.budget_week
(
    id                       integer                                           not null
        primary key,
    created_at               timestamp(6)                                      not null,
    is_deleted               boolean      default false                        not null,
    updated_at               timestamp(6)                                      not null,
    actual_spent_amount      bigint       default 0                            not null,
    final_free_amount        bigint       default 0                            not null,
    manual_adjustment_amount bigint       default 0                            not null,
    is_partial               boolean      default false                        not null,
    period_days              integer      default 7                            not null,
    planned_essential_amount bigint       default 0                            not null,
    planned_income_amount    bigint       default 0                            not null,
    planned_saving_amount    bigint       default 0                            not null,
    rollover_in_amount       bigint       default 0                            not null,
    status                   varchar(255) default 'PLANNED'::character varying not null
        constraint budget_week_status_check
            check ((status)::text = ANY
                   ((ARRAY ['PLANNED'::character varying, 'ACTIVE'::character varying, 'CLOSED'::character varying, 'ADJUSTED'::character varying])::text[])),
    week_end_date            date                                              not null,
    week_sequence            integer                                           not null,
    week_start_date          date                                              not null,
    users_id                 integer                                           not null
        constraint fkactgxtesxwy1kkm7wvytgmh0y
            references ??? ()
);

alter table public.budget_week
    owner to d101;

create table public.budget_rollover
(
    id                    integer                                           not null
        primary key,
    created_at            timestamp(6)                                      not null,
    is_deleted            boolean      default false                        not null,
    updated_at            timestamp(6)                                      not null,
    note                  varchar(255),
    resolved_at           timestamp(6),
    rollover_amount       bigint                                            not null,
    rollover_step         integer                                           not null,
    status                varchar(255) default 'APPLIED'::character varying not null
        constraint budget_rollover_status_check
            check ((status)::text = ANY
                   ((ARRAY ['PLANNED'::character varying, 'APPLIED'::character varying, 'EXHAUSTED'::character varying, 'PLAN_DEDUCTED'::character varying])::text[])),
    source_budget_week_id integer                                           not null
        constraint fkqkxyju874a71mdpuyg0fj8s3w
            references public.budget_week,
    target_budget_week_id integer                                           not null
        constraint fkmwbdtlb48gj1m7hh3t3vex10y
            references public.budget_week
);

alter table public.budget_rollover
    owner to d101;

create table public.category
(
    id   integer     not null
        primary key,
    name varchar(20) not null
);

alter table public.category
    owner to d101;

create table public.essential_expense_month
(
    id              integer                                            not null
        primary key,
    created_at      timestamp(6)                                       not null,
    is_deleted      boolean      default false                         not null,
    updated_at      timestamp(6)                                       not null,
    amount          bigint                                             not null,
    expense_scope   varchar(20)                                        not null,
    source_provider varchar(255) default 'ANALYZER'::character varying not null
        constraint essential_expense_month_source_provider_check
            check ((source_provider)::text = ANY
                   ((ARRAY ['ANALYZER'::character varying, 'USER_INPUT'::character varying])::text[])),
    budget_month_id integer                                            not null
        constraint fk9ly5tur56xgsccskfkl1i0elx
            references public.budget_month,
    category_id     integer                                            not null
        constraint fke3l94clib9409449vuhsbjw82
            references public.category
);

alter table public.essential_expense_month
    owner to d101;

create table public.essential_week_allocation
(
    essential_week_allocation_id integer               not null
        primary key,
    created_at                   timestamp(6)          not null,
    is_deleted                   boolean default false not null,
    updated_at                   timestamp(6)          not null,
    actual_spent_amount          bigint  default 0     not null,
    expense_scope                varchar(255)          not null
        constraint essential_week_allocation_expense_scope_check
            check ((expense_scope)::text = ANY
                   ((ARRAY ['FIXED'::character varying, 'VARIABLE'::character varying])::text[])),
    note                         varchar(255),
    planned_amount               bigint  default 0     not null,
    budget_week_id               integer               not null
        constraint fk8n512jxrji9eseshk4ilneiau
            references public.budget_week,
    category_id                  integer               not null
        constraint fkjl9ydqlp1a3qa7vvwqalocu6j
            references public.category,
    essential_expense_month_id   integer               not null
        constraint fkdjk810ju6w0oj9o1g350va499
            references public.essential_expense_month
);

alter table public.essential_week_allocation
    owner to d101;

create table public.merchant
(
    id   integer      not null
        primary key,
    name varchar(100) not null
);

alter table public.merchant
    owner to d101;

create table public.card_transaction_override
(
    bill_statements_status varchar(20) default '미결제'::character varying not null,
    bill_statements_yn     boolean                                      not null,
    card_status            boolean                                      not null,
    transaction_unique_no  bigint                                       not null,
    id                     integer                                      not null
        primary key
        constraint fkfb85k4794o41u4hyp886l7pcr
            references ??? (),
    card_unique_no         bigint                                       not null
        constraint fk3u1vu6pi1usclfgt8j8jxkv2e
            references ??? (),
    merchant_id            integer                                      not null
        constraint fkjowlmjuad0si8mhogf756dp3q
            references public.merchant
);

alter table public.card_transaction_override
    owner to d101;

create table public.plan
(
    id                    integer                                          not null
        primary key,
    created_at            timestamp(6)                                     not null,
    is_deleted            boolean      default false                       not null,
    updated_at            timestamp(6)                                     not null,
    auto_allocate_enabled boolean      default true                        not null,
    description           varchar(255),
    end_date              date                                             not null,
    plan_url              varchar(255),
    priority              integer                                          not null,
    saved_amount          bigint       default 0                           not null,
    start_date            date                                             not null,
    status                varchar(255) default 'ACTIVE'::character varying not null
        constraint plan_status_check
            check ((status)::text = ANY
                   ((ARRAY ['ACTIVE'::character varying, 'PAUSED'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying])::text[])),
    target_amount         bigint                                           not null,
    target_weeks          integer                                          not null,
    title                 varchar(100)                                     not null,
    users_id              integer                                          not null
        constraint fkp5uhpm64kucpxnvm3jde0l0ud
            references ??? (),
    plan_img_url          varchar(255),
    plan_video_url        varchar(255)
);

alter table public.plan
    owner to d101;

create table public.budget_adjustment
(
    id                integer               not null
        primary key,
    created_at        timestamp(6)          not null,
    is_deleted        boolean default false not null,
    updated_at        timestamp(6)          not null,
    adjustment_amount bigint  default 0     not null,
    adjustment_type   varchar(255)          not null
        constraint budget_adjustment_adjustment_type_check
            check ((adjustment_type)::text = ANY
                   ((ARRAY ['FREE_BUDGET_ADD'::character varying, 'FREE_BUDGET_SUBTRACT'::character varying, 'PLAN_ADD'::character varying, 'PLAN_SUBTRACT'::character varying, 'PLAN_DELAY'::character varying, 'PLAN_EARLY_CLOSE'::character varying, 'ROLLOVER_APPLIED'::character varying])::text[])),
    reason            varchar(255),
    budget_week_id    integer               not null
        constraint fk3xgb3q00xigbhh59s2nflhw6j
            references public.budget_week,
    plan_id           integer
        constraint fkb6b05ybhoxyhjpf9d9c9r4cjx
            references public.plan
);

alter table public.budget_adjustment
    owner to d101;

create table public.plan_week_allocation
(
    id              integer                                       not null
        primary key,
    created_at      timestamp(6)                                  not null,
    is_deleted      boolean     default false                     not null,
    updated_at      timestamp(6)                                  not null,
    allocation_type varchar(30) default 'AUTO'::character varying not null,
    final_amount    bigint      default 0                         not null,
    note            varchar(255),
    planned_amount  bigint      default 0                         not null,
    budget_week_id  integer                                       not null
        constraint fkm9jsyp8tcpcopqkaqa4ewvm4o
            references public.budget_week,
    plan_id         integer                                       not null
        constraint fke1i3d9q9dh086h5t8lt2bmvgs
            references public.plan
);

alter table public.plan_week_allocation
    owner to d101;

create table public.pocket
(
    id               varchar(50)           not null
        primary key,
    created_at       timestamp(6)          not null,
    is_deleted       boolean default false not null,
    updated_at       timestamp(6)          not null,
    allocated_amount bigint                not null,
    name             varchar(30)           not null,
    account_id       varchar(30)           not null
        constraint fk8ohvcor8ep8qeeharcrpvi9ok
            references public.account,
    budget_week_id   integer               not null
        constraint fkscqp57yul06wqdebacm5phwy
            references public.budget_week,
    category_id      integer               not null
        constraint fkss2lw6trsmnq8dyul963olel8
            references public.category
);

alter table public.pocket
    owner to d101;

create table public.policy
(
    id             integer               not null
        primary key,
    created_at     timestamp(6)          not null,
    is_deleted     boolean default false not null,
    updated_at     timestamp(6)          not null,
    agreement_type varchar(255)          not null
        constraint policy_agreement_type_check
            check ((agreement_type)::text = ANY
                   ((ARRAY ['TERMS'::character varying, 'PRIVACY'::character varying, 'MARKETING'::character varying])::text[])),
    content        text                  not null,
    required       boolean               not null,
    version        integer               not null
);

alter table public.policy
    owner to d101;

create table public.setting
(
    id         integer               not null
        primary key,
    created_at timestamp(6)          not null,
    is_deleted boolean default false not null,
    updated_at timestamp(6)          not null,
    alarm      boolean default false not null,
    users_id   integer               not null
        constraint uksjgwlqd6bpibeb8dm54m8lmis
            unique
        constraint fkj0uyxhec59fajg73gx64tchjx
            references ??? ()
);

alter table public.setting
    owner to d101;

create table public.user_agreements
(
    id         integer               not null
        primary key,
    created_at timestamp(6)          not null,
    is_deleted boolean default false not null,
    updated_at timestamp(6)          not null,
    agreed     boolean               not null,
    agreed_at  timestamp(6),
    policy_id  integer               not null
        constraint fkbng08h3t9ascf0tyj7t1gbl1e
            references public.policy,
    users_id   integer               not null
        constraint fklo1umwuxjf5y5tp2mv4k8nfu5
            references ??? ()
);

alter table public.user_agreements
    owner to d101;

create table public.essential_expense_snapshot_week
(
    id                        integer generated by default as identity
        primary key,
    created_at                timestamp(6)          not null,
    is_deleted                boolean default false not null,
    updated_at                timestamp(6)          not null,
    analyzed_at               timestamp(6)          not null,
    base_months               integer               not null,
    essential_amount          bigint  default 0     not null,
    fixed_essential_amount    bigint  default 0     not null,
    variable_essential_amount bigint  default 0     not null,
    week_end_date             date                  not null,
    week_start_date           date                  not null,
    year_week                 varchar(10)           not null,
    users_id                  integer               not null
        constraint fkkn77i6n2bwf1evb64bfpyne0n
            references ??? ()
);

alter table public.essential_expense_snapshot_week
    owner to d101;

create table public.essential_expense_snapshot_week_category
(
    id               integer generated by default as identity
        primary key,
    created_at       timestamp(6)          not null,
    is_deleted       boolean default false not null,
    updated_at       timestamp(6)          not null,
    amount           bigint  default 0     not null,
    expense_scope    varchar(20)           not null
        constraint essential_expense_snapshot_week_category_expense_scope_check
            check ((expense_scope)::text = ANY
                   ((ARRAY ['FIXED'::character varying, 'VARIABLE'::character varying])::text[])),
    category_id      integer               not null
        constraint fkl6ui3qf78eg6w0easvba224xt
            references public.category,
    snapshot_week_id integer               not null
        constraint fkscit8lioy831w6fl86ay64o17
            references public.essential_expense_snapshot_week
);

alter table public.essential_expense_snapshot_week_category
    owner to d101;

create table public.transaction_analysis_job
(
    id               varchar(80)                                      not null
        primary key,
    created_at       timestamp(6)                                     not null,
    is_deleted       boolean     default false                        not null,
    updated_at       timestamp(6)                                     not null,
    current_step     varchar(30) default 'SYNCING'::character varying not null
        constraint transaction_analysis_job_current_step_check
            check ((current_step)::text = ANY
                   ((ARRAY ['SYNCING'::character varying, 'PREPROCESSING'::character varying, 'CATEGORY_LABELING'::character varying, 'CLASSIFYING_ESSENTIAL'::character varying, 'BUILDING_SNAPSHOTS'::character varying, 'COMPLETED'::character varying])::text[])),
    error_code       varchar(50),
    error_message    varchar(255),
    finished_at      timestamp(6),
    requested_at     timestamp(6)                                     not null,
    requested_months integer                                          not null,
    started_at       timestamp(6),
    status           varchar(20) default 'PENDING'::character varying not null
        constraint transaction_analysis_job_status_check
            check ((status)::text = ANY
                   ((ARRAY ['PENDING'::character varying, 'RUNNING'::character varying, 'SUCCEEDED'::character varying, 'FAILED'::character varying])::text[])),
    users_id         integer                                          not null
        constraint fkhr555sebj308a3aa0b9gvncwe
            references ??? ()
);

alter table public.transaction_analysis_job
    owner to d101;

create table public.transaction_analysis_result
(
    id                      integer generated by default as identity
        primary key,
    created_at              timestamp(6)          not null,
    is_deleted              boolean default false not null,
    updated_at              timestamp(6)          not null,
    analysis_included       boolean default true  not null,
    analyzed_at             timestamp(6)          not null,
    confidence              double precision,
    excluded_reason         varchar(30)
        constraint transaction_analysis_result_excluded_reason_check
            check ((excluded_reason)::text = ANY
                   ((ARRAY ['INCOME'::character varying, 'INTERNAL_TRANSFER'::character varying, 'NON_ESSENTIAL'::character varying, 'MANUAL_EXCLUDE'::character varying])::text[])),
    expense_scope           varchar(20)
        constraint transaction_analysis_result_expense_scope_check
            check ((expense_scope)::text = ANY
                   ((ARRAY ['FIXED'::character varying, 'VARIABLE'::character varying])::text[])),
    fallback_applied        boolean default false not null,
    final_label             varchar(100),
    raw_label               varchar(100),
    threshold_used          double precision,
    category_id             integer               not null
        constraint fkg3e7v6c8aogtkqvloutwmgr1k
            references public.category,
    transaction_override_id integer               not null
        constraint fkkbisusbf79lt48mi0rivbpw8u
            references ??? ()
);

alter table public.transaction_analysis_result
    owner to d101;

create table public.account_verification
(
    id                            bigint generated by default as identity
        primary key,
    created_at                    timestamp(6)                                       not null,
    is_deleted                    boolean     default false                          not null,
    updated_at                    timestamp(6)                                       not null,
    auth_text                     varchar(20)                                        not null,
    failure_reason                varchar(255),
    request_transaction_unique_no bigint                                             not null,
    requested_at                  timestamp(6)                                       not null,
    status                        varchar(20) default 'REQUESTED'::character varying not null
        constraint account_verification_status_check
            check ((status)::text = ANY
                   ((ARRAY ['REQUESTED'::character varying, 'SUCCESS'::character varying, 'FAILED'::character varying, 'EXPIRED'::character varying])::text[])),
    verified_at                   timestamp(6),
    account_id                    varchar(30)                                        not null
        constraint fkm9rsmtt69xakdxdnj0gta0l5a
            references public.account
);

alter table public.account_verification
    owner to d101;

create table public.users
(
    birth_date       date,
    has_fixed_income boolean,
    id               integer generated by default as identity
        primary key,
    is_deleted       boolean     default false                       not null,
    created_at       timestamp(6)                                    not null,
    updated_at       timestamp(6)                                    not null,
    status           varchar(20) default 'ACTIVE'::character varying not null
        constraint users_status_check
            check ((status)::text = ANY ((ARRAY ['ACTIVE'::character varying, 'TEMP'::character varying])::text[])),
    nickname         varchar(50),
    user_key         varchar(60),
    email            varchar(255),
    profile_url      varchar(255)
);

alter table public.users
    owner to d101;

create table public.card
(
    cvc                   varchar(3)            not null,
    is_deleted            boolean default false not null,
    baseline_performance  bigint                not null,
    card_expiry_date      timestamp(6)          not null,
    card_unique_no        bigint                not null
        primary key,
    created_at            timestamp(6)          not null,
    max_benefit_limit     bigint                not null,
    updated_at            timestamp(6)          not null,
    issuer_code           varchar(10)           not null,
    withdrawal_date       varchar(10)           not null,
    card_issuer_name      varchar(20)           not null,
    card_no               varchar(20)           not null,
    name                  varchar(20)           not null,
    withdrawal_account_no varchar(30)           not null
        constraint fkfk3pbv830fcuk7xhiktfidmlf
            references ??? (),
    card_description      varchar(255)          not null
);

alter table public.card
    owner to d101;

create table public.transaction_override
(
    id                    integer               not null
        primary key,
    created_at            timestamp(6)          not null,
    is_deleted            boolean default false not null,
    updated_at            timestamp(6)          not null,
    transaction_balance   bigint                not null,
    transaction_date_time timestamp(6)          not null,
    transaction_memo      text                  not null,
    category_id           integer               not null
        constraint fkginndbfdcjal8ie1lgf1np6rw
            references ??? ()
);

alter table public.transaction_override
    owner to d101;

create table public.social_account
(
    id           integer generated by default as identity
        primary key,
    is_deleted   boolean default false not null,
    users_id     integer               not null
        unique
        constraint fk6j1bdl826gi0kcyl85vx11u7f
            references ??? (),
    created_at   timestamp(6)          not null,
    updated_at   timestamp(6)          not null,
    provider     varchar(20)           not null
        constraint social_account_provider_check
            check ((provider)::text = ANY
                   ((ARRAY ['GOOGLE'::character varying, 'KAKAO'::character varying, 'NAVER'::character varying, 'TEST'::character varying])::text[])),
    provider_uid varchar(255)          not null,
    unique (provider, provider_uid)
);

alter table public.social_account
    owner to d101;

