param(
    [Parameter(Mandatory = $true)]
    [string]$BucketName,
    [string]$SourceVideoDir = "D:\raw_videos\archive\Dataset\Videos",
    [string]$Prefix = "videos",
    [string]$Region = "ap-southeast-1"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command aws -ErrorAction SilentlyContinue)) {
    throw "AWS CLI is not installed or not available in PATH."
}

$backendRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$migrationFiles = @(
    (Join-Path $backendRoot "src\main\resources\db\migration\V4__learning_catalog.sql"),
    (Join-Path $backendRoot "src\main\resources\db\migration\V11__ai_model_practice_items.sql")
)

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
                SourceVideoFile = $source.Trim("'")
                OrderIndex = [int]$match.Groups["order"].Value
            }
        }
    }
}

$normalizedPrefix = $Prefix.Trim("/")
$rows = Get-PracticeRows -Files $migrationFiles
$videoFiles = $rows |
    Select-Object -ExpandProperty SourceVideoFile -Unique |
    Sort-Object

if (-not $videoFiles -or $videoFiles.Count -eq 0) {
    throw "No source video files were found in practice_items seed migrations."
}

Write-Host "Checking AWS credentials..."
& aws sts get-caller-identity --region $Region | Out-Host
if ($LASTEXITCODE -ne 0) {
    throw "AWS credentials are not valid. Run 'aws configure' or 'aws sso login' before uploading."
}

$uploaded = 0
$missing = New-Object System.Collections.Generic.List[string]

foreach ($fileName in $videoFiles) {
    $sourcePath = Join-Path $SourceVideoDir $fileName
    if (-not (Test-Path $sourcePath)) {
        $missing.Add($fileName)
        continue
    }

    $key = "$normalizedPrefix/$fileName"
    $target = "s3://$BucketName/$key"
    Write-Host "Uploading $fileName -> $target"
    & aws s3 cp $sourcePath $target `
        --region $Region `
        --content-type "video/mp4" `
        --cache-control "public,max-age=31536000,immutable" `
        --only-show-errors | Out-Host

    if ($LASTEXITCODE -ne 0) {
        throw "Upload failed for $fileName."
    }

    $uploaded++
}

Write-Host "Uploaded $uploaded video file(s)."
if ($missing.Count -gt 0) {
    Write-Warning "Missing $($missing.Count) file(s) in ${SourceVideoDir}:"
    $missing | ForEach-Object { Write-Warning "  $_" }
}
