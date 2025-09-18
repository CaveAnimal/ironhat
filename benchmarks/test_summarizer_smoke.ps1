<#
Test script for summarize_sweep.ps1 - smoke checks for selection logic

Creates small aggregated CSV files representing different scenarios and runs
the summarizer to validate:
  - selection when all ef meet threshold
  - behavior when none meet threshold (both without and with -bestByMaxRecall)
  - tie-breaker behavior using -tieBreaker buildMs

Exits with code 0 on success, non-zero on failure.
#>
Set-StrictMode -Version Latest
Set-Location -Path $PSScriptRoot

function Fail([string]$msg) {
    Write-Host "[FAIL] $msg" -ForegroundColor Red
    exit 1
}
function Pass([string]$msg) { Write-Host "[PASS] $msg" -ForegroundColor Green }

$summ = Join-Path $PSScriptRoot 'summarize_sweep.ps1'
if (-not (Test-Path $summ)) { Fail "summarizer not found at $summ" }

$tmpDir = Join-Path $PSScriptRoot 'test_smoke_tmp'
if (Test-Path $tmpDir) { Remove-Item -Recurse -Force $tmpDir }
New-Item -ItemType Directory -Path $tmpDir | Out-Null

function RunScenario($name, $csvContent, $summArgs, $assertAction) {
    $inFile = Join-Path $tmpDir "agg_${name}.csv"
    $outDir = Join-Path $tmpDir "out_${name}"
    New-Item -ItemType Directory -Path $outDir | Out-Null
    $csvContent | Out-File -FilePath $inFile -Encoding utf8

    Write-Host "\n=== Scenario: $name ==="
    $runArgs = @('-File', $summ, '-in', (Resolve-Path $inFile).Path, '-outDir', (Resolve-Path $outDir).Path, '-bestTable')
    $runArgs += $summArgs
    # run the summarizer
    powershell -NoProfile -ExecutionPolicy Bypass @runArgs | Out-Null

    $bestTable = Join-Path $outDir 'best_table.csv'
    if (-not (Test-Path $bestTable)) { Fail ("{0}: best_table.csv not created" -f $name) }
    $content = Get-Content $bestTable -Raw
    & $assertAction $content
    Pass "$name"
}

# Scenario 1: all meet threshold -> choose minimal ef (ef wins)
$csv1 = @"
m,ef,recall@1,avgQueryMs,buildMs,memoryBytes
8,50,0.9700,0.1,200,1000
8,100,0.9800,0.2,180,2000
"@
RunScenario 'all-meet' $csv1 @('-recallK', '1', '-recallThreshold', '0.95') {
    param($c)
    if ($c -notmatch '8,50,0.9700') { Fail 'all-meet: expected ef=50 chosen' }
}

# Scenario 2: none meet threshold, no fallback -> placeholder line
$csv2 = @"
m,ef,recall@1,avgQueryMs,buildMs,memoryBytes
16,50,0.9000,0.2,200,1100
16,100,0.9200,0.25,190,2100
"@
RunScenario 'none-meet-no-fallback' $csv2 @('-recallK','1','-recallThreshold','0.95') {
    param($c)
    if ($c -notmatch 'no ef met threshold') { Fail 'none-meet-no-fallback: expected placeholder message' }
}

# Scenario 3: none meet threshold, with -bestByMaxRecall -> pick max recall and annotate fallback
$csv3 = $csv2
RunScenario 'none-meet-with-fallback' $csv3 @('-recallK','1','-recallThreshold','0.95','-bestByMaxRecall') {
    param($c)
    if ($c -notmatch '16,100') { Fail 'none-meet-with-fallback: expected ef=100 (max recall) selected' }
    if ($c -notmatch '\(max-recall fallback\)') { Fail 'none-meet-with-fallback: expected fallback annotation' }
}

# Scenario 4: tie-breaker buildMs (choose lower buildMs even if ef is higher)
$csv4 = @"
m,ef,recall@1,avgQueryMs,buildMs,memoryBytes
32,50,0.9600,0.2,300,1200
32,100,0.9600,0.25,150,3000
"@
RunScenario 'tie-break-buildMs' $csv4 @('-recallK','1','-recallThreshold','0.95','-bestByMaxRecall','-tieBreaker','buildMs') {
    param($c)
    # buildMs tie-breaker should prefer ef=100 (buildMs=150 < 300)
    if ($c -notmatch '32,100') { Fail 'tie-break-buildMs: expected ef=100 selected by buildMs tie-breaker' }
}

Write-Host "\nAll smoke tests passed." -ForegroundColor Green
exit 0
