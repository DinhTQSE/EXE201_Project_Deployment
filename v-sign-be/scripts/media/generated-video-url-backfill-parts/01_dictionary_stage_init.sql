drop table if exists "v-sign_schema"."vsign_dictionary_video_backfill_stage";

create table "v-sign_schema"."vsign_dictionary_video_backfill_stage" (
    word text not null,
    category text not null,
    difficulty text not null,
    difficulty_level int not null,
    description text not null,
    source_video_file text not null,
    video_url text not null,
    region_code text not null,
    region_name text not null,
    default_rank int not null,
    order_index int not null
);
