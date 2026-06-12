-- V-Sign learning catalog expansion from dictionary_entries.
-- Run this in Supabase SQL Editor after the R2 video URL backfill has populated:
--   "v-sign_schema"."dictionary_entries".video_url
--
-- Result:
--   - 120 lessons per generated unit
--   - 30 lessons per generated chapter
--   - 1 practice item per lesson
--
-- Safe to rerun: generated units/chapters/lessons/practice_items are upserted.

begin;

with source_entries as (
    select
        d.id,
        d.word,
        row_number() over (order by coalesce(d.difficulty_level, 1), lower(d.word), d.id) as rn
    from "v-sign_schema"."dictionary_entries" d
    where d.is_published = true
      and d.video_url is not null
      and d.video_url <> ''
),
unit_rows as (
    select
        floor((rn - 1)::numeric / 120)::int + 1 as unit_no
    from source_entries
    group by floor((rn - 1)::numeric / 120)::int + 1
)
insert into "v-sign_schema"."learning_units"
    (unit_id, title, description, thumbnail_url, is_published, order_index, updated_at)
select
    'unit-dict-pack-' || lpad(unit_no::text, 2, '0') as unit_id,
    case
        when unit_no <= 4 then 'Tu vung nen tang ' || lpad(unit_no::text, 2, '0')
        when unit_no <= 12 then 'Tu vung thong dung ' || lpad((unit_no - 4)::text, 2, '0')
        when unit_no <= 20 then 'Tu vung mo rong ' || lpad((unit_no - 12)::text, 2, '0')
        else 'Tu vung nang cao ' || lpad((unit_no - 20)::text, 2, '0')
    end as title,
    case
        when unit_no <= 4 then 'Goi tu vung co ban sinh tu tu dien chinh thuc da gan video R2.'
        when unit_no <= 12 then 'Goi tu vung thuong dung giup mo rong von ky hieu hang ngay.'
        when unit_no <= 20 then 'Goi tu vung mo rong cho nhieu ngu canh hoc tap va giao tiep.'
        else 'Goi tu vung nang cao va it gap hon, danh cho giai doan on luyen sau.'
    end as description,
    null as thumbnail_url,
    true as is_published,
    100 + unit_no as order_index,
    now() as updated_at
from unit_rows
on conflict (unit_id) do update set
    title = excluded.title,
    description = excluded.description,
    is_published = true,
    order_index = excluded.order_index,
    updated_at = now();

with source_entries as (
    select
        d.id,
        d.word,
        row_number() over (order by coalesce(d.difficulty_level, 1), lower(d.word), d.id) as rn
    from "v-sign_schema"."dictionary_entries" d
    where d.is_published = true
      and d.video_url is not null
      and d.video_url <> ''
),
packed_entries as (
    select
        floor((rn - 1)::numeric / 120)::int + 1 as unit_no,
        floor(((rn - 1) % 120)::numeric / 30)::int + 1 as chapter_no
    from source_entries
)
insert into "v-sign_schema"."learning_chapters"
    (chapter_id, unit_id, title, description, is_premium, is_published, order_index, updated_at)
select
    'chapter-dict-pack-' || lpad(unit_no::text, 2, '0') || '-' || lpad(chapter_no::text, 2, '0') as chapter_id,
    'unit-dict-pack-' || lpad(unit_no::text, 2, '0') as unit_id,
    'Nhom tu vung ' || lpad(chapter_no::text, 2, '0') as title,
    '30 bai hoc ky hieu tu dictionary_entries, sap xep theo cap do va ten tu.' as description,
    (unit_no > 4) as is_premium,
    true as is_published,
    chapter_no as order_index,
    now() as updated_at
from packed_entries
group by unit_no, chapter_no
on conflict (chapter_id) do update set
    unit_id = excluded.unit_id,
    title = excluded.title,
    description = excluded.description,
    is_premium = excluded.is_premium,
    is_published = true,
    order_index = excluded.order_index,
    updated_at = now();

with source_entries as (
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
packed_entries as (
    select
        *,
        floor((rn - 1)::numeric / 120)::int + 1 as unit_no,
        floor(((rn - 1) % 120)::numeric / 30)::int + 1 as chapter_no,
        ((rn - 1) % 30)::int + 1 as lesson_no
    from source_entries
)
insert into "v-sign_schema"."learning_lessons"
    (lesson_id, chapter_id, title, description, video_url, duration_seconds, is_premium, is_published, order_index, updated_at)
select
    'lesson-dict-' || id::text as lesson_id,
    'chapter-dict-pack-' || lpad(unit_no::text, 2, '0') || '-' || lpad(chapter_no::text, 2, '0') as chapter_id,
    left(word, 160) as title,
    description,
    video_url,
    5 as duration_seconds,
    (unit_no > 4) as is_premium,
    true as is_published,
    lesson_no as order_index,
    now() as updated_at
from packed_entries
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

with source_entries as (
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
packed_entries as (
    select
        *,
        ((rn - 1) % 30)::int + 1 as lesson_no
    from source_entries
)
insert into "v-sign_schema"."practice_items"
    (practice_item_id, lesson_id, category, level, label, expected_gloss, source_video_file, is_published, order_index, video_url, updated_at)
select
    'practice-dict-' || id::text as practice_item_id,
    'lesson-dict-' || id::text as lesson_id,
    left(category, 60) as category,
    left(difficulty, 30) as level,
    left(word, 160) as label,
    left(upper(regexp_replace(word, '\s+', '_', 'g')), 120) as expected_gloss,
    left(substring(video_url from '[^/]+$'), 120) as source_video_file,
    true as is_published,
    lesson_no as order_index,
    video_url,
    now() as updated_at
from packed_entries
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
where p.practice_item_id like 'practice-dict-%'
  and not exists (
      select 1
      from "v-sign_schema"."practice_item_rubrics" existing
      where existing.practice_item_id = p.practice_item_id
        and existing.code = r.code
  );

commit;

-- Verification queries:
-- select count(*) as generated_units from "v-sign_schema"."learning_units" where unit_id like 'unit-dict-pack-%';
-- select count(*) as generated_lessons from "v-sign_schema"."learning_lessons" where lesson_id like 'lesson-dict-%';
-- select count(*) as generated_practice_items from "v-sign_schema"."practice_items" where practice_item_id like 'practice-dict-%';
