-- V-Sign curated unit vocabulary expansion.
-- Run this after dictionary_entries.video_url has been populated.
--
-- Purpose:
--   Existing curated units currently have too few lessons.
--   This script adds dictionary-backed vocabulary lessons into the existing
--   non-generated chapters so the first course pages immediately show richer units.
--
-- Behavior:
--   - Adds 20 dictionary lessons per existing curated chapter.
--   - Keeps original unit/chapter IDs and titles.
--   - Uses lesson IDs `lesson-curated-ext-<dictionary_id>`.
--   - Safe to rerun via ON CONFLICT upserts.

begin;

with target_chapters as (
    select
        c.chapter_id,
        row_number() over (order by u.order_index, c.order_index, c.chapter_id) as chapter_slot,
        count(*) over () as chapter_count
    from "v-sign_schema"."learning_chapters" c
    join "v-sign_schema"."learning_units" u on u.unit_id = c.unit_id
    where u.unit_id not like 'unit-dict-pack-%'
      and c.chapter_id not like 'chapter-dict-pack-%'
      and u.is_published = true
      and c.is_published = true
),
source_entries as (
    select
        d.id,
        d.word,
        coalesce(nullif(d.description, ''), 'Video minh hoa ky hieu: ' || d.word) as description,
        d.video_url,
        row_number() over (order by coalesce(d.difficulty_level, 1), lower(d.word), d.id) as rn
    from "v-sign_schema"."dictionary_entries" d
    where d.is_published = true
      and d.video_url is not null
      and d.video_url <> ''
),
assigned_entries as (
    select
        s.id,
        s.word,
        s.description,
        s.video_url,
        t.chapter_id,
        1000 + floor((s.rn - 1)::numeric / nullif(t.chapter_count, 0))::int + 1 as lesson_order
    from source_entries s
    join target_chapters t
      on t.chapter_slot = ((s.rn - 1) % t.chapter_count) + 1
    where s.rn <= t.chapter_count * 20
)
insert into "v-sign_schema"."learning_lessons"
    (lesson_id, chapter_id, title, description, video_url, duration_seconds, is_premium, is_published, order_index, updated_at)
select
    'lesson-curated-ext-' || id::text as lesson_id,
    chapter_id,
    left(word, 160) as title,
    description,
    video_url,
    5 as duration_seconds,
    false as is_premium,
    true as is_published,
    lesson_order as order_index,
    now() as updated_at
from assigned_entries
on conflict (lesson_id) do update set
    chapter_id = excluded.chapter_id,
    title = excluded.title,
    description = excluded.description,
    video_url = excluded.video_url,
    duration_seconds = excluded.duration_seconds,
    is_premium = excluded.is_premium,
    is_published = true,
    order_index = excluded.order_index,
    updated_at = now();

with target_chapters as (
    select
        c.chapter_id,
        row_number() over (order by u.order_index, c.order_index, c.chapter_id) as chapter_slot,
        count(*) over () as chapter_count
    from "v-sign_schema"."learning_chapters" c
    join "v-sign_schema"."learning_units" u on u.unit_id = c.unit_id
    where u.unit_id not like 'unit-dict-pack-%'
      and c.chapter_id not like 'chapter-dict-pack-%'
      and u.is_published = true
      and c.is_published = true
),
source_entries as (
    select
        d.id,
        d.word,
        coalesce(nullif(d.category, ''), 'general') as category,
        coalesce(nullif(d.difficulty, ''), 'CO_BAN') as difficulty,
        d.video_url,
        row_number() over (order by coalesce(d.difficulty_level, 1), lower(d.word), d.id) as rn
    from "v-sign_schema"."dictionary_entries" d
    where d.is_published = true
      and d.video_url is not null
      and d.video_url <> ''
),
assigned_entries as (
    select
        s.id,
        s.word,
        s.category,
        s.difficulty,
        s.video_url,
        1000 + floor((s.rn - 1)::numeric / nullif(t.chapter_count, 0))::int + 1 as item_order
    from source_entries s
    join target_chapters t
      on t.chapter_slot = ((s.rn - 1) % t.chapter_count) + 1
    where s.rn <= t.chapter_count * 20
)
insert into "v-sign_schema"."practice_items"
    (practice_item_id, lesson_id, category, level, label, expected_gloss, source_video_file, is_published, order_index, video_url, updated_at)
select
    'practice-curated-ext-' || id::text as practice_item_id,
    'lesson-curated-ext-' || id::text as lesson_id,
    left(category, 60) as category,
    left(difficulty, 30) as level,
    left(word, 160) as label,
    left(upper(regexp_replace(word, '\s+', '_', 'g')), 120) as expected_gloss,
    left(substring(video_url from '[^/]+$'), 120) as source_video_file,
    true as is_published,
    item_order as order_index,
    video_url,
    now() as updated_at
from assigned_entries
on conflict (practice_item_id) do update set
    lesson_id = excluded.lesson_id,
    category = excluded.category,
    level = excluded.level,
    label = excluded.label,
    expected_gloss = excluded.expected_gloss,
    source_video_file = excluded.source_video_file,
    video_url = excluded.video_url,
    is_published = true,
    order_index = excluded.order_index,
    updated_at = now();

insert into "v-sign_schema"."practice_item_rubrics" (practice_item_id, code, order_index)
select p.practice_item_id, r.code, r.order_index
from "v-sign_schema"."practice_items" p
cross join (
    values
        ('HAND_SHAPE_MATCH', 1),
        ('PALM_ORIENTATION_MATCH', 2),
        ('MOVEMENT_PATH_MATCH', 3)
) as r(code, order_index)
where p.practice_item_id like 'practice-curated-ext-%'
  and not exists (
      select 1
      from "v-sign_schema"."practice_item_rubrics" existing
      where existing.practice_item_id = p.practice_item_id
        and existing.code = r.code
  );

commit;

-- Verification queries:
-- select chapter_id, count(*) from "v-sign_schema"."learning_lessons" where lesson_id like 'lesson-curated-ext-%' group by chapter_id order by chapter_id;
-- select count(*) as curated_extension_lessons from "v-sign_schema"."learning_lessons" where lesson_id like 'lesson-curated-ext-%';
