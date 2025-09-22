<#
Creates a file-based H2 database at `./data/ironhat-db` and executes `scripts/seed.sql`.

This script uses Maven to build the project and generate a classpath, then runs
the H2 `RunScript` tool via `java -cp` so no extra downloads are needed.

Run from repository root in PowerShell:

    .\scripts\create_h2_seed.ps1

#>

param(
    [string]$DbPath = "./data/ironhat-db",
    [string]$SqlFile = "scripts/seed.sql"
)

Write-Host "Building project (to ensure dependencies are available)..."
mvn -DskipTests=true package

Write-Host "Building classpath via Maven dependency plugin..."
$cp = & mvn -q -Dexec.executable=echo -Dexec.args="%classpath" org.codehaus.mojo:exec-maven-plugin:3.1.0:exec

if (-not $cp) {
    Write-Host "Failed to get classpath via Maven. Falling back to local target/classes and lib jars."
    $cp = Get-ChildItem -Recurse -Filter "*.jar" -Path target | ForEach-Object { $_.FullName } | Select-Object -Join ";"
}

$javaCmd = "java -cp `"$cp`" org.h2.tools.RunScript -url `"jdbc:h2:$DbPath`" -user sa -script $SqlFile -showResults"
Write-Host "Running: $javaCmd"
cmd /c $javaCmd

Write-Host "H2 seed script completed. DB file location: $DbPath.mv.db (H2 uses .mv.db suffix)"
