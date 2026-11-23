$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$helper = Join-Path $root 'tools/ApkPaths.ps1'
$runId = [Guid]::NewGuid().ToString('N')
$testRoot = Join-Path $root ".local/tests/apk-paths-$runId"
$environmentBefore = @{}
foreach ($key in @('TEMP', 'TMP')) {
    $environmentBefore[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
}

function Assert-Equal([string]$Expected, [string]$Actual, [string]$Description) {
    if ($Expected -cne $Actual) { throw "$Description expected '$Expected', got '$Actual'." }
}

try {
    [void][IO.Directory]::CreateDirectory($testRoot)
    $env:TEMP = $testRoot
    $env:TMP = $testRoot
    . $helper
    $versionFile = Join-Path $testRoot 'version.properties'
    [IO.File]::WriteAllText($versionFile, "major=3`nminor=0`npatch=6`nversionCode=70`n")

    Assert-Equal (Join-Path $testRoot 'app/build/outputs/apk/github/release/Circular-3.0.6.apk') `
        (Get-CircularApkPath -Root $testRoot) 'Default release filename'
    Assert-Equal (Join-Path $testRoot 'app/build/outputs/apk/github/debug/Circular-3.0.6-debug.apk') `
        (Get-CircularApkPath -Root $testRoot -Variant debug) 'Debug filename'
    Assert-Equal (Join-Path $testRoot 'app/build/outputs/apk/lspRepo/release/Circular-3.0.6-lsp-repo.apk') `
        (Get-CircularApkPath -Root $testRoot -Edition lspRepo) 'LSPosed repository release filename'
    Assert-Equal (Join-Path $testRoot 'app/build/outputs/apk/lspRepo/debug/Circular-3.0.6-lsp-repo-debug.apk') `
        (Get-CircularApkPath -Root $testRoot -Edition lspRepo -Variant debug) 'LSPosed repository Debug filename'

    [IO.File]::WriteAllText($versionFile, "major=3`nminor=1`npatch=0`nversionCode=71`n")
    $expectedRelease = Join-Path $testRoot 'app/build/outputs/apk/github/release/Circular-3.1.0.apk'
    Assert-Equal $expectedRelease (Get-CircularApkPath -Root $testRoot) 'Version bump changes the default filename'

    $legacyDirectory = Join-Path $testRoot 'app/build/outputs/apk/release'
    [void][IO.Directory]::CreateDirectory($legacyDirectory)
    [IO.File]::WriteAllText((Join-Path $legacyDirectory 'app-release.apk'), 'legacy package')
    Assert-Equal $expectedRelease (Get-CircularApkPath -Root $testRoot) 'Legacy package is never a fallback'
    if (Test-Path -LiteralPath $expectedRelease) { throw 'Unexpected fixture package.' }

    Remove-Item -LiteralPath $versionFile
    Assert-Equal (Join-Path $testRoot 'selected/custom.apk') `
        (Get-CircularApkPath -Root $testRoot -ApkPath 'selected/custom.apk') 'Relative override precedes version loading'
    $absolute = Join-Path $testRoot 'absolute.apk'
    Assert-Equal $absolute (Get-CircularApkPath -Root $testRoot -ApkPath $absolute) 'Absolute override stays absolute'

    [IO.File]::WriteAllText($versionFile, "major=3`nminor=0`nversionCode=70`n")
    $rejected = $false
    try { Get-CircularApkPath -Root $testRoot | Out-Null } catch { $rejected = $true }
    if (!$rejected) { throw 'An incomplete version must not select a malformed or legacy package.' }
    Write-Output 'ApkPathsTest passed'
} finally {
    foreach ($key in $environmentBefore.Keys) {
        if ($null -eq $environmentBefore[$key]) {
            Remove-Item -LiteralPath "Env:$key" -ErrorAction SilentlyContinue
        } else {
            [Environment]::SetEnvironmentVariable($key, $environmentBefore[$key], 'Process')
        }
    }
    $resolved = [IO.Path]::GetFullPath($testRoot)
    $expectedParent = [IO.Path]::GetFullPath((Join-Path $root '.local/tests')).TrimEnd('\')
    if ([IO.Path]::GetDirectoryName($resolved).TrimEnd('\') -ne $expectedParent -or
        [IO.Path]::GetFileName($resolved) -ne "apk-paths-$runId") {
        throw 'Refusing to clean an unexpected APK-path test directory.'
    }
    if (Test-Path -LiteralPath $resolved) { Remove-Item -LiteralPath $resolved -Recurse -Force }
    if (Test-Path -LiteralPath $resolved) { throw 'APK-path test directory remains.' }
}
