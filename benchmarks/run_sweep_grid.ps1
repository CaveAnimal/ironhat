# Parallel sweep runner for SweepBench
#
# Usage:
#   .\run_sweep_grid.ps1 -mList "8,16,32" -efList "50,100,200" -N 5000 -DIM 64 -queries 200 -K 1 -out results.csv
#
# Optional summarization flags (forwarded to summarize_sweep.ps1 when -AutoSummarize is used):
#   -AutoSummarize            : After aggregation, invoke the summarizer to produce summary.csv, summary.json, and best_table.csv
#   -bestByMaxRecall          : If no ef meets the -recallThreshold for a given m, pick the ef with the maximum recall (fallback)
#   -tieBreaker <ef|buildMs|memoryBytes> : When multiple ef values meet the threshold, break ties using the chosen metric (default: ef)

param(
    [string]$mList = "8,16,32",
    [string]$efList = "50,100,200",
    [int]$N = 5000,
    [int]$DIM = 64,
    [int]$queries = 200,
    [int]$K = 1,
    [string]$out = "sweep_$(Get-Date -Format yyyyMMdd_HHmmss).csv",
    [int]$parallel = 3
    ,
    [int]$jobTimeoutSeconds = 600
)
[switch]$NoRun = $false
[switch]$AutoSummarize = $false
[double]$recallThreshold = 0.95
[int]$recallK = 1
[switch]$bestByMaxRecall = $false
[string]$tieBreaker = 'ef'

# Prepare grid
$mArr = $mList -split ',' | ForEach-Object { $_.Trim() }
$efArr = $efList -split ',' | ForEach-Object { $_.Trim() }

$tempDir = Join-Path $PSScriptRoot "sweep_tmp"
if (-Not (Test-Path $tempDir)) { New-Item -ItemType Directory -Path $tempDir | Out-Null }

$jobs = @()
foreach ($m in $mArr) {
    foreach ($ef in $efArr) {
        $file = Join-Path $tempDir "sweep_m${m}_ef${ef}.csv"
        $jarPath = Join-Path $PSScriptRoot 'target\benchmarks-1.0.0-SNAPSHOT-shaded.jar'
        $argList = @(
            '-cp', $jarPath,
            'com.codetalker.benchmarks.SweepBench',
            '--n', $N.ToString(), '--dim', $DIM.ToString(), '--queries', $queries.ToString(),
            '--mList', $m.ToString(), '--efList', $ef.ToString(), '--k', $K.ToString()
        )
        $jobs += @{ m=$m; ef=$ef; argList=$argList; file=$file }
    }
}

# Run jobs in parallel batches
$running = @()
# flag to print the expected-wait message once
$expectedWaitPrinted = $false

# helper to print bold uppercase messages (uses ANSI bold if available)
function Print-ExpectedWait([string]$msg) {
    $bold = "`e[1m"
    $reset = "`e[0m"
    $out = ($msg.ToUpper())
    try {
        Write-Host "$bold$out$reset"
    } catch {
        # fallback to plain uppercase if ANSI not supported
        Write-Host $out
    }
}

# compute and print an upfront expected-wait message for long runs
if (-not $expectedWaitPrinted -and $jobs.Count -gt 0) {
    $perJobMin = [math]::Ceiling($jobTimeoutSeconds / 60)
    $batches = [math]::Ceiling($jobs.Count / $parallel)
    $estMaxTotalMin = $perJobMin * $batches
    Print-ExpectedWait("EXPECTED WAIT: UP TO $perJobMin MINUTES PER JOB (TIMEOUT). ESTIMATED MAX TOTAL WAIT: $estMaxTotalMin MINUTES")
    $expectedWaitPrinted = $true
}
foreach ($job in $jobs) {
    if ($NoRun) { Write-Host "NoRun: skipping job m=$($job.m) ef=$($job.ef) -> $($job.file)"; continue }
    while ($running.Count -ge $parallel) {
        # monitor running processes for exit or timeout
        if (-not $expectedWaitPrinted) {
            $perJobMin = [math]::Ceiling($jobTimeoutSeconds / 60)
            Print-ExpectedWait("WAITING FOR JOBS TO FINISH. EXPECTED UP TO $perJobMin MINUTES PER JOB")
            $expectedWaitPrinted = $true
        }
        Start-Sleep -Seconds 1
        $now = Get-Date
        $newRunning = @()
        foreach ($entry in $running) {
            $proc = $entry.proc
            if ($proc.HasExited) {
                Write-Host "Job finished m=$($entry.m) ef=$($entry.ef)"
                continue
            }
            $age = ($now - $entry.start).TotalSeconds
            if ($age -gt $jobTimeoutSeconds) {
                Write-Host "Job timed out after $age sec: m=$($entry.m) ef=$($entry.ef). Killing process..."
                try { $proc.Kill() } catch { }
                continue
            }
            $newRunning += $entry
        }
        $running = $newRunning
    }
    Write-Host "Starting m=$($job.m) ef=$($job.ef) -> $($job.file)"
    # Start a background Java process directly and redirect its stdout and stderr to files
    $javaExe = 'java'
    $errFile = "$($job.file).err"
    try {
        $proc = Start-Process -FilePath $javaExe -ArgumentList $job.argList -RedirectStandardOutput $job.file -RedirectStandardError $errFile -WorkingDirectory $PSScriptRoot -PassThru
    } catch {
        Write-Host "Failed to start java for m=$($job.m) ef=$($job.ef): $_" -ForegroundColor Red
        # create an error marker file
        "ERROR: Failed to start java: $_" | Out-File -FilePath $errFile -Encoding utf8
        continue
    }
    $running += @{ proc = $proc; file = $job.file; m = $job.m; ef = $job.ef; start = (Get-Date) }
}

# Wait for remaining jobs (monitor processes and timeouts)
while ($running.Count -gt 0) {
    Start-Sleep -Seconds 1
    $now = Get-Date
    $newRunning = @()
    foreach ($entry in $running) {
        $proc = $entry.proc
        if ($proc.HasExited) {
            Write-Host "Job finished m=$($entry.m) ef=$($entry.ef)"
            continue
        }
        $age = ($now - $entry.start).TotalSeconds
        if ($age -gt $jobTimeoutSeconds) {
            Write-Host "Job timed out after $age sec: m=$($entry.m) ef=$($entry.ef). Killing process..."
            try { $proc.Kill() } catch { }
            continue
        }
        $newRunning += $entry
    }
    $running = $newRunning
}

# Aggregate CSVs robustly by parsing headers and normalizing rows to a union header.
# Collect headers first
$files = Get-ChildItem -Path $tempDir -Filter 'sweep_m*_ef*.csv'
if ($files.Count -eq 0) { Write-Host "No sweep output files found in $tempDir"; exit 1 }

$allColumns = [System.Collections.Generic.List[string]]::new()
$fileColumns = @{}
$fileRows = @{}

# Use ConvertFrom-Csv to parse CSV content robustly (skip comment lines starting with '#')
foreach ($f in $files) {
    $raw = Get-Content -Path $f.FullName -Raw -ErrorAction SilentlyContinue
    if (-not $raw) { $fileColumns[$f.FullName] = @(); continue }
    # filter out comment lines
    $nonComment = ($raw -split "\r?\n") | Where-Object { $_ -and -not ($_.StartsWith('#')) }
    if (-not $nonComment -or $nonComment.Count -eq 0) { $fileColumns[$f.FullName] = @(); continue }
    $csvText = ($nonComment -join "`n")
    try {
        $objs = $csvText | ConvertFrom-Csv -ErrorAction Stop
    } catch {
        # If ConvertFrom-Csv fails (e.g., single header-only line), try to extract header
        $headerLine = $nonComment[0]
        $cols = $headerLine -split ',' | ForEach-Object { $_.Trim() }
        $fileColumns[$f.FullName] = $cols
        foreach ($c in $cols) { if (-not ($allColumns -contains $c)) { $allColumns.Add($c) } }
        $fileRows[$f.FullName] = @()
        continue
    }
    # collect property names
    $cols = @()
    if ($objs -and $objs.Count -gt 0) { $cols = ($objs[0].psobject.Properties | ForEach-Object { $_.Name }) } else { $cols = $nonComment[0] -split ',' | ForEach-Object { $_.Trim() } }
    $fileColumns[$f.FullName] = $cols
    foreach ($c in $cols) { if (-not ($allColumns -contains $c)) { $allColumns.Add($c) } }
    $fileRows[$f.FullName] = $objs
}

$timestamp = Get-Date -Format u
# Write unified header (stable ordering: keep m,ef first if present)
$orderedCols = [System.Collections.Generic.List[string]]::new()
if ($allColumns -contains 'm') { $orderedCols.Add('m') }
if ($allColumns -contains 'ef') { $orderedCols.Add('ef') }
foreach ($c in $allColumns) { if ($c -ne 'm' -and $c -ne 'ef') { $orderedCols.Add($c) } }

# Write comment block including file->columns mapping
$commentLines = @()
$commentLines += "# Aggregated sweep results generated: $timestamp"
$commentLines += "# Source files and their columns:"
foreach ($kv in $fileColumns.GetEnumerator()) {
    $fname = Split-Path -Leaf $kv.Key
    $cols = $kv.Value -join ','
    if ([string]::IsNullOrWhiteSpace($cols)) { $cols = "(no columns detected)" }
    # file diagnostics
    $fi = Get-Item -LiteralPath $kv.Key -ErrorAction SilentlyContinue
    if ($fi) {
        $size = $fi.Length
        $lt = $fi.LastWriteTime.ToString('u')
    } else { $size = 0; $lt = '(missing)'}
    $errFile = "$($kv.Key).err"
    $errSnippet = ''
    if (Test-Path $errFile) {
        $errLines = Get-Content -Path $errFile -ErrorAction SilentlyContinue
        if ($errLines) { $errSnippet = ($errLines[0..[math]::Min(19, $errLines.Count-1)] -join ' | ') }
    }
    $commentLines += "#  $fname -> $cols (size=${size} bytes, lastWrite=$lt)"
    if ($errSnippet) { $commentLines += "#    ERR: $errSnippet" }
}
$commentLines | Out-File -FilePath $out -Encoding utf8
$orderedCols -join ',' | Out-File -FilePath $out -Append -Encoding utf8

# Now read each file, parse rows and write normalized rows
foreach ($f in $files) {
    $lines = Get-Content -Path $f.FullName
    $rows = $lines | Where-Object { $_ -and -not ($_.StartsWith('#')) }
    if ($rows.Count -eq 0) { continue }
    # skip header (rows[0] is the header)
    $cols = $fileColumns[$f.FullName]
    $colIndex = @{}
    for ($i = 0; $i -lt $cols.Count; $i++) { $colIndex[$cols[$i]] = $i }

    # Use ConvertFrom-Csv parsed objects if available for this file
    if ($fileRows.ContainsKey($f.FullName) -and $fileRows[$f.FullName]) {
        $objs = $fileRows[$f.FullName]
        foreach ($o in $objs) {
            $outParts = @()
            foreach ($c in $orderedCols) {
                if ($o.psobject.Properties.Match($c)) { $outParts += ($o.$c) } else { $outParts += '' }
            }
            ($outParts -join ',') | Out-File -FilePath $out -Append -Encoding utf8
        }
    } else {
        # fallback to manual parse of rows
        for ($ri = 1; $ri -lt $rows.Count; $ri++) {
            $line = $rows[$ri]
            if ($line -match '^[mM],ef') { continue }
            $parts = $line -split ','
            # build normalized row matching $orderedCols
            $outParts = @()
            foreach ($c in $orderedCols) {
                if ($colIndex.ContainsKey($c)) {
                    $idx = $colIndex[$c]
                    if ($idx -lt $parts.Count) { $outParts += $parts[$idx] } else { $outParts += '' }
                } else {
                    $outParts += ''
                }
            }
            ($outParts -join ',') | Out-File -FilePath $out -Append -Encoding utf8
        }
    }
}

Write-Host "Aggregated results written to $out"

# If requested, call summarizer to produce summary.csv, summary.json, summary.csv.gz, and best_summary.csv
if ($AutoSummarize) {
    $summarizer = Join-Path $PSScriptRoot 'summarize_sweep.ps1'
    if (Test-Path $summarizer) {
        Write-Host "AutoSummarize requested: running summarizer..."
    $summArgs = @('-NoProfile','-ExecutionPolicy','Bypass','-File', $summarizer, '-in', (Resolve-Path $out).Path, '-outDir', $PSScriptRoot, '-gz', '-json', '-recallK', $recallK.ToString(), '-recallThreshold', $recallThreshold.ToString())
    if ($bestByMaxRecall) { $summArgs += '-bestByMaxRecall' }
    if ($tieBreaker) { $summArgs += '-tieBreaker'; $summArgs += $tieBreaker }
    # Run summarizer in a new PowerShell process so it doesn't interfere with current state
    Start-Process -FilePath 'powershell' -ArgumentList $summArgs -NoNewWindow -Wait
        Write-Host "Summarizer finished. Check summary files in $PSScriptRoot"
    } else {
        Write-Host "AutoSummarize requested, but summarizer not found at $summarizer" -ForegroundColor Yellow
    }
}
