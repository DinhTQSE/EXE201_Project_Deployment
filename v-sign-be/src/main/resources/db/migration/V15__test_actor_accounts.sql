-- Production migration intentionally does not seed shared test accounts.
-- Test-only fixtures live under src/test/resources/db/testdata.

insert into reference_roles (code, description)
select 'CONTENT_REVIEWER', 'Content moderation and review staff'
where not exists (
    select 1 from reference_roles where code = 'CONTENT_REVIEWER'
);
