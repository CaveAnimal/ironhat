param(
    [string]$Url = 'https://tfhub.dev/google/lite-model/universal-sentence-encoder-lite/1?tf-hub-format=compressed',
    [string]$DestDir = 'src/main/resources/models'
)

if (-not (Test-Path $DestDir)) { New-Item -ItemType Directory -Path $DestDir -Force | Out-Null }
$temp = Join-Path $DestDir 'temp-download'

Write-Host "Downloading from $Url to $temp..."
# Use native curl.exe to follow redirects and write binary data
$curl = Get-Command curl.exe -ErrorAction SilentlyContinue
if ($null -eq $curl) {
    Write-Error "curl.exe not found in PATH. Please install curl or use a machine with curl available."
    exit 2
}

# Download
& curl.exe -L $Url -o $temp
if (-not (Test-Path $temp)) { Write-Error "Download failed: no file created at $temp"; exit 3 }

# Read header bytes
$fs = [System.IO.File]::OpenRead($temp)
$hdr = New-Object byte[] 8
$fs.Read($hdr,0,8) | Out-Null
$fs.Close()
$hex = ($hdr | ForEach-Object { '{0:X2}' -f $_ }) -join ' '
Write-Host "Header bytes: $hex"

# Detect type
$signature = -join ($hdr | ForEach-Object { '{0:X2}' -f $_ })
if ($signature.StartsWith('504B0304')) {
    Write-Host "Detected ZIP archive"
    try {
        Expand-Archive -LiteralPath $temp -DestinationPath $DestDir -Force
    } catch {
        Write-Warning "Expand-Archive failed: $_"
    }
} elseif ($signature.StartsWith('1F8B08')) {
    Write-Host "Detected GZIP (tar.gz)"
    try {
        # Use tar to extract
        & tar -xzf $temp -C $DestDir
    } catch {
        Write-Warning "tar extraction failed: $_"
    }
} else {
    # Check if it's HTML (starts with '<')
    $first = Get-Content -Path $temp -TotalCount 1 -Encoding UTF8
    if ($first -like '<*' -or $first -like '*html*') {
        Write-Warning "Downloaded content looks like HTML or a page, not an archive. Inspect scripts/tfhub_model_page.html or use manual provisioning."
    } else {
        Write-Host "Downloaded file doesn't look like a zip/tar/gzip or HTML. We'll try to detect .tflite inside as raw file."
        # If the file itself is .tflite (some providers send the tflite directly), move it
        # Try a simple magic check for TensorFlow Lite: no fixed magic, but size > 1000 bytes is a heuristic
        if ((Get-Item $temp).Length -gt 1000) {
            $target = Join-Path $DestDir 'universal-sentence-encoder-lite.tflite'
            Move-Item -Path $temp -Destination $target -Force
            Write-Host "Moved downloaded file to $target"
            exit 0
        }
    }
}

# Find any .tflite in the destdir
$tflites = Get-ChildItem -Path $DestDir -Recurse -Filter *.tflite -ErrorAction SilentlyContinue
if ($tflites -and $tflites.Count -gt 0) {
    foreach ($f in $tflites) {
        Write-Host "Found tflite: $($f.FullName)"
    }
    exit 0
}

Write-Warning "No .tflite found after extraction; please follow docs/MODEL_PROVISIONING.md for manual steps."
exit 4
