create table subscription_plans (
    plan_id varchar(80) primary key,
    plan_type varchar(20),
    name varchar(160) not null,
    amount int not null,
    price int not null,
    currency varchar(10) not null,
    duration_days int not null,
    active boolean not null default true,
    legacy_visible boolean not null default false,
    display_order int not null
);

create table checkout_intents (
    checkout_id varchar(120) primary key,
    plan_id varchar(80) not null,
    user_id varchar(120) not null,
    status varchar(30) not null,
    checkout_url text not null,
    success_url text,
    cancel_url text,
    created_at timestamp with time zone not null default now()
);

create table user_subscriptions (
    email varchar(255) primary key,
    plan_type varchar(20),
    status varchar(30) not null,
    started_at timestamp with time zone,
    expires_at timestamp with time zone
);

create table payment_orders (
    transaction_id varchar(120) primary key,
    provider_transaction_id varchar(160) not null,
    provider varchar(30) not null,
    plan_id varchar(80) not null,
    plan_type varchar(20),
    amount int not null,
    currency varchar(10) not null,
    status varchar(30) not null,
    qr_code_data text not null,
    deep_link text not null,
    expires_at timestamp with time zone not null,
    qr_code_url text not null,
    expires_in_seconds int not null,
    retryable boolean not null default true,
    user_email varchar(255),
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    manual_reason text
);

insert into subscription_plans
(plan_id, plan_type, name, amount, price, currency, duration_days, active, legacy_visible, display_order)
values
('free', null, 'Free', 0, 0, 'VND', 0, true, true, 0),
('pro-monthly', 'MONTHLY', 'Premium Monthly', 49000, 49000, 'VND', 30, true, true, 1),
('pro-yearly', 'YEARLY', 'Premium Yearly', 399000, 399000, 'VND', 365, true, true, 2),
('school', null, 'School Plan', 1990000, 1990000, 'VND', 365, true, false, 3);

insert into user_subscriptions (email, plan_type, status, started_at, expires_at) values
('premium@vsign.vn', 'MONTHLY', 'ACTIVE', '2026-05-21T00:00:00+07:00', '2026-06-20T00:00:00+07:00');

insert into payment_orders
(transaction_id, provider_transaction_id, provider, plan_id, plan_type, amount, currency, status, qr_code_data, deep_link, expires_at, qr_code_url, expires_in_seconds, retryable, user_email, created_at, updated_at)
values
('txn-1001', 'MOMO-seed-1001', 'MOMO', 'pro-monthly', 'MONTHLY', 49000, 'VND', 'PENDING', 'VSIGN|MOMO|MONTHLY|txn-1001|49000', 'momo://payment/txn-1001', '2026-05-10T08:05:00+07:00', 'https://pay.vsign.test/qr/txn-1001', 300, true, 'learner.one@vsign.test', '2026-05-10T08:00:00+07:00', '2026-05-10T08:00:00+07:00'),
('txn-1002', 'ZALOPAY-seed-1002', 'ZALOPAY', 'pro-yearly', 'YEARLY', 199000, 'VND', 'PAID', 'VSIGN|ZALOPAY|YEARLY|txn-1002|199000', 'zalopay://payment/txn-1002', '2026-05-11T08:05:00+07:00', 'https://pay.vsign.test/qr/txn-1002', 300, false, 'learner.two@vsign.test', '2026-05-11T08:00:00+07:00', '2026-05-11T08:03:00+07:00');
