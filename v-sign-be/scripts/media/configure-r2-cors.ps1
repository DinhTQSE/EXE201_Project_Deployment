param(
    [Parameter(Mandatory = $true)]
    [string]$AccountId,
    [Parameter(Mandatory = $true)]
    [string]$BucketName,
    [Parameter(Mandatory = $true)]
    [string[]]$AllowedOrigins,
    [string]$Profile = "",
    [int]$MaxAgeSeconds = 86400,
    [string]$OutputPath = ""
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command aws -ErrorAction SilentlyContinue)) {
    throw "AWS CLI is not installed or not available in PATH."
}

$origins = $AllowedOrigins |
    ForEach-Object { $_.Trim().TrimEnd("/") } |
    Where-Object { $_ } |
    Sort-Object -Unique

if (-not $origins -or $origins.Count -eq 0) {
    throw "At least one allowed origin is required."
}

foreach ($origin in $origins) {
    if ($origin -notmatch "^https://") {
        throw "Production/staging R2 CORS origins must be HTTPS origins. Invalid origin: $origin"
    }
}

if (-not $OutputPath) {
    $OutputPath = Join-Path $PSScriptRoot "generated-r2-cors.json"
}

$cors = [ordered]@{
    CORSRules = @(
        [ordered]@{
            AllowedOrigins = @($origins)
            AllowedMethods = @("GET", "HEAD")
            AllowedHeaders = @("Range", "If-None-Match", "If-Modified-Since")
            ExposeHeaders = @("Accept-Ranges", "Content-Length", "Content-Range", "Content-Type", "ETag", "Cache-Control")
            MaxAgeSeconds = $MaxAgeSeconds
        }
    )
}

$outputDir = Split-Path -Parent $OutputPath
if ($outputDir -and -not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
}

$cors | ConvertTo-Json -Depth 10 | Set-Content -Path $OutputPath -Encoding UTF8

$endpointUrl = "https://$AccountId.r2.cloudflarestorage.com"
$awsBaseArgs = @("--endpoint-url", $endpointUrl)
if ($Profile.Trim()) {
    $awsBaseArgs += @("--profile", $Profile)
}

Write-Host "Generated CORS config: $OutputPath"
Write-Host "Applying CORS to R2 bucket '$BucketName' via $endpointUrl"

& aws @awsBaseArgs s3api put-bucket-cors `
    --bucket $BucketName `
    --cors-configuration "file://$OutputPath" | Out-Host

if ($LASTEXITCODE -ne 0) {
    throw "Failed to apply R2 CORS configuration."
}

Write-Host "R2 CORS configuration applied."
