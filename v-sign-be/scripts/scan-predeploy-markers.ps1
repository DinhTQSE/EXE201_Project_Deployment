param(
    [string]$Root = (Resolve-Path "$PSScriptRoot\..\..").Path,
    [int]$MaxRows = 200,
    [switch]$NoFail
)

$ErrorActionPreference = "Stop"

$tokens = @(
    "vsign.test",
    "pay.vsign.test",
    "cdn.vsign.test",
    "localhost",
    "learner-001",
    "learner.premium@vsign.test",
    "premium@vsign.vn",
    "retired-plan"
)

$excludedDirectories = @(
    ".git",
    ".idea",
    "docs",
    "node_modules",
    "target",
    "dist",
    "venv",
    "__pycache__",
    "test"
)

$includedExtensions = @(
    ".java",
    ".ts",
    ".tsx",
    ".js",
    ".jsx",
    ".sql",
    ".properties",
    ".yml",
    ".yaml",
    ".json",
    ".ps1",
    ".py"
)

$rootPath = (Resolve-Path $Root).Path
$selfPath = $PSCommandPath
$pattern = ($tokens | ForEach-Object { [regex]::Escape($_) }) -join "|"
$allowedLocalhostPathPatterns = @(
    "\\docker-compose\.prod\.yml$",
    "\\Caddyfile$",
    "\\src\\main\\resources\\application\.properties$",
    "\\src\\main\\java\\com\\vsign\\backend\\common\\security\\SecurityConfig\.java$",
    "\\src\\main\\java\\com\\vsign\\backend\\learning\\service\\AiPredictionProxyService\.java$",
    "\\openapi-backend\.json$"
)

$files = Get-ChildItem -LiteralPath $rootPath -Recurse -File | Where-Object {
    $file = $_
    if ($selfPath -and $file.FullName -eq $selfPath) {
        return $false
    }
    if ($includedExtensions -notcontains $file.Extension) {
        return $false
    }
    foreach ($directory in $excludedDirectories) {
        if ($file.FullName -match "\\$([regex]::Escape($directory))\\") {
            return $false
        }
    }
    return $true
}

$findings = foreach ($file in $files) {
    Select-String -LiteralPath $file.FullName -Pattern $pattern -AllMatches | ForEach-Object {
        $matchValues = @($_.Matches | Select-Object -ExpandProperty Value -Unique)
        $onlyAllowedLocalhost = $matchValues.Count -eq 1 -and $matchValues[0] -eq "localhost"
        if ($onlyAllowedLocalhost) {
            $allowedPath = $false
            foreach ($allowedPattern in $allowedLocalhostPathPatterns) {
                if ($_.Path -match $allowedPattern) {
                    $allowedPath = $true
                    break
                }
            }
            if ($allowedPath) {
                return
            }
        }
        [pscustomobject]@{
            Path = Resolve-Path -LiteralPath $_.Path -Relative
            Line = $_.LineNumber
            Match = $matchValues -join ","
        }
    }
}

if (-not $findings) {
    Write-Host "No predeploy mock/domain markers found."
    exit 0
}

$sortedFindings = @($findings | Sort-Object Path, Line)
$summary = $sortedFindings | Group-Object Path | Sort-Object Count -Descending | ForEach-Object {
    [pscustomobject]@{
        Count = $_.Count
        Path = $_.Name
    }
}

Write-Host "Predeploy marker summary by file:"
$summary | Format-Table -AutoSize

Write-Host "Showing first $MaxRows detailed finding(s):"
$sortedFindings | Select-Object -First $MaxRows | Format-Table -AutoSize

if (-not $NoFail) {
    Write-Error "Found $($sortedFindings.Count) predeploy marker occurrence(s). Clean them up or rerun with -NoFail for reporting only."
    exit 1
}

Write-Host "Found $($sortedFindings.Count) predeploy marker occurrence(s)."
