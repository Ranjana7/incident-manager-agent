$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$uiRoot = Join-Path $projectRoot 'ui'

Set-Location $uiRoot
& 'C:\Program Files\nodejs\npm.cmd' install --no-audit --no-fund --loglevel=error
& 'C:\Program Files\nodejs\npm.cmd' run build
