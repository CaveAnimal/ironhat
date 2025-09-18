<#
summarize_sweep.ps1

Reads an aggregated sweep CSV (the one produced by `run_sweep_grid.ps1`) and writes a sorted CSV and JSON summary.
Usage:
  powershell -NoProfile -ExecutionPolicy Bypass -File .\benchmarks\summarize_sweep.ps1 -in ..\live_sweep.csv -outDir .\benchmarks -gz

Options:
  -in      Path to aggregated CSV (default: live_sweep.csv in repo root)
  -outDir  Directory for outputs (default: ./benchmarks)
  -gz      Also write a gzipped CSV (`summary.csv.gz`)
  -json    Also write JSON summary (`summary.json`)
#>
param(
    [string]$in = "E:/MyProjects/MyGitHubCopilot/ironhat/FeH-001/live_sweep.csv",
    [string]$outDir = "E:/MyProjects/MyGitHubCopilot/ironhat/FeH-001/benchmarks",
    [switch]$gz,
    [switch]$json,
    [int]$recallK = 1,
    [double]$recallThreshold = 0.95,
    [switch]$bestTable,
    [ValidateSet('ef','buildMs','memoryBytes')][string]$tieBreaker = 'ef',
    [switch]$bestByMaxRecall
)

if (-not (Test-Path $in)) { Write-Error "Input file not found: $in"; exit 1 }
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir | Out-Null }

# Read file, skip comment lines
$lines = Get-Content -Path $in -ErrorAction Stop
$nonComment = $lines | Where-Object { $_ -and -not ($_.StartsWith('#')) }
if ($nonComment.Count -lt 2) { Write-Error "No data rows found in $in"; exit 1 }

$header = $nonComment[0]
$cols = $header -split ',' | ForEach-Object { $_.Trim() }
$rows = @()
for ($i = 1; $i -lt $nonComment.Count; $i++) {
    $line = $nonComment[$i]
    if ([string]::IsNullOrWhiteSpace($line)) { continue }
    $parts = $line -split ','
    $obj = @{}
    for ($j=0; $j -lt $cols.Count; $j++) {
        $name = $cols[$j]
        $value = if ($j -lt $parts.Count) { $parts[$j] } else { '' }
        $obj[$name] = $value
    }
    $rows += (New-Object psobject -Property $obj)
}

# Convert m and ef to numeric for sorting
foreach ($r in $rows) {
    $r.m = [int]($r.m)
    $r.ef = [int]($r.ef)
}

$sorted = $rows | Sort-Object -Property @{Expression={$_.m};Descending=$false}, @{Expression={$_.ef};Descending=$false}

$outCsv = Join-Path $outDir 'summary.csv'
# write header
$cols -join ',' | Out-File -FilePath $outCsv -Encoding utf8
foreach ($r in $sorted) {
    $values = @()
    foreach ($c in $cols) { $values += ($r.$c) }
    ($values -join ',') | Out-File -FilePath $outCsv -Append -Encoding utf8
}
Write-Host "Wrote $outCsv"

if ($json) {
    $outJson = Join-Path $outDir 'summary.json'
    $sorted | ConvertTo-Json -Depth 4 | Out-File -FilePath $outJson -Encoding utf8
    Write-Host "Wrote $outJson"
}

if ($gz) {
    $outGz = Join-Path $outDir 'summary.csv.gz'
    # Create gzipped CSV using .NET streams
    $ms = New-Object System.IO.MemoryStream
    $sw = New-Object System.IO.StreamWriter($ms, [System.Text.Encoding]::UTF8)
    $linesToWrite = Get-Content -Path $outCsv
    foreach ($l in $linesToWrite) { $sw.WriteLine($l) }
    $sw.Flush()
    $ms.Position = 0
    $fs = [System.IO.File]::OpenWrite($outGz)
    $gzStream = New-Object System.IO.Compression.GzipStream($fs, [System.IO.Compression.CompressionMode]::Compress)
    $ms.CopyTo($gzStream)
    $gzStream.Close(); $fs.Close(); $ms.Close()
    Write-Host "Wrote $outGz"
}

# Produce a "best summary" table: for each m, select the minimal ef whose recall@<recallK> >= recallThreshold
try {
    $recallCol = "recall@${recallK}"
    if ($cols -notcontains $recallCol) {
        Write-Host "Recall column $recallCol not found in data; skipping best-summary generation." -ForegroundColor Yellow
    } else {
        $grouped = @{}
        foreach ($r in $sorted) {
            $mkey = $r.m.ToString()
            if (-not $grouped.ContainsKey($mkey)) { $grouped[$mkey] = @() }
            $grouped[$mkey] += $r
        }
        $bestRows = @()
        foreach ($k in $grouped.Keys | Sort-Object {[int]$_}) {
            $allCandidates = $grouped[$k]
            # filter candidates meeting recall threshold
            $candidatesMeeting = @()
            foreach ($cand in $allCandidates) {
                $val = 0.0
                try { $val = [double]$cand.$recallCol } catch { $val = 0.0 }
                if ($val -ge $recallThreshold) { $candidatesMeeting += $cand }
            }
            if ($candidatesMeeting.Count -eq 0) {
                if ($bestByMaxRecall) {
                    # select candidate(s) with maximum recall
                    $maxVal = -1.0
                    foreach ($cand in $allCandidates) {
                        $v = 0.0
                        try { $v = [double]$cand.$recallCol } catch { $v = 0.0 }
                        if ($v -gt $maxVal) { $maxVal = $v }
                    }
                    if ($maxVal -ge 0) {
                        $candidatesMeeting = @()
                        foreach ($cand in $allCandidates) {
                            try { $v = [double]$cand.$recallCol } catch { $v = 0.0 }
                            if ($v -eq $maxVal) { $candidatesMeeting += $cand }
                        }
                        # mark that we used the fallback for this m
                        $usedFallbackForGroup = $true
                    } else { continue }
                } else { continue }
            } else {
                $usedFallbackForGroup = $false
            }
            # Order by tieBreaker preference
            switch ($tieBreaker) {
                'ef' { $ordered = $candidatesMeeting | Sort-Object -Property @{Expression={[int]$_.ef}}, @{Expression={ try { [double]$_.buildMs } catch { [double]::MaxValue } }} }
                'buildMs' { $ordered = $candidatesMeeting | Sort-Object -Property @{Expression={ try { [double]$_.buildMs } catch { [double]::MaxValue } }}, @{Expression={[int]$_.ef}} }
                'memoryBytes' { $ordered = $candidatesMeeting | Sort-Object -Property @{Expression={ try { [double]$_.memoryBytes } catch { [double]::MaxValue } }}, @{Expression={[int]$_.ef}} }
                default { $ordered = $candidatesMeeting | Sort-Object -Property @{Expression={[int]$_.ef}} }
            }
            $chosen = $ordered[0]
            if ($null -ne $chosen) { $bestRows += $chosen }
        }
        if ($bestRows.Count -gt 0) {
            $outBest = Join-Path $outDir 'best_summary.csv'
            $cols -join ',' | Out-File -FilePath $outBest -Encoding utf8
            foreach ($r in $bestRows) {
                $values = @(); foreach ($c in $cols) { $values += ($r.$c) }
                ($values -join ',') | Out-File -FilePath $outBest -Append -Encoding utf8
            }
            Write-Host "Wrote best-summary to $outBest"
        } else {
            Write-Host "No (m,ef) pair met recall >= $recallThreshold for recall@${recallK}" -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "Error while generating best-summary: $_" -ForegroundColor Red
}

# If requested, also write a condensed best_table.csv with a short comment per m
if ($bestTable) {
    $outTable = Join-Path $outDir 'best_table.csv'
    $tableHeader = 'm,ef,recallUsed,comment,avgQueryMs,buildMs,memoryBytes'
    $tableHeader | Out-File -FilePath $outTable -Encoding utf8
    # For each m, either write the chosen row or a placeholder
    $allM = $grouped.Keys | ForEach-Object { [int]$_ } | Sort-Object
    foreach ($mval in $allM) {
        $mkey = $mval.ToString()
        $allCandidates = $grouped[$mkey]
        $candidatesMeeting = @()
        foreach ($cand in $allCandidates) {
            $val = 0.0
            try { $val = [double]$cand.$recallCol } catch { $val = 0.0 }
            if ($val -ge $recallThreshold) { $candidatesMeeting += $cand }
        }
        $usedFallback = $false
        if ($candidatesMeeting.Count -eq 0 -and $bestByMaxRecall) {
            # pick max recall candidates as fallback
            $maxVal = -1.0
            foreach ($cand in $allCandidates) {
                try { $v = [double]$cand.$recallCol } catch { $v = 0.0 }
                if ($v -gt $maxVal) { $maxVal = $v }
            }
            if ($maxVal -ge 0) {
                foreach ($cand in $allCandidates) {
                    try { $v = [double]$cand.$recallCol } catch { $v = 0.0 }
                    if ($v -eq $maxVal) { $candidatesMeeting += $cand }
                }
                $usedFallback = $true
            }
        }
        $chosen = $null
        if ($candidatesMeeting.Count -gt 0) {
            switch ($tieBreaker) {
                'ef' { $ordered = $candidatesMeeting | Sort-Object -Property @{Expression={[int]$_.ef}}, @{Expression={ try { [double]$_.buildMs } catch { [double]::MaxValue } }} }
                'buildMs' { $ordered = $candidatesMeeting | Sort-Object -Property @{Expression={ try { [double]$_.buildMs } catch { [double]::MaxValue } }}, @{Expression={[int]$_.ef}} }
                'memoryBytes' { $ordered = $candidatesMeeting | Sort-Object -Property @{Expression={ try { [double]$_.memoryBytes } catch { [double]::MaxValue } }}, @{Expression={[int]$_.ef}} }
                default { $ordered = $candidatesMeeting | Sort-Object -Property @{Expression={[int]$_.ef}} }
            }
            $chosen = $ordered[0]
        }
        if ($null -ne $chosen) {
            $recVal = 0.0
            try { $recVal = [double]$chosen.$recallCol } catch { $recVal = 0.0 }
            $recUsed = "{0:N4}" -f ($recVal)
            $comment = "recall=$recUsed (threshold={0})" -f $recallThreshold
            if ($usedFallback) { $comment += ' (max-recall fallback)' }
            $avgQ = if ($chosen.PSObject.Properties.Match('avgQueryMs')) { $chosen.avgQueryMs } else { '' }
            $build = if ($chosen.PSObject.Properties.Match('buildMs')) { $chosen.buildMs } else { '' }
            $mem = if ($chosen.PSObject.Properties.Match('memoryBytes')) { $chosen.memoryBytes } else { '' }
            "$($mval),$($chosen.ef),$recUsed,$comment,$avgQ,$build,$mem" | Out-File -FilePath $outTable -Append -Encoding utf8
        } else {
            # no ef met threshold for this m
            "$($mval),,,$('no ef met threshold (threshold=' + $recallThreshold + ')'),,," | Out-File -FilePath $outTable -Append -Encoding utf8
        }
    }
    Write-Host "Wrote condensed best table to $outTable"
}
