$p='.\src\main\resources\models\temp-download'
if (-not (Test-Path $p)) { Write-Host 'File not found: ' $p; exit 1 }
$bytes = Get-Content -Encoding Byte -TotalCount 16 -Path $p
$hex = $bytes | ForEach-Object { '{0:X2}' -f $_ }
Write-Host 'Header:' ($hex -join ' ')
Write-Host 'Length:' (Get-Item $p).Length
Write-Host "\nFirst 400 characters (as text preview):"
Get-Content -Path $p -Raw | ForEach-Object { $_.Substring(0,[Math]::Min(400,$_.Length)) }
