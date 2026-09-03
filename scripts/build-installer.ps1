$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$dist = Join-Path $projectRoot 'dist'
$packageInput = Join-Path $projectRoot 'target\package-input'
$appName = 'IncidentManagerAgent'
$version = '0.1.0'
$javaHome = 'C:\Program Files\Microsoft\jdk-21.0.12.8-hotspot'
$mavenHome = Join-Path $projectRoot 'tools\apache-maven-3.9.9'
$maven = Join-Path $mavenHome 'bin\mvn.cmd'
$jpackage = Join-Path $javaHome 'bin\jpackage.exe'

Set-Location $projectRoot

if (-not (Test-Path -LiteralPath 'C:\Program Files\nodejs\npm.cmd')) {
    throw 'npm was not found. Install Node.js before building the dashboard.'
}
if (-not (Test-Path -LiteralPath $javaHome)) {
    throw 'JDK 21 was not found. Run scripts\install-prerequisites.ps1 or install JDK 21.'
}
if (-not (Test-Path -LiteralPath $maven)) {
    $tools = Join-Path $projectRoot 'tools'
    $zip = Join-Path $tools 'apache-maven-3.9.9-bin.zip'
    New-Item -ItemType Directory -Path $tools -Force | Out-Null
    Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip' -OutFile $zip
    Expand-Archive -LiteralPath $zip -DestinationPath $tools -Force
}
if (-not (Test-Path -LiteralPath $jpackage)) {
    throw 'jpackage was not found. Ensure JDK 21 is installed.'
}

$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$mavenHome\bin;$env:Path"

& (Join-Path $projectRoot 'scripts\build-ui.ps1')
Set-Location $projectRoot
& $maven clean package

if (Test-Path -LiteralPath $dist) { Remove-Item -LiteralPath $dist -Recurse -Force }
if (Test-Path -LiteralPath $packageInput) { Remove-Item -LiteralPath $packageInput -Recurse -Force }
New-Item -ItemType Directory -Path $dist | Out-Null
New-Item -ItemType Directory -Path $packageInput | Out-Null

Copy-Item -LiteralPath (Join-Path $projectRoot "target\incident-manager-agent-$version.jar") -Destination $packageInput
Copy-Item -LiteralPath (Join-Path $projectRoot 'config') -Destination $packageInput -Recurse
Copy-Item -LiteralPath (Join-Path $projectRoot 'runbooks') -Destination $packageInput -Recurse
Copy-Item -LiteralPath (Join-Path $projectRoot 'run-agent.cmd') -Destination $packageInput
Copy-Item -LiteralPath (Join-Path $projectRoot 'run-agent.ps1') -Destination $packageInput

$jpackageArgsBase = @(
    '--name', $appName,
    '--app-version', $version,
    '--vendor', 'Incident Manager',
    '--input', $packageInput,
    '--main-jar', "incident-manager-agent-$version.jar",
    '--dest', $dist,
    '--java-options', '-Dspring.profiles.active=local',
    '--java-options', '-Dlogging.file.name=${user.home}/IncidentManagerAgent/logs/agent.log'
)
$jpackageInstallerArgs = $jpackageArgsBase + @('--win-menu', '--win-shortcut', '--win-console')

try {
    & $jpackage @jpackageInstallerArgs '--type' 'exe'
} catch {
    Write-Warning 'EXE installer packaging failed, likely because WiX Toolset is not installed. Creating app image and ZIP fallback.'
}

$exe = Join-Path $dist "$appName-$version.exe"
if (-not (Test-Path -LiteralPath $exe)) {
    Write-Warning 'EXE installer was not produced. Creating app image and ZIP fallback.'
    $appImage = Join-Path $dist $appName
    $zipOutput = Join-Path $dist "$appName.zip"
    if (Test-Path -LiteralPath $appImage) { Remove-Item -LiteralPath $appImage -Recurse -Force }
    if (Test-Path -LiteralPath $zipOutput) { Remove-Item -LiteralPath $zipOutput -Force }
    & $jpackage @jpackageArgsBase '--type' 'app-image'

    $appLauncher = Join-Path $appImage 'Start-IncidentManagerAgent.cmd'
@'
@echo off
setlocal
set "ROOT=%~dp0"
set "AGENT_EXE=%ROOT%IncidentManagerAgent.exe"

if not exist "%AGENT_EXE%" (
  echo Incident Manager Agent executable was not found: "%AGENT_EXE%"
  pause
  exit /b 1
)

if not exist "%USERPROFILE%\IncidentManagerAgent\logs" mkdir "%USERPROFILE%\IncidentManagerAgent\logs"

cd /d "%ROOT%"
start "Incident Manager Agent" "%AGENT_EXE%"
timeout /t 8 /nobreak >nul
start "" "http://localhost:8080"
echo Incident Manager Agent is starting. If the dashboard did not open, browse to http://localhost:8080
'@ | Set-Content -LiteralPath $appLauncher -Encoding ASCII

    $runFromZip = Join-Path $dist 'Run-IncidentManagerAgent.cmd'
    @'
@echo off
setlocal
call "%~dp0IncidentManagerAgent\Start-IncidentManagerAgent.cmd"
'@ | Set-Content -LiteralPath $runFromZip -Encoding ASCII

    $readmeFirst = Join-Path $dist 'README-FIRST.txt'
    @'
Incident Manager Agent

No Java, Maven, Node.js, or developer tooling is required on the customer's machine.

Quick start:
1. Extract this ZIP.
2. Double-click Run-IncidentManagerAgent.cmd.
3. The dashboard opens at http://localhost:8080.
4. Configure mailbox, Teams, polling, and runbook settings in the dashboard.

Optional install:
Run Install-IncidentManagerAgent.cmd to copy the app to %LOCALAPPDATA%\IncidentManagerAgent and create a desktop shortcut.

Logs:
%USERPROFILE%\IncidentManagerAgent\logs\agent.log

Local data:
%USERPROFILE%\IncidentManagerAgent\incident-agent.db
%USERPROFILE%\IncidentManagerAgent\config\application-local.yml
%USERPROFILE%\IncidentManagerAgent\runbooks\
'@ | Set-Content -LiteralPath $readmeFirst -Encoding ASCII

    $portableInstaller = Join-Path $dist 'Install-IncidentManagerAgent.cmd'
    @'
@echo off
setlocal
set "SOURCE=%~dp0IncidentManagerAgent"
set "DEST=%LOCALAPPDATA%\IncidentManagerAgent"
if not exist "%SOURCE%\IncidentManagerAgent.exe" (
  echo Could not find IncidentManagerAgent app image next to this installer.
  exit /b 1
)
if exist "%DEST%" rmdir /s /q "%DEST%"
xcopy "%SOURCE%" "%DEST%\" /e /i /y >nul
powershell -NoProfile -ExecutionPolicy Bypass -Command "$s=(New-Object -ComObject WScript.Shell).CreateShortcut([Environment]::GetFolderPath('Desktop') + '\Incident Manager Agent.lnk'); $s.TargetPath=$env:LOCALAPPDATA + '\IncidentManagerAgent\Start-IncidentManagerAgent.cmd'; $s.WorkingDirectory=$env:LOCALAPPDATA + '\IncidentManagerAgent'; $s.Save()"
echo Installed Incident Manager Agent to "%DEST%".
echo Use the desktop shortcut or run "%DEST%\Start-IncidentManagerAgent.cmd".
call "%DEST%\Start-IncidentManagerAgent.cmd"
pause
'@ | Set-Content -LiteralPath $portableInstaller -Encoding ASCII
    Compress-Archive -Path $appImage,$portableInstaller,$runFromZip,$readmeFirst -DestinationPath $zipOutput -Force
}

Write-Host "Package output written to $dist"
