do $$
begin
    if to_regclass('"v-sign_schema"."dictionary_entries"') is not null then
        if pg_get_serial_sequence('"v-sign_schema"."dictionary_entries"', 'id') is not null then
            perform setval(
                pg_get_serial_sequence('"v-sign_schema"."dictionary_entries"', 'id')::regclass,
                coalesce((select max(id) from "v-sign_schema"."dictionary_entries"), 0) + 1,
                false
            );
        end if;

        with dictionary_videos as (
            select distinct on (lower(word)) word, category, difficulty, difficulty_level, description, video_url
            from "v-sign_schema"."vsign_dictionary_video_backfill_stage"
            where word is not null and word <> ''
            order by lower(word), default_rank, order_index
        ), updated as (
            update "v-sign_schema"."dictionary_entries" d
            set video_url = v.video_url,
                category = v.category,
                difficulty = v.difficulty,
                difficulty_level = v.difficulty_level,
                updated_at = now()
            from dictionary_videos v
            where lower(d.word) = lower(v.word)
            returning d.word
        )
        insert into "v-sign_schema"."dictionary_entries" (word, category, difficulty, difficulty_level, description, video_url, thumbnail_url, is_published)
        select v.word, v.category, v.difficulty, v.difficulty_level, v.description, v.video_url, null, true
        from dictionary_videos v
        where not exists (
            select 1 from "v-sign_schema"."dictionary_entries" d where lower(d.word) = lower(v.word)
        );
    end if;
end
$$;

do $$
begin
    if to_regclass('"v-sign_schema"."dictionary_entries"') is not null
       and to_regclass('"v-sign_schema"."dictionary_entry_video_variants"') is not null then
        insert into "v-sign_schema"."dictionary_entry_video_variants" (
            dictionary_entry_id, source_video_file, region_code, region_name, video_url, is_default
        )
        select d.id, v.source_video_file, v.region_code, v.region_name, v.video_url,
               v.default_rank = min(v.default_rank) over (partition by lower(v.word))
        from "v-sign_schema"."vsign_dictionary_video_backfill_stage" v
        join "v-sign_schema"."dictionary_entries" d on lower(d.word) = lower(v.word)
        where v.word is not null and v.word <> ''
        on conflict (dictionary_entry_id, source_video_file)
        do update set
            region_code = excluded.region_code,
            region_name = excluded.region_name,
            video_url = excluded.video_url,
            is_default = excluded.is_default,
            updated_at = now();
    end if;
end
$$;

drop table if exists "v-sign_schema"."vsign_dictionary_video_backfill_stage";
