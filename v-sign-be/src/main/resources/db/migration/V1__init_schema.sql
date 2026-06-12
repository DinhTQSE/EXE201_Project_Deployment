create table users (
    id uuid primary key,
    email varchar(255) not null unique,
    password_hash varchar(255),
    full_name varchar(120) not null,
    avatar_url text,
    account_type varchar(20) not null default 'BASIC',
    role varchar(20) not null default 'USER',
    is_active boolean not null default true,
    current_streak int not null default 0,
    longest_streak int not null default 0,
    total_xp int not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table reference_roles (
    code varchar(20) primary key,
    description varchar(120) not null
);
