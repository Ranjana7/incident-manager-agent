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
    throw 'Local Maven was not found. Download Maven 3.9.9 into tools or rerun the Maven setup step.'
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
powershell -NoProfile -ExecutionPolicy Bypass -Command "$s=(New-Object -ComObject WScript.Shell).CreateShortcut([Environment]::GetFolderPath('Desktop') + '\Incident Manager Agent.lnk'); $s.TargetPath=$env:LOCALAPPDATA + '\IncidentManagerAgent\IncidentManagerAgent.exe'; $s.WorkingDirectory=$env:LOCALAPPDATA + '\IncidentManagerAgent'; $s.Save()"
echo Installed Incident Manager Agent to "%DEST%".
echo Use the desktop shortcut or run "%DEST%\IncidentManagerAgent.exe".
pause
'@ | Set-Content -LiteralPath $portableInstaller -Encoding ASCII
    Compress-Archive -Path $appImage,$portableInstaller -DestinationPath $zipOutput -Force
}

Write-Host "Package output written to $dist"
