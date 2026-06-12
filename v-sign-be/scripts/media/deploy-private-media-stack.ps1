param(
    [string]$StackName = "v-sign-media",
    [string]$BucketName = "",
    [ValidateSet("PriceClass_100", "PriceClass_200", "PriceClass_All")]
    [string]$PriceClass = "PriceClass_200",
    [string]$Region = "ap-southeast-1",
    [string]$TemplatePath = ""
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command aws -ErrorAction SilentlyContinue)) {
    throw "AWS CLI is not installed or not available in PATH."
}

$backendRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
if (-not $TemplatePath) {
    $TemplatePath = Join-Path $backendRoot "infra\cloudformation\private-media-cloudfront-oac.yaml"
}

Write-Host "Checking AWS credentials..."
& aws sts get-caller-identity --region $Region | Out-Host
if ($LASTEXITCODE -ne 0) {
    throw "AWS credentials are not valid. Run 'aws configure' or 'aws sso login' before deploying."
}

$parameterOverrides = @("PriceClass=$PriceClass")
if ($BucketName.Trim()) {
    $parameterOverrides += "BucketName=$BucketName"
}

$deployArgs = @(
    "cloudformation", "deploy",
    "--stack-name", $StackName,
    "--template-file", $TemplatePath,
    "--region", $Region,
    "--parameter-overrides"
) + $parameterOverrides

Write-Host "Deploying CloudFormation stack '$StackName' in $Region..."
& aws @deployArgs | Out-Host
if ($LASTEXITCODE -ne 0) {
    throw "CloudFormation deploy failed."
}

Write-Host "Stack outputs:"
& aws cloudformation describe-stacks `
    --stack-name $StackName `
    --region $Region `
    --query "Stacks[0].Outputs[].{Key:OutputKey,Value:OutputValue}" `
    --output table | Out-Host
