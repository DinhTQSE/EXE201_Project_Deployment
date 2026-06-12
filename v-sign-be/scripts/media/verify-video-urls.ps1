param(
    [string]$ApiBaseUrl = "",
    [string[]]$SqlPath = @(),
    [string[]]$UrlFile = @(),
    [int]$PageSize = 100,
    [int]$TimeoutSeconds = 15,
    [int]$MaxUrls = 0,
    [switch]$ListOnly,
    [switch]$RequireCacheControl,
    [int]$MinCacheMaxAgeSeconds = 2592000,
    [string]$ReportPath = ""
)

$ErrorActionPreference = "Stop"

$script:VideoUrlByUrl = [ordered]@{}

function Add-VideoUrl {
    param(
        [AllowNull()][string]$Url,
        [string]$Source
    )

    if ([string]::IsNullOrWhiteSpace($Url)) {
        return
    }

    $normalized = $Url.Trim()
    if ($normalized -notmatch "^https?://") {
        return
    }

    if (-not $script:VideoUrlByUrl.Contains($normalized)) {
        $script:VideoUrlByUrl[$normalized] = [pscustomobject]@{
            Url = $normalized
            Sources = New-Object System.Collections.Generic.List[string]
        }
    }
    $script:VideoUrlByUrl[$normalized].Sources.Add($Source)
}

function Join-ApiUrl {
    param(
        [string]$BaseUrl,
        [string]$Path
    )

    return "$($BaseUrl.TrimEnd("/"))/$($Path.TrimStart("/"))"
}

function Add-UrlsFromObject {
    param(
        [AllowNull()]$Value,
        [string]$Source
    )

    if ($null -eq $Value) {
        return
    }

    if ($Value -is [string]) {
        Add-VideoUrl -Url $Value -Source $Source
        return
    }

    if ($Value -is [System.Collections.IEnumerable] -and -not ($Value -is [string])) {
        foreach ($item in $Value) {
            Add-UrlsFromObject -Value $item -Source $Source
        }
        return
    }

    $properties = $Value.PSObject.Properties
    foreach ($property in $properties) {
        if ($property.Name -match "videoUrl|thumbnailUrl") {
            Add-VideoUrl -Url ([string]$property.Value) -Source "$Source.$($property.Name)"
        } elseif ($property.Value -isnot [string]) {
            Add-UrlsFromObject -Value $property.Value -Source "$Source.$($property.Name)"
        }
    }
}

function Invoke-ApiGet {
    param([string]$Url)
    return Invoke-RestMethod -Method Get -Uri $Url -TimeoutSec $TimeoutSeconds
}

function Add-UrlsFromApi {
    param([string]$BaseUrl)

    $base = $BaseUrl.TrimEnd("/")

    $page = 0
    do {
        $response = Invoke-ApiGet -Url (Join-ApiUrl $base "/api/v1/dictionary?page=$page&size=$PageSize")
        Add-UrlsFromObject -Value $response.data.content -Source "api.dictionary.page$page"
        $totalPages = [int]$response.data.totalPages
        $page++
    } while ($page -lt $totalPages)

    $page = 0
    do {
        $response = Invoke-ApiGet -Url (Join-ApiUrl $base "/api/v1/learning/practice-items?page=$page&size=$PageSize")
        Add-UrlsFromObject -Value $response.data.content -Source "api.practice-items.page$page"
        $totalPages = [int]$response.data.totalPages
        $page++
    } while ($page -lt $totalPages)

    $page = 0
    do {
        $response = Invoke-ApiGet -Url (Join-ApiUrl $base "/api/v1/units?page=$page&size=$PageSize")
        $units = @($response.data.units)
        $totalPages = [int]$response.data.totalPages

        foreach ($unit in $units) {
            $chaptersResponse = Invoke-ApiGet -Url (Join-ApiUrl $base "/api/v1/units/$($unit.unitId)/chapters")
            foreach ($chapter in @($chaptersResponse.data.chapters)) {
                $lessonsResponse = Invoke-ApiGet -Url (Join-ApiUrl $base "/api/v1/chapters/$($chapter.chapterId)/lessons")
                Add-UrlsFromObject -Value $lessonsResponse.data.lessons -Source "api.lessons.$($chapter.chapterId)"
            }
        }

        $page++
    } while ($page -lt $totalPages)
}

function Add-UrlsFromSql {
    param([string[]]$Paths)

    foreach ($path in $Paths) {
        if (-not (Test-Path $path)) {
            throw "SQL path does not exist: $path"
        }

        $content = Get-Content -Path $path -Raw -Encoding UTF8
        $matches = [regex]::Matches($content, 'https?://[^''")\s]+\.mp4(?:\?[^''")\s]+)?', [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
        foreach ($match in $matches) {
            Add-VideoUrl -Url $match.Value -Source "sql:$path"
        }
    }
}

function Add-UrlsFromTextFile {
    param([string[]]$Paths)

    foreach ($path in $Paths) {
        if (-not (Test-Path $path)) {
            throw "URL file does not exist: $path"
        }

        Get-Content -Path $path -Encoding UTF8 | ForEach-Object {
            $line = $_.Trim()
            if ($line -and -not $line.StartsWith("#")) {
                Add-VideoUrl -Url $line -Source "file:$path"
            }
        }
    }
}

function Get-CacheMaxAge {
    param([AllowNull()][string]$CacheControl)

    if ([string]::IsNullOrWhiteSpace($CacheControl)) {
        return $null
    }

    $match = [regex]::Match($CacheControl, "max-age=(\d+)", [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
    if (-not $match.Success) {
        return $null
    }

    return [int]$match.Groups[1].Value
}

function Test-VideoUrl {
    param([string]$Url)

    $statusCode = $null
    $contentType = ""
    $cacheControl = ""
    $errorMessage = ""

    try {
        $response = Invoke-WebRequest -Method Head -Uri $Url -TimeoutSec $TimeoutSeconds -MaximumRedirection 5
        $statusCode = [int]$response.StatusCode
        $contentType = [string]$response.Headers["Content-Type"]
        $cacheControl = [string]$response.Headers["Cache-Control"]
    } catch {
        try {
            $response = Invoke-WebRequest -Method Get -Uri $Url -Headers @{ Range = "bytes=0-0" } -TimeoutSec $TimeoutSeconds -MaximumRedirection 5
            $statusCode = [int]$response.StatusCode
            $contentType = [string]$response.Headers["Content-Type"]
            $cacheControl = [string]$response.Headers["Cache-Control"]
        } catch {
            $errorMessage = $_.Exception.Message
        }
    }

    $cacheMaxAge = Get-CacheMaxAge -CacheControl $cacheControl
    $okStatus = $statusCode -in @(200, 206)
    $okType = $contentType -match "^video/mp4\b"
    $okCache = -not $RequireCacheControl -or ($null -ne $cacheMaxAge -and $cacheMaxAge -ge $MinCacheMaxAgeSeconds)
    $ok = $okStatus -and $okType -and $okCache

    [pscustomobject]@{
        Url = $Url
        Ok = $ok
        StatusCode = $statusCode
        ContentType = $contentType
        CacheControl = $cacheControl
        CacheMaxAgeSeconds = $cacheMaxAge
        Sources = ($script:VideoUrlByUrl[$Url].Sources | Sort-Object -Unique) -join ";"
        Error = $errorMessage
    }
}

if (-not $ApiBaseUrl -and $SqlPath.Count -eq 0 -and $UrlFile.Count -eq 0) {
    $defaultSql = Join-Path $PSScriptRoot "generated-video-url-backfill.sql"
    if (Test-Path $defaultSql) {
        $SqlPath = @($defaultSql)
    } else {
        throw "No input provided. Pass -ApiBaseUrl, -SqlPath, or -UrlFile."
    }
}

if ($ApiBaseUrl) {
    Add-UrlsFromApi -BaseUrl $ApiBaseUrl
}

if ($SqlPath.Count -gt 0) {
    Add-UrlsFromSql -Paths $SqlPath
}

if ($UrlFile.Count -gt 0) {
    Add-UrlsFromTextFile -Paths $UrlFile
}

$urls = @($script:VideoUrlByUrl.Keys)
if ($MaxUrls -gt 0) {
    $urls = $urls | Select-Object -First $MaxUrls
}

if ($urls.Count -eq 0) {
    throw "No video URLs were found."
}

if ($ListOnly) {
    $items = $urls | ForEach-Object {
        [pscustomobject]@{
            Url = $_
            Sources = ($script:VideoUrlByUrl[$_].Sources | Sort-Object -Unique) -join ";"
        }
    }

    if ($ReportPath) {
        $reportDir = Split-Path -Parent $ReportPath
        if ($reportDir -and -not (Test-Path $reportDir)) {
            New-Item -ItemType Directory -Force -Path $reportDir | Out-Null
        }
        $items | Export-Csv -Path $ReportPath -NoTypeInformation -Encoding UTF8
        Write-Host "Report written: $ReportPath"
    }

    Write-Host "Found $($items.Count) video URL(s)."
    return
}

Write-Host "Verifying $($urls.Count) video URL(s)."

$results = New-Object System.Collections.Generic.List[object]
$index = 0
foreach ($url in $urls) {
    $index++
    Write-Progress -Activity "Verifying video URLs" -Status "$index / $($urls.Count)" -PercentComplete (($index / $urls.Count) * 100)
    $result = Test-VideoUrl -Url $url
    $results.Add($result)
}
Write-Progress -Activity "Verifying video URLs" -Completed

$failures = @($results | Where-Object { -not $_.Ok })

if ($ReportPath) {
    $reportDir = Split-Path -Parent $ReportPath
    if ($reportDir -and -not (Test-Path $reportDir)) {
        New-Item -ItemType Directory -Force -Path $reportDir | Out-Null
    }
    $results | Export-Csv -Path $ReportPath -NoTypeInformation -Encoding UTF8
    Write-Host "Report written: $ReportPath"
}

Write-Host "Checked: $($results.Count), Failed: $($failures.Count)"

if ($failures.Count -gt 0) {
    $failures | Select-Object -First 20 | Format-Table -AutoSize | Out-Host
    throw "Video URL verification failed for $($failures.Count) URL(s)."
}

Write-Host "All video URLs passed."
