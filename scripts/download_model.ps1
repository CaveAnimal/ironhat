# Download Universal Sentence Encoder Lite (TFLite) model from TensorFlow Hub
# Saves model to src/main/resources/models/

$destDir = "src/main/resources/models"
if (-not (Test-Path $destDir)) {
    New-Item -ItemType Directory -Path $destDir -Force | Out-Null
}

# TF Hub provides a zipped collection; for the lite model URL we can download the .tflite file directly if available.
# As a fallback this script downloads the tarball from TF Hub page and extracts the .tflite file.

$hubUrl = "https://tfhub.dev/google/lite-model/universal-sentence-encoder-lite/1?tf-hub-format=compressed"
$zipPath = Join-Path $destDir "use-lite.zip"

Write-Host "Downloading model from $hubUrl ..."
Invoke-WebRequest -Uri $hubUrl -OutFile $zipPath -UseBasicParsing

Write-Host "Extracting model..."
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::ExtractToDirectory($zipPath, $destDir)

# Find any .tflite files and move them to the model dir root
Get-ChildItem -Path $destDir -Recurse -Filter *.tflite | ForEach-Object {
    $target = Join-Path $destDir $_.Name
    Move-Item -Path $_.FullName -Destination $target -Force
}

Remove-Item $zipPath -Force
Write-Host "Model downloaded to $destDir"