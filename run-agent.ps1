$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'Java was not found. Install JDK 21 and ensure java.exe is on PATH.'
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw 'Maven was not found. Install Maven 3.9+ and ensure mvn.exe is on PATH.'
}

mvn spring-boot:run "-Dspring-boot.run.profiles=local"
