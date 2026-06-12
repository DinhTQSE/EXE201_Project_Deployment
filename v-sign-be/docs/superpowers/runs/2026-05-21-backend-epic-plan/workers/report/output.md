## Objective

Assemble a backend epic/API alignment plan for "Final backend epic plan assembly".

## Summary

- The final plan must align instruction requirements and user stories to backend epics, API contract decisions, PostgreSQL persistence, validation, testing, and a delivery roadmap.
- Authentication and user profile are foundational because protected workflows depend on JWT identity, account status, and role claims.
- Signature workflow and document upload require explicit API contracts, request/response DTOs, validation rules, and frontend contract tests.
- Frontend verification gaps become backend deliverables only when they require backend API support or response-field changes.

## Decisions

- Use `/api/v1` REST endpoints with standard success and error envelopes.
- Use PostgreSQL with Flyway migrations and production `ddl-auto=validate` behavior.
- Use centralized validation and exception handling so frontend errors are consistent.
- Build the delivery roadmap in dependency order: foundation, authentication/user profile, document upload/signature workflow, API contract integration, testing, and later admin/reporting extensions.

## Next Actions

- Turn each epic into endpoint-level tasks with DTOs, validation codes, database migrations, service rules, and tests.
- Run the frontend/backend contract smoke suite against authentication, user profile, signature workflow, and document upload.
- Keep audit status `pass`; treat `needs_revision` as a failed planning run requiring regenerated worker outputs.
