param([string]$ApkPath = '')
& (Join-Path $PSScriptRoot 'verify-apk.ps1') -ApkPath $ApkPath
exit $LASTEXITCODE
