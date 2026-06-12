# Checkpoint 03: VSL Dictionary

## Goal

Provide public dictionary browsing/search/filter APIs and practice target lookup for frontend dictionary and guest access flows.

## API Contract

| Endpoint | Method | Auth | Request DTO | Response DTO | Status |
| --- | --- | --- | --- | --- | --- |
| `/api/v1/dictionary` | GET | Public | query params | `SuccessResponse<DictionaryEntriesPageResponse>` | Batch 1 accepted |
| `/api/v1/dictionary/{entryId}/practice-target` | GET | Optional USER | None | `SuccessResponse<PracticeTargetResponse>` | Batch 1 accepted |

## Current Implementation

| File | Status |
| --- | --- |
| `dictionary/controller/DictionaryPublicController.java` | `/api/v1/dictionary`, success envelope |
| `dictionary/service/DictionaryService.java` | In-memory list with category/keyword/difficulty filters |
| `dictionary/dto/DictionaryEntryResponse.java` | Basic response DTO |
| `dictionary/DictionaryIT.java` | Contract tests updated and passing in targeted run |

## DB/Migration Plan

| Table | Columns | Status |
| --- | --- | --- |
| `dictionary_entries` | `id`, `keyword`, `definition`, `category`, `difficulty`, `video_url`, `published`, timestamps | Not started |
| `dictionary_practice_targets` | `entry_id`, `unit_id`, `chapter_id`, `lesson_id`, `quiz_id`, `requires_premium` | Not started |

## Test Gates

- [x] Guest can list dictionary entries.
- [x] Category, keyword, difficulty, paging filters work for sample data.
- [x] Query length validation returns `VALIDATION_ERROR`.
- [ ] Only published entries are returned from DB.
- [x] Practice target endpoint returns deep link metadata.
- [x] Missing entry returns normalized `NOT_FOUND`.
- [x] Out-of-range pages preserve total page metadata.

## Review Decision

- [ ] Accept in-memory dictionary as first implementation checkpoint.
- [ ] Require migration/repository before continuing other epics.
