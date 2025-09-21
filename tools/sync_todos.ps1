#!/usr/bin/env pwsh
<#
Runs the Python-based todo sync helper to regenerate `ironhat_TODO.md` from the assistant-managed snapshot.

Usage (PowerShell):
  .\tools\sync_todos.ps1

This script will invoke Python from PATH. If you prefer a specific interpreter, set the $Python variable accordingly.
#>

$ErrorActionPreference = 'Stop'

# Allow overriding Python executable via environment variable
$Python = $env:PYTHON_EXE
if (-not $Python) {
    $Python = 'python'
}

Write-Host "Running todo sync using: $Python tools/sync_todos.py"

try {
    & $Python "tools/sync_todos.py"
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Todo sync completed successfully. Updated ironhat_TODO.md"
        exit 0
    } else {
        Write-Error "sync_todos.py exited with code $LASTEXITCODE"
        exit $LASTEXITCODE
    }
} catch {
    Write-Error "Failed to run sync_todos.py: $_"
    exit 1
}
