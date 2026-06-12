## Objective

Extract source-backed backend planning evidence for "Instruction and user story evidence".

## Repository Evidence

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
  - authentication, user profile, document upload, API contract, PostgreSQL, validation, testing, delivery roadmap, epic/user story alignment
- Instruction excerpts:
- - REST API endpoints
- - Input validation
- - Validation rules
- - JWT authentication integration
- - PostgreSQL database
- - JWT authentication and authorization
- - RESTful API design
- - Use PostgreSQL
- Relevant user stories:
- EPIC 1: Authentication & Profile
- EPIC 2: Content Management (Learning)
- EPIC 3: Assessment
- EPIC 4: Gamification
- EPIC 5: VSL Dictionary
- EPIC 6: Monetization (Freemium & Payment)
- EPIC 7: Admin Panel
- Epic 1: Authentication | Epic 2: Learning | Epic 3: Assessment
- EPIC 1: Authentication & Profile
- US-01 â€” ÄÄƒng kÃ½ tÃ i khoáº£n
- AC3: Given password < 8 characters or missing uppercase/number â†’ inline validation error shown before submission.
- US-02 â€” ÄÄƒng nháº­p
- US-03 â€” ÄÄƒng nháº­p Google OAuth
- US-04 â€” Xem Profile
- Story: As a User, I want to view my profile page, so that I can see my account info, streak, and XP.
- AC1: Profile page displays: full name, avatar, account type (Basic/Premium), total XP, current streak, longest streak.
- Assigned task details:
  - Extract domain constraints, user stories, frontend verification gaps, and planning assumptions.
- Expected output contract:
- List the instruction constraints that shape backend implementation.
- Map visible epics and user stories to backend work areas.
- Identify frontend API dependencies and mismatches.
- Domain-specific checklist:
- epic/user story alignment
- authentication
- user profile
- API contract

## Findings

- The instruction requires a backend epic/API alignment plan, not an orchestrator architecture plan.
- The planning brief includes knowledge-base guidance, instruction excerpts, user stories, and frontend verification excerpts.
- Backend outputs must preserve epic/user story alignment and turn frontend gaps into API contract requirements.
- Core anchors for this domain include authentication, user profile, signature workflow, document upload, API contract, PostgreSQL, validation, testing, and delivery roadmap.

## Recommended Inputs

- Use user-story excerpts as the story coverage source.
- Use frontend verification excerpts as the integration dependency source.
- Use backend-planning and fullstack-planning knowledge as the planning checklist.
- Keep repo summaries as supporting context only.
