param(
    [string]$InputPath = "",
    [string]$OutputDir = "",
    [string]$DbSchema = "v-sign_schema",
    [int]$BatchSize = 350
)

$ErrorActionPreference = "Stop"

if (-not $InputPath) {
    $InputPath = Join-Path $PSScriptRoot "generated-video-url-backfill.sql"
}
if (-not $OutputDir) {
    $OutputDir = Join-Path $PSScriptRoot "generated-video-url-backfill-parts"
}

function Quote-Ident {
    param([string]$Identifier)
    return '"' + $Identifier.Replace('"', '""') + '"'
}

function Qualified {
    param([string]$SchemaName, [string]$TableName)
    return "$(Quote-Ident $SchemaName).$(Quote-Ident $TableName)"
}

function RegClassLiteral {
    param([string]$SchemaName, [string]$TableName)
    return (Qualified -SchemaName $SchemaName -TableName $TableName).Replace("'", "''")
}

if (-not (Test-Path $InputPath)) {
    throw "Input SQL not found: $InputPath"
}

$content = Get-Content -Path $InputPath -Raw -Encoding UTF8
$firstDictionaryMarker = "drop table if exists tmp_vsign_dictionary_video_candidates;"
$markerIndex = $content.IndexOf($firstDictionaryMarker)
if ($markerIndex -lt 0) {
    throw "Could not find dictionary staging marker in $InputPath"
}

$practiceLessonSql = $content.Substring(0, $markerIndex).Trim()
if ($practiceLessonSql -notmatch "(?im)^\s*commit;\s*$") {
    $practiceLessonSql = "$practiceLessonSql`ncommit;"
}

$valuesMatch = [regex]::Match(
    $content,
    "insert\s+into\s+tmp_vsign_dictionary_video_candidates\s*\([^)]*\)\s*values\s*(?<values>.*?)\n;",
    [System.Text.RegularExpressions.RegexOptions]::Singleline -bor [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
)
if (-not $valuesMatch.Success) {
    throw "Could not extract dictionary values from $InputPath"
}

$rows = @()
foreach ($line in $valuesMatch.Groups["values"].Value -split "`r?`n") {
    $trimmed = $line.Trim()
    if ($trimmed.StartsWith("(")) {
        $rows += $trimmed.TrimEnd(",")
    }
}
if ($rows.Count -eq 0) {
    throw "No dictionary rows found in $InputPath"
}

if (Test-Path $OutputDir) {
    Get-ChildItem -Path $OutputDir -Filter "*.sql" | Remove-Item -Force
} else {
    New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
}

$stageTable = Qualified -SchemaName $DbSchema -TableName "vsign_dictionary_video_backfill_stage"
$dictionaryEntriesTable = Qualified -SchemaName $DbSchema -TableName "dictionary_entries"
$dictionaryVariantsTable = Qualified -SchemaName $DbSchema -TableName "dictionary_entry_video_variants"
$dictionaryEntriesRegClass = RegClassLiteral -SchemaName $DbSchema -TableName "dictionary_entries"
$dictionaryVariantsRegClass = RegClassLiteral -SchemaName $DbSchema -TableName "dictionary_entry_video_variants"
$pgDoDelimiter = '$$'

Set-Content -Path (Join-Path $OutputDir "00_practice_and_lessons.sql") -Encoding UTF8 -Value $practiceLessonSql

$initSql = @"
drop table if exists $stageTable;

create table $stageTable (
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
"@
Set-Content -Path (Join-Path $OutputDir "01_dictionary_stage_init.sql") -Encoding UTF8 -Value $initSql

$batchIndex = 1
for ($offset = 0; $offset -lt $rows.Count; $offset += $BatchSize) {
    $batchRows = $rows[$offset..([Math]::Min($offset + $BatchSize - 1, $rows.Count - 1))]
    $batchName = "10_dictionary_stage_insert_{0:D3}.sql" -f $batchIndex
    $batchSql = New-Object System.Collections.Generic.List[string]
    $batchSql.Add("insert into $stageTable (")
    $batchSql.Add("    word, category, difficulty, difficulty_level, description, source_video_file, video_url, region_code, region_name, default_rank, order_index")
    $batchSql.Add(")")
    $batchSql.Add("values")
    $batchSql.Add(($batchRows | ForEach-Object { "    $_" }) -join ",`n")
    $batchSql.Add(";")
    Set-Content -Path (Join-Path $OutputDir $batchName) -Encoding UTF8 -Value ($batchSql -join "`n")
    $batchIndex++
}

$applySql = @"
do $pgDoDelimiter
begin
    if to_regclass('$dictionaryEntriesRegClass') is not null then
        if pg_get_serial_sequence('$dictionaryEntriesRegClass', 'id') is not null then
            perform setval(
                pg_get_serial_sequence('$dictionaryEntriesRegClass', 'id')::regclass,
                coalesce((select max(id) from $dictionaryEntriesTable), 0) + 1,
                false
            );
        end if;

        with dictionary_videos as (
            select distinct on (lower(word)) word, category, difficulty, difficulty_level, description, video_url
            from $stageTable
            where word is not null and word <> ''
            order by lower(word), default_rank, order_index
        ), updated as (
            update $dictionaryEntriesTable d
            set video_url = v.video_url,
                category = v.category,
                difficulty = v.difficulty,
                difficulty_level = v.difficulty_level,
                updated_at = now()
            from dictionary_videos v
            where lower(d.word) = lower(v.word)
            returning d.word
        )
        insert into $dictionaryEntriesTable (word, category, difficulty, difficulty_level, description, video_url, thumbnail_url, is_published)
        select v.word, v.category, v.difficulty, v.difficulty_level, v.description, v.video_url, null, true
        from dictionary_videos v
        where not exists (
            select 1 from $dictionaryEntriesTable d where lower(d.word) = lower(v.word)
        );
    end if;
end
$pgDoDelimiter;

do $pgDoDelimiter
begin
    if to_regclass('$dictionaryEntriesRegClass') is not null
       and to_regclass('$dictionaryVariantsRegClass') is not null then
        insert into $dictionaryVariantsTable (
            dictionary_entry_id, source_video_file, region_code, region_name, video_url, is_default
        )
        select d.id, v.source_video_file, v.region_code, v.region_name, v.video_url,
               v.default_rank = min(v.default_rank) over (partition by lower(v.word))
        from $stageTable v
        join $dictionaryEntriesTable d on lower(d.word) = lower(v.word)
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
$pgDoDelimiter;

drop table if exists $stageTable;
"@
Set-Content -Path (Join-Path $OutputDir "90_dictionary_apply_and_cleanup.sql") -Encoding UTF8 -Value $applySql

Write-Host "Split SQL files written to: $OutputDir"
Write-Host "Dictionary rows: $($rows.Count)"
Write-Host "Dictionary insert batches: $($batchIndex - 1)"
