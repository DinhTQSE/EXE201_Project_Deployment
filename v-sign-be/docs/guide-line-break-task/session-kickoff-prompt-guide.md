# Session Kickoff Prompt Guide

## Purpose
Use this guide to start any new Codex session with full context even when the context window is not shared across sessions.

## Standard Kickoff Prompt (Copy/Paste)

```md
Session kickoff: Session <1|2|3>

Goal:
Implement only assigned epics for this session, with checkpoint-first control.

Mandatory first step:
Read and report these paths before any code change:
1) Instruction path
2) Global source-of-truth paths
3) Session checkpoint paths
4) Current checkpoint status
5) STOP/GO decision (must wait for my approval if pending)

Paths:
- Instruction:
  C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_BE\docs\backend-implementation-checkpoints\PLAN-backend-epic-implementation.md
- Source of truth:
  C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_BE\docs\backend-implementation-checkpoints\00-implementation-summary.md
  C:\Users\KHAI\Documents\Exe201\source-code\EXE101_Project_V-Sign_BE\docs\superpowers\runs\2026-05-21-backend-epic-plan\final-report.md

Session scope:
- Session 1: Epics 01-03
- Session 2: Epics 04-05
- Session 3: Epics 06-07

Rules:
- Do not edit outside session epics.
- If checkpoint is pending approval, stop after review notes.
- Before coding, provide: scope, API rows, files, tests, risks.
- After coding, provide: changed files, endpoint status, test evidence, checkpoint updates.

Verification command:
<put session command here>

Output format required:
- Section A: Path report + status
- Section B: Implementation plan for this session
- Section C: Waiting for reviewer approval
```

## Session Test Commands

- Session 1:

```powershell
mvn -q "-Dtest=AuthControllerIT,ProfileControllerIT,DictionaryIT" test
```

- Session 2:

```powershell
mvn -q "-Dtest=com.vsign.backend.learning.LearningWorkflowIT,com.vsign.backend.assessment.AssessmentControllerIT" test
```

- Session 3:

```powershell
mvn -q "-Dtest=com.vsign.backend.gamification.GamificationControllerIT,com.vsign.backend.monetization.SubscriptionControllerIT" test
```

## Review Rule
- No backend code edits until checkpoint approval is explicitly given by reviewer.
- Every session must update its checkpoint markdown status.

## Recommended Reviewer Decision Tags
- `APPROVED`
- `NEEDS_REVISION`
