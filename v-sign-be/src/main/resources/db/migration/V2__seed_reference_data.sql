insert into reference_roles (code, description)
select 'USER', 'Regular learner account'
where not exists (
    select 1 from reference_roles where code = 'USER'
);

insert into reference_roles (code, description)
select 'ADMIN', 'Content and platform administrator'
where not exists (
    select 1 from reference_roles where code = 'ADMIN'
);

insert into reference_roles (code, description)
select 'SUPER_ADMIN', 'System-wide administrator'
where not exists (
    select 1 from reference_roles where code = 'SUPER_ADMIN'
);
