$ErrorActionPreference = 'Stop'

if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
    throw 'winget was not found. Install Java 21 and Maven manually, then rerun build-installer.ps1.'
}

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    winget install --id Microsoft.OpenJDK.21 --exact --accept-package-agreements --accept-source-agreements
}

if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    $javaHome = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'Machine')
    if ($javaHome) {
        $env:Path = "$javaHome\bin;$env:Path"
    }
}

$projectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$tools = Join-Path $projectRoot 'tools'
$mavenVersion = '3.9.9'
$mavenDir = Join-Path $tools "apache-maven-$mavenVersion"
$zip = Join-Path $tools "apache-maven-$mavenVersion-bin.zip"
New-Item -ItemType Directory -Path $tools -Force | Out-Null
if (-not (Test-Path -LiteralPath $mavenDir)) {
    Invoke-WebRequest -Uri "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip" -OutFile $zip
    Expand-Archive -LiteralPath $zip -DestinationPath $tools -Force
}

Write-Host 'Prerequisite installation completed. Open a new terminal if java or jpackage are still not on PATH.'
