<#
PowerShell helper: run_integration.ps1
Usage:
  .\run_integration.ps1 -ModelPath <path-to-tflite-or-savedmodel>

This script attempts to run `mvn -Pwith-tensorflow -Dcodetalker.model.path=... verify` in a PowerShell-safe way.
It first tries the PowerShell-friendly approach (set an env var and pass it with double quotes). If mvn fails with an argument parsing error,
it falls back to invoking `cmd /c` with PowerShell stop-parsing `--%` so the `-D` argument is forwarded literally.
#>
param(
    [string]$ModelPath
)

function Write-Info($msg) { Write-Host "[info] $msg" -ForegroundColor Cyan }
function Write-Err($msg) { Write-Host "[error] $msg" -ForegroundColor Red }

Set-Location -LiteralPath (Split-Path -Path $MyInvocation.MyCommand.Definition -Parent | Split-Path -Parent)

if ([string]::IsNullOrEmpty($ModelPath)) {
    # Default model path used in repo
    $ModelPath = Join-Path -Path (Get-Location) -ChildPath 'src\main\resources\models\universal-sentence-encoder-lite.tflite'
}

Write-Info "Using model path: $ModelPath"

# Try PowerShell-friendly invocation first
$env:CODETALKER_MODEL_PATH = $ModelPath
Write-Info 'Attempting PowerShell mvn invocation...'

# Clean up previous file-based H2 test databases to avoid unique constraint collisions
Write-Info "Cleaning up ./test-data* directories to ensure fresh H2 databases..."
Get-ChildItem -Path . -Filter "test-data*" -Directory -ErrorAction SilentlyContinue | ForEach-Object {
    try {
        Remove-Item -LiteralPath $_.FullName -Recurse -Force -ErrorAction Stop
        Write-Info "Removed: $($_.FullName)"
    } catch {
        Write-Err "Failed to remove $($_.FullName): $($_.Exception.Message)"
    }
}

Write-Info "Running: mvn -Pwith-tensorflow -Dcodetalker.model.path=`"$env:CODETALKER_MODEL_PATH`" verify"

try {
    & mvn -Pwith-tensorflow -Dcodetalker.model.path="$env:CODETALKER_MODEL_PATH" verify
    $exitCode = $LASTEXITCODE
} catch {
    Write-Err "PowerShell mvn invocation failed with exception: $_"
    $exitCode = 1
}

if ($exitCode -eq 0) {
    Write-Info "Maven integration succeeded (PowerShell invocation)."
    exit 0
}

Write-Info "PowerShell invocation failed (exit code $exitCode). Falling back to cmd /c..."

# Fallback: call cmd.exe directly and pass a quoted mvn command string so cmd handles quoting (no PowerShell stop-parsing)
$escapedPath = $ModelPath -replace '"', '""'
$cmd = 'mvn -Pwith-tensorflow -Dcodetalker.model.path="' + $escapedPath + '" verify'
Write-Info "Running fallback: cmd /c $cmd"

$proc = Start-Process -FilePath cmd.exe -ArgumentList @('/c', $cmd) -NoNewWindow -Wait -PassThru

if ($proc.ExitCode -eq 0) {
    Write-Info "Maven integration succeeded (cmd fallback)."
    exit 0
} else {
    Write-Err "Maven integration failed (cmd fallback) with exit code $($proc.ExitCode)."
    exit $proc.ExitCode
}
