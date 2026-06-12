# Planning Brief

## Planning Intent

# Backend Epic Implementation Plan Request (V-SIGN)

Reference baseline (must follow):
- EXE101_Project_V-Sign_BE/BE-init.md

Primary objective:
- Produce a deterministic backend implementation plan per epic for V-SIGN with Java Spring Boot 3.x architecture, JWT security, Flyway migrations, and centralized exception handling as specified in BE-init.md.

Planning context documents (must be used):
- EXE101_Project_V-Sign_BE/docs/superpowers/working/2026-05-21-vsign-userstories-from-docx.md
- docs/US_Frontend_Verification_Report_v2.md

Mandatory planning requirements:
1. Derive backend epics from user stories and map story-to-epic coverage.
2. Define API contracts per epic (endpoint, method, auth role, request DTO, response DTO, validation/error codes).
3. Verify backend API expectations against frontend verification report findings; call out mismatches, missing fields, and contract changes required for FE alignment.
4. Include database and migration implications per epic.
5. Sequence implementation by dependencies and risk.
6. Include test strategy gates per epic (unit/integration/security/contract).
7. Explicitly list assumptions when project details are missing.

Deliverable orientation:
- planning-brief.md should be directly actionable for implementation planning.
- task-graph.json should support incremental delivery with parallelizable tasks when safe.

## Context Mode

- Mode: fullstack
- Repo Scan Enabled: yes
- Explicit Docs: 3
- Explicit Repos: 2
- Domain Anchors: authentication, user profile, document upload, API contract, PostgreSQL, validation, testing, delivery roadmap, epic/user story alignment

## Knowledge Base Inputs

- writing-plan-principles.md: # Writing Plan Principles - Start by clarifying the user outcome, scope boundary, and definition of done. - Prefer explicit assumptions over hidden assumptions. - Organize plans into concrete phases that can be valida...
- task-decomposition-principles.md: # Task Decomposition Principles - Break the planning problem into a small graph of worker tasks with clear ownership. - Separate evidence gathering, architecture, risk review, and final synthesis. - Pass only the cont...
- risk-audit-checklist.md: # Risk Audit Checklist - Check for missing assumptions, context overload, and ambiguous ownership. - Check for optional inputs becoming accidental prerequisites. - Check whether the merge output can be audited without...
- verification-checklist.md: # Verification Checklist - Verify that generated artifacts exist and are internally consistent. - Verify that worker outputs include their required sections. - Verify that the final plan includes a main-agent audit an...
- backend-planning-guide.md: # Backend Planning Guide - Capture service boundaries, APIs, persistence, validation, and deployment constraints. - Highlight framework/runtime assumptions only when they are explicitly known. - Include integration ri...
- frontend-planning-guide.md: # Frontend Planning Guide - Capture UX flows, component boundaries, state ownership, and testing surfaces. - Distinguish between product requirements and implementation details. - Prefer plans that are incremental and...
- fullstack-planning-guide.md: # Fullstack Planning Guide - Align frontend, backend, and integration work around a single delivery sequence. - Make interface contracts explicit before implementation tasks are assigned. - Reserve cross-cutting valid...
- architecture-planning-guide.md: # Architecture Planning Guide - Identify the runtime boundaries, context boundaries, and ownership boundaries. - Prefer extension of native project systems over parallel frameworks. - Record tradeoffs so the final pla...
- implementation-roadmap-guide.md: # Implementation Roadmap Guide - Sequence work into a thin vertical slice first, then follow with expansion tasks. - Pair each implementation phase with validation criteria. - Keep roadmap steps small enough for agent...

## Knowledge Base Excerpts

### writing-plan-principles.md
# Writing Plan Principles

- Start by clarifying the user outcome, scope boundary, and definition of done.
- Prefer explicit assumptions over hidden assumptions.
- Organize plans into concrete phases that can be validated independently.
- Keep outputs deterministic, reviewable, and concise enough for downstream agents to consume.

### task-decomposition-principles.md
# Task Decomposition Principles

- Break the planning problem into a small graph of worker tasks with clear ownership.
- Separate evidence gathering, architecture, risk review, and final synthesis.
- Pass only the context each worker needs to reduce drift.
- Preserve a stable merge order for deterministic final output.

### risk-audit-checklist.md
# Risk Audit Checklist

- Check for missing assumptions, context overload, and ambiguous ownership.
- Check for optional inputs becoming accidental prerequisites.
- Check whether the merge output can be audited without reading every worker artifact.

### verification-checklist.md
# Verification Checklist

- Verify that generated artifacts exist and are internally consistent.
- Verify that worker outputs include their required sections.
- Verify that the final plan includes a main-agent audit and references the planning brief.

### backend-planning-guide.md
# Backend Planning Guide

- Capture service boundaries, APIs, persistence, validation, and deployment constraints.
- Highlight framework/runtime assumptions only when they are explicitly known.
- Include integration risks, data contracts, and verification gates.

### frontend-planning-guide.md
# Frontend Planning Guide

- Capture UX flows, component boundaries, state ownership, and testing surfaces.
- Distinguish between product requirements and implementation details.
- Prefer plans that are incremental and independently reviewable.

### fullstack-planning-guide.md
# Fullstack Planning Guide

- Align frontend, backend, and integration work around a single delivery sequence.
- Make interface contracts explicit before implementation tasks are assigned.
- Reserve cross-cutting validation for the main-agent audit pass.

### architecture-planning-guide.md
# Architecture Planning Guide

- Identify the runtime boundaries, context boundaries, and ownership boundaries.
- Prefer extension of native project systems over parallel frameworks.
- Record tradeoffs so the final plan is traceable.

### implementation-roadmap-guide.md
# Implementation Roadmap Guide

- Sequence work into a thin vertical slice first, then follow with expansion tasks.
- Pair each implementation phase with validation criteria.
- Keep roadmap steps small enough for agent execution or human review.

## Instruction Excerpts

- - REST API endpoints
- - Input validation
- - Validation rules
- - JWT authentication integration
- - PostgreSQL database
- - JWT authentication and authorization
- - RESTful API design
- - Use PostgreSQL
- - Explain how the Spring Boot application connects to PostgreSQL
- Authentication requirements:
- - Implement JWT-based authentication
- - Include login API

## Relevant User Stories

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
- AC3: Earned badges are displayed in a grid on the profile page.
- US-05 â€” Chá»‰nh sá»­a Profile
- AC2: Given user uploads a new avatar (JPG/PNG â‰¤ 2MB) â†’ image saved to S3, displayed on profile.
- US-06 â€” Äá»•i máº­t kháº©u
- AC4: Given new password < 8 characters â†’ validation error shown.
- US-07 â€” ÄÄƒng xuáº¥t
- US-08 â€” QuÃªn máº­t kháº©u
- EPIC 2: Content Management (Learning)


## Planning Directives

- Use the knowledge base to shape the plan before decomposing worker tasks.
- Preserve instruction, user story, and frontend verification traceability in every downstream output.
- Prefer concrete backend API contracts, validation rules, persistence impacts, and test gates over generic process notes.
- Keep optional docs and repos as supporting inputs rather than prerequisites.
- Produce a deterministic worker graph with explicit ownership and merge order.

## Brief Summary

This planning brief is built from 9 knowledge-base documents: writing-plan-principles.md, task-decomposition-principles.md, risk-audit-checklist.md, verification-checklist.md, backend-planning-guide.md, frontend-planning-guide.md, fullstack-planning-guide.md, architecture-planning-guide.md, implementation-roadmap-guide.md.
