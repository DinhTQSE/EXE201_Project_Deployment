param(
    [Parameter(Mandatory = $true)]
    [Alias("CloudFrontDomain")]
    [string]$MediaBaseUrl,
    [string]$Prefix = "videos",
    [string]$LabelCsvPath = "D:\raw_videos\archive\Dataset\Labels\label.csv",
    [string]$OutputPath = "",
    [ValidateSet("FIRST", "BAC", "TRUNG", "NAM", "TOAN_QUOC")]
    [string]$DefaultRegionCode = "FIRST",
    [string]$DbSchema = "public",
    [switch]$SkipDictionaryUpsert
)

$ErrorActionPreference = "Stop"

$backendRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$migrationFiles = @(
    (Join-Path $backendRoot "src\main\resources\db\migration\V4__learning_catalog.sql"),
    (Join-Path $backendRoot "src\main\resources\db\migration\V11__ai_model_practice_items.sql")
)

if (-not $OutputPath) {
    $OutputPath = Join-Path $PSScriptRoot "generated-video-url-backfill.sql"
}

function Escape-Sql {
    param([AllowNull()][string]$Value)
    if ($null -eq $Value) {
        return ""
    }
    return $Value.Replace("'", "''")
}

function Get-BaseUrl {
    param([string]$Domain)
    $value = $Domain.Trim().TrimEnd("/")
    if ($value -notmatch "^https?://") {
        $value = "https://$value"
    }
    return $value
}

function Quote-Ident {
    param([string]$Identifier)
    return '"' + $Identifier.Replace('"', '""') + '"'
}

function Get-QualifiedName {
    param(
        [string]$SchemaName,
        [string]$TableName
    )
    return "$(Quote-Ident $SchemaName).$(Quote-Ident $TableName)"
}

function Get-RegClassLiteral {
    param(
        [string]$SchemaName,
        [string]$TableName
    )
    $qualified = "$(Quote-Ident $SchemaName).$(Quote-Ident $TableName)"
    return $qualified.Replace("'", "''")
}

function Get-Difficulty {
    param([string]$Level)
    switch ($Level) {
        "intermediate" { return "TRUNG_BINH" }
        "advanced" { return "NANG_CAO" }
        default { return "CO_BAN" }
    }
}

function Get-DifficultyLevel {
    param([string]$Level)
    switch ($Level) {
        "intermediate" { return 2 }
        "advanced" { return 3 }
        default { return 1 }
    }
}

function Get-VideoRegion {
    param([string]$SourceVideoFile)

    $stem = [System.IO.Path]::GetFileNameWithoutExtension($SourceVideoFile)
    if ($stem -match "B$") {
        return [pscustomobject]@{ Code = "BAC"; Name = "Mien Bac" }
    }
    if ($stem -match "T$") {
        return [pscustomobject]@{ Code = "TRUNG"; Name = "Mien Trung" }
    }
    if ($stem -match "N$") {
        return [pscustomobject]@{ Code = "NAM"; Name = "Mien Nam" }
    }
    return [pscustomobject]@{ Code = "TOAN_QUOC"; Name = "Khong phan vung" }
}

function Get-DefaultRank {
    param(
        [string]$RegionCode,
        [int]$OrderIndex,
        [string]$PreferredRegionCode
    )

    if ($PreferredRegionCode -eq "FIRST") {
        return $OrderIndex
    }
    if ($RegionCode -eq $PreferredRegionCode) {
        return 0
    }
    if ($RegionCode -eq "TOAN_QUOC") {
        return 1
    }
    return 100000 + $OrderIndex
}

function Get-PracticeRows {
    param([string[]]$Files)

    $pattern = "\('(?<item>[^']+)'\s*,\s*'(?<lesson>[^']+)'\s*,\s*'(?<category>[^']+)'\s*,\s*'(?<level>[^']+)'\s*,\s*'(?<label>(?:''|[^'])*)'\s*,\s*'(?<gloss>[^']+)'\s*,\s*(?<source>null|'[^']+')\s*,\s*(?:true|false)\s*,\s*(?<order>\d+)\)"

    foreach ($file in $Files) {
        if (-not (Test-Path $file)) {
            continue
        }

        $content = Get-Content -Path $file -Raw -Encoding UTF8
        foreach ($match in [regex]::Matches($content, $pattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)) {
            $source = $match.Groups["source"].Value
            if ($source -eq "null") {
                continue
            }

            [pscustomobject]@{
                ItemId = $match.Groups["item"].Value
                LessonId = $match.Groups["lesson"].Value
                Category = $match.Groups["category"].Value
                Level = $match.Groups["level"].Value
                Label = $match.Groups["label"].Value.Replace("''", "'")
                ExpectedGloss = $match.Groups["gloss"].Value
                SourceVideoFile = $source.Trim("'")
                OrderIndex = [int]$match.Groups["order"].Value
            }
        }
    }
}

function Get-LabelMap {
    param([string]$Path)

    $map = @{}
    if (-not (Test-Path $Path)) {
        return $map
    }

    Import-Csv -Path $Path -Encoding UTF8 | ForEach-Object {
        if ($_.VIDEO -and $_.LABEL -and -not $map.ContainsKey($_.VIDEO)) {
            $map[$_.VIDEO] = $_.LABEL
        }
    }
    return $map
}

function Get-LabelRows {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        return @()
    }

    $index = 0
    return Import-Csv -Path $Path -Encoding UTF8 | ForEach-Object {
        $index++
        if ($_.VIDEO -and $_.LABEL) {
            [pscustomobject]@{
                Word = $_.LABEL
                SourceVideoFile = $_.VIDEO
                OrderIndex = $index
            }
        }
    }
}

$baseUrl = Get-BaseUrl -Domain $MediaBaseUrl
$normalizedPrefix = $Prefix.Trim("/")
$practiceItemsTable = Get-QualifiedName -SchemaName $DbSchema -TableName "practice_items"
$learningLessonsTable = Get-QualifiedName -SchemaName $DbSchema -TableName "learning_lessons"
$dictionaryEntriesTable = Get-QualifiedName -SchemaName $DbSchema -TableName "dictionary_entries"
$dictionaryVariantsTable = Get-QualifiedName -SchemaName $DbSchema -TableName "dictionary_entry_video_variants"
$practiceItemsRegClass = Get-RegClassLiteral -SchemaName $DbSchema -TableName "practice_items"
$learningLessonsRegClass = Get-RegClassLiteral -SchemaName $DbSchema -TableName "learning_lessons"
$dictionaryEntriesRegClass = Get-RegClassLiteral -SchemaName $DbSchema -TableName "dictionary_entries"
$dictionaryVariantsRegClass = Get-RegClassLiteral -SchemaName $DbSchema -TableName "dictionary_entry_video_variants"
$rows = Get-PracticeRows -Files $migrationFiles | Sort-Object OrderIndex
if (-not $rows -or $rows.Count -eq 0) {
    throw "No practice item rows with source videos were found."
}

$labelMap = Get-LabelMap -Path $LabelCsvPath
$labelRows = Get-LabelRows -Path $LabelCsvPath
$practiceBySource = @{}
foreach ($row in $rows) {
    if ($row.SourceVideoFile -and -not $practiceBySource.ContainsKey($row.SourceVideoFile)) {
        $practiceBySource[$row.SourceVideoFile] = $row
    }
}

$practiceValues = $rows | ForEach-Object {
    $url = "$baseUrl/$normalizedPrefix/$($_.SourceVideoFile)"
    "        ('$((Escape-Sql $_.ItemId))', '$((Escape-Sql $url))')"
}

$lessonValues = $rows | ForEach-Object {
    $url = "$baseUrl/$normalizedPrefix/$($_.SourceVideoFile)"
    "        ('$((Escape-Sql $_.LessonId))', '$((Escape-Sql $url))', $($_.OrderIndex))"
}

$dictionarySourceRows = if ($labelRows.Count -gt 0) {
    $labelRows | ForEach-Object {
        $practice = $practiceBySource[$_.SourceVideoFile]
        [pscustomobject]@{
            Word = $_.Word
            SourceVideoFile = $_.SourceVideoFile
            Category = if ($practice) { $practice.Category } else { "general" }
            Level = if ($practice) { $practice.Level } else { "beginner" }
            OrderIndex = $_.OrderIndex
        }
    }
} else {
    $rows | ForEach-Object {
        [pscustomobject]@{
            Word = if ($labelMap.ContainsKey($_.SourceVideoFile)) { $labelMap[$_.SourceVideoFile] } else { $_.Label }
            SourceVideoFile = $_.SourceVideoFile
            Category = $_.Category
            Level = $_.Level
            OrderIndex = $_.OrderIndex
        }
    }
}

$dictionaryValues = $dictionarySourceRows | ForEach-Object {
    $word = $_.Word
    $url = "$baseUrl/$normalizedPrefix/$($_.SourceVideoFile)"
    $difficulty = Get-Difficulty -Level $_.Level
    $difficultyLevel = Get-DifficultyLevel -Level $_.Level
    $description = "Video minh hoa ky hieu: $word"
    $region = Get-VideoRegion -SourceVideoFile $_.SourceVideoFile
    $defaultRank = Get-DefaultRank -RegionCode $region.Code -OrderIndex $_.OrderIndex -PreferredRegionCode $DefaultRegionCode
    "        ('$((Escape-Sql $word))', '$((Escape-Sql $_.Category))', '$difficulty', $difficultyLevel, '$((Escape-Sql $description))', '$((Escape-Sql $_.SourceVideoFile))', '$((Escape-Sql $url))', '$($region.Code)', '$((Escape-Sql $region.Name))', $defaultRank, $($_.OrderIndex))"
}

$sql = New-Object System.Collections.Generic.List[string]
$sql.Add("-- Generated by scripts/media/generate-video-url-backfill-sql.ps1")
$sql.Add("-- Generated media URLs must point to the public media delivery domain, not a private object-storage API endpoint.")
$sql.Add("-- Target schema: $DbSchema")
$sql.Add("begin;")
$sql.Add("")
$sql.Add("do `$`$")
$sql.Add("begin")
$sql.Add("    if to_regclass('$practiceItemsRegClass') is not null and exists (")
$sql.Add("        select 1")
$sql.Add("        from information_schema.columns")
$sql.Add("        where table_schema = '$DbSchema'")
$sql.Add("          and table_name = 'practice_items'")
$sql.Add("          and column_name = 'video_url'")
$sql.Add("    ) then")
$sql.Add("        update $practiceItemsTable p")
$sql.Add("        set video_url = v.video_url,")
$sql.Add("            updated_at = now()")
$sql.Add("        from (values")
$sql.Add(($practiceValues -join ",`n"))
$sql.Add("        ) as v(practice_item_id, video_url)")
$sql.Add("        where p.practice_item_id = v.practice_item_id;")
$sql.Add("    end if;")
$sql.Add("end")
$sql.Add("`$`$;")
$sql.Add("")
$sql.Add("do `$`$")
$sql.Add("begin")
$sql.Add("    if to_regclass('$learningLessonsRegClass') is not null and exists (")
$sql.Add("        select 1")
$sql.Add("        from information_schema.columns")
$sql.Add("        where table_schema = '$DbSchema'")
$sql.Add("          and table_name = 'learning_lessons'")
$sql.Add("          and column_name = 'video_url'")
$sql.Add("    ) then")
$sql.Add("        with lesson_video_candidates(lesson_id, video_url, order_index) as (")
$sql.Add("    values")
$sql.Add(($lessonValues -join ",`n"))
$sql.Add("        ), lesson_videos as (")
$sql.Add("            select distinct on (lesson_id) lesson_id, video_url")
$sql.Add("            from lesson_video_candidates")
$sql.Add("            order by lesson_id, order_index")
$sql.Add("        )")
$sql.Add("        update $learningLessonsTable l")
$sql.Add("        set video_url = v.video_url,")
$sql.Add("            updated_at = now()")
$sql.Add("        from lesson_videos v")
$sql.Add("        where l.lesson_id = v.lesson_id")
$sql.Add("          and (l.video_url is null or l.video_url = '' or l.video_url like 'https://cdn.vsign.test/%');")
$sql.Add("    end if;")
$sql.Add("end")
$sql.Add("`$`$;")
$sql.Add("")

if (-not $SkipDictionaryUpsert) {
    $sql.Add("drop table if exists tmp_vsign_dictionary_video_candidates;")
    $sql.Add("")
    $sql.Add("create temporary table tmp_vsign_dictionary_video_candidates (")
    $sql.Add("    word text not null,")
    $sql.Add("    category text not null,")
    $sql.Add("    difficulty text not null,")
    $sql.Add("    difficulty_level int not null,")
    $sql.Add("    description text not null,")
    $sql.Add("    source_video_file text not null,")
    $sql.Add("    video_url text not null,")
    $sql.Add("    region_code text not null,")
    $sql.Add("    region_name text not null,")
    $sql.Add("    default_rank int not null,")
    $sql.Add("    order_index int not null")
    $sql.Add(");")
    $sql.Add("")
    $sql.Add("insert into tmp_vsign_dictionary_video_candidates (")
    $sql.Add("    word, category, difficulty, difficulty_level, description, source_video_file, video_url, region_code, region_name, default_rank, order_index")
    $sql.Add(")")
    $sql.Add("values")
    $sql.Add(($dictionaryValues -join ",`n"))
    $sql.Add(";")
    $sql.Add("")
    $sql.Add("do `$`$")
    $sql.Add("begin")
    $sql.Add("    if to_regclass('$dictionaryEntriesRegClass') is not null then")
    $sql.Add("        if pg_get_serial_sequence('$dictionaryEntriesRegClass', 'id') is not null then")
    $sql.Add("            perform setval(")
    $sql.Add("                pg_get_serial_sequence('$dictionaryEntriesRegClass', 'id')::regclass,")
    $sql.Add("                coalesce((select max(id) from $dictionaryEntriesTable), 0) + 1,")
    $sql.Add("                false")
    $sql.Add("            );")
    $sql.Add("        end if;")
    $sql.Add("with dictionary_videos as (")
    $sql.Add("    select distinct on (lower(word)) word, category, difficulty, difficulty_level, description, video_url")
    $sql.Add("    from tmp_vsign_dictionary_video_candidates")
    $sql.Add("    where word is not null and word <> ''")
    $sql.Add("    order by lower(word), default_rank, order_index")
    $sql.Add("), updated as (")
    $sql.Add("    update $dictionaryEntriesTable d")
    $sql.Add("    set video_url = v.video_url,")
    $sql.Add("        category = v.category,")
    $sql.Add("        difficulty = v.difficulty,")
    $sql.Add("        difficulty_level = v.difficulty_level,")
    $sql.Add("        updated_at = now()")
    $sql.Add("    from dictionary_videos v")
    $sql.Add("    where lower(d.word) = lower(v.word)")
    $sql.Add("    returning d.word")
    $sql.Add(")")
    $sql.Add("insert into $dictionaryEntriesTable (word, category, difficulty, difficulty_level, description, video_url, thumbnail_url, is_published)")
    $sql.Add("select v.word, v.category, v.difficulty, v.difficulty_level, v.description, v.video_url, null, true")
    $sql.Add("from dictionary_videos v")
    $sql.Add("where not exists (")
    $sql.Add("    select 1 from $dictionaryEntriesTable d where lower(d.word) = lower(v.word)")
    $sql.Add(");")
    $sql.Add("    end if;")
    $sql.Add("end")
    $sql.Add("`$`$;")
    $sql.Add("")
    $sql.Add("do `$`$")
    $sql.Add("begin")
    $sql.Add("    if to_regclass('$dictionaryEntriesRegClass') is not null")
    $sql.Add("       and to_regclass('$dictionaryVariantsRegClass') is not null")
    $sql.Add("       and exists (")
    $sql.Add("        select 1")
    $sql.Add("        from information_schema.tables")
$sql.Add("        where table_schema = '$DbSchema'")
$sql.Add("          and table_name = 'dictionary_entry_video_variants'")
    $sql.Add("    ) then")
    $sql.Add("        insert into $dictionaryVariantsTable (")
    $sql.Add("            dictionary_entry_id, source_video_file, region_code, region_name, video_url, is_default")
    $sql.Add("        )")
    $sql.Add("        select d.id, v.source_video_file, v.region_code, v.region_name, v.video_url,")
    $sql.Add("               v.default_rank = min(v.default_rank) over (partition by lower(v.word))")
    $sql.Add("        from tmp_vsign_dictionary_video_candidates v")
$sql.Add("        join $dictionaryEntriesTable d on lower(d.word) = lower(v.word)")
    $sql.Add("        where v.word is not null and v.word <> ''")
    $sql.Add("        on conflict (dictionary_entry_id, source_video_file)")
    $sql.Add("        do update set")
    $sql.Add("            region_code = excluded.region_code,")
    $sql.Add("            region_name = excluded.region_name,")
    $sql.Add("            video_url = excluded.video_url,")
    $sql.Add("            is_default = excluded.is_default,")
    $sql.Add("            updated_at = now();")
    $sql.Add("    end if;")
    $sql.Add("end")
    $sql.Add("`$`$;")
    $sql.Add("")
    $sql.Add("drop table if exists tmp_vsign_dictionary_video_candidates;")
    $sql.Add("")
}

$sql.Add("commit;")

$outputDir = Split-Path -Parent $OutputPath
if ($outputDir -and -not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
}

Set-Content -Path $OutputPath -Value ($sql -join "`n") -Encoding UTF8
Write-Host "Generated SQL: $OutputPath"
