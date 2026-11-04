# Build MokoFile with JDK 17 and copy the APK to ..\builds (nichevo\builds).
param(
    [ValidateSet('debug', 'release')]
    [string]$Type = 'debug'
)

$ErrorActionPreference = 'Stop'

$jdk = 'C:\android-sdk\jdk17'
if (Test-Path $jdk) {
    $env:JAVA_HOME = $jdk
} elseif (-not $env:JAVA_HOME) {
    Write-Error 'JAVA_HOME is not set. Install JDK 17 and try again.'
}

$task = if ($Type -eq 'release') { 'exportReleaseApk' } else { 'exportDebugApk' }
Push-Location $PSScriptRoot
try {
    & (Join-Path $PSScriptRoot 'gradlew.bat') $task --console=plain
    $code = $LASTEXITCODE
} finally {
    Pop-Location
}

if ($code -ne 0) {
    exit $code
}

$builds = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\builds'))
Write-Host "Done. APK is in $builds"
