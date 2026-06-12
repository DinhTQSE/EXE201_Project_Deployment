param(
    [Parameter(Mandatory = $true)]
    [string]$AccountId,
    [Parameter(Mandatory = $true)]
    [string]$BucketName,
    [string]$SourceVideoDir = "D:\raw_videos\archive\Dataset\Videos",
    [string]$Prefix = "videos",
    [string]$Profile = "",
    [int]$MaxFiles = 0
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command aws -ErrorAction SilentlyContinue)) {
    throw "AWS CLI is not installed or not available in PATH."
}

if (-not (Test-Path $SourceVideoDir)) {
    throw "Source video directory does not exist: $SourceVideoDir"
}

$endpointUrl = "https://$AccountId.r2.cloudflarestorage.com"
$normalizedPrefix = $Prefix.Trim("/")
$videos = Get-ChildItem -Path $SourceVideoDir -File -Filter *.mp4 | Sort-Object Name

if ($MaxFiles -gt 0) {
    $videos = $videos | Select-Object -First $MaxFiles
}

if (-not $videos -or $videos.Count -eq 0) {
    throw "No .mp4 files were found in $SourceVideoDir"
}

$awsBaseArgs = @("--endpoint-url", $endpointUrl)
if ($Profile.Trim()) {
    $awsBaseArgs += @("--profile", $Profile)
}

Write-Host "Uploading $($videos.Count) video file(s) to R2 bucket '$BucketName' via $endpointUrl"

$index = 0
foreach ($video in $videos) {
    $index++
    $key = "$normalizedPrefix/$($video.Name)"
    $target = "s3://$BucketName/$key"
    Write-Host "[$index/$($videos.Count)] $($video.Name) -> $target"

    & aws @awsBaseArgs s3 cp $video.FullName $target `
        --content-type "video/mp4" `
        --cache-control "public,max-age=31536000,immutable" `
        --only-show-errors | Out-Host

    if ($LASTEXITCODE -ne 0) {
        throw "Upload failed for $($video.FullName)"
    }
}

Write-Host "Upload completed."
