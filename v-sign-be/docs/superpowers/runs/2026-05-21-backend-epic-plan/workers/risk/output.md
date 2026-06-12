## Objective

Review domain risks and validation gates for "Domain risk and validation review".

## Risks

- Generic planning risk: worker output can drift back to orchestrator process notes instead of backend epic/API decisions.
- API contract risk: frontend authentication, user profile, signature workflow, and document upload screens need stable request/response fields before integration.
- Persistence risk: PostgreSQL and Flyway constraints must match service validation or runtime errors will bypass standard error responses.
- Security risk: missing JWT role checks can expose protected profile, upload, or signature APIs.
- Original instruction summary:
  - # Backend Epic Implementation Plan Request (V-SIGN) Reference baseline (must follow): - EXE101_Project_V-Sign_BE/BE-init.md Primary objective: - Produce a deterministic backend implementation plan per epic for V-SIGN with Java Spring Boot 3.x architecture, JWT security, Flyway migrations, and centralized exception handling as specified in BE-init.md. Planning context documents (must be used): - EXE101_Project_V-Sign_
- Selected knowledge base:
- writing-plan-principles.md: # Writing Plan Principles - Start by clarifying the user outcome, scope boundary, and definition of done. - Prefer explicit assumptions over hidden assumptions. - Organize plans into concrete phases that can be valida...
  - # Writing Plan Principles
  - - Start by clarifying the user outcome, scope boundary, and definition of done.
  - - Prefer explicit assumptions over hidden assumptions.
  - - Organize plans into concrete phases that can be validated independently.
- task-decomposition-principles.md: # Task Decomposition Principles - Break the planning problem into a small graph of worker tasks with clear ownership. - Separate evidence gathering, architecture, risk review, and final synthesis. - Pass only the cont...
  - # Task Decomposition Principles
  - - Break the planning problem into a small graph of worker tasks with clear ownership.
  - - Separate evidence gathering, architecture, risk review, and final synthesis.
  - - Pass only the context each worker needs to reduce drift.
- risk-audit-checklist.md: # Risk Audit Checklist - Check for missing assumptions, context overload, and ambiguous ownership. - Check for optional inputs becoming accidental prerequisites. - Check whether the merge output can be audited without...
  - # Risk Audit Checklist
  - - Check for missing assumptions, context overload, and ambiguous ownership.
  - - Check for optional inputs becoming accidental prerequisites.
- Domain anchors:

## Validation Plan

- Require domain anchors in each worker output and in the final report.
- Verify planning brief references instruction excerpts, relevant user stories, frontend verification excerpts, and selected knowledge-base guidance.
- Verify task graph includes API contract, frontend alignment, delivery roadmap, and risk tasks for backend epic plans.
- Test authentication, user profile, signature workflow, document upload, PostgreSQL migrations, validation errors, and delivery roadmap traceability in the generated report.

## Mitigations

- Add a generic-output detector that marks `needs_revision` when required domain anchors are absent.
- Keep worker context narrow by passing excerpts, summaries, task contracts, and domain checklists instead of raw repo dumps.
- Preserve traceability from instruction/user stories to planning brief, task graph, worker outputs, final report, and audit.
