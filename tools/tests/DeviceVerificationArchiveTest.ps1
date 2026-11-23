$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$helper = Join-Path $root 'tools/VerificationArchive.ps1'
$testRunId = [Guid]::NewGuid().ToString('N')
$testRoot = Join-Path $root ".local/tests/verification-archive-test-$testRunId"

function Assert-Equal([string]$Expected, [string]$Actual, [string]$Description) {
    if ($Expected -ne $Actual) {
        throw "$Description expected '$Expected', got '$Actual'."
    }
}

try {
    [void][IO.Directory]::CreateDirectory($testRoot)
    if (!(Test-Path -LiteralPath $helper -PathType Leaf)) {
        throw "Device verification archive helper is missing: $helper"
    }
    . $helper

    $runId = '0123456789abcdef0123456789abcdef'
    $paths = New-DeviceVerificationPaths `
        -Root $testRoot `
        -Version '3.0.5' `
        -RunId $runId `
        -Timestamp '20261119-130000'
    Assert-Equal `
        (Join-Path $testRoot "delivery/verification/3.0.5/device-ui-20261119-130000-$runId") `
        $paths.EvidenceDirectory `
        'Versioned evidence directory'
    Assert-Equal `
        (Join-Path $testRoot ".local/tests/verify-device-$runId") `
        $paths.ScratchDirectory `
        'Temporary device-test directory'
    Assert-Equal `
        (Join-Path $paths.EvidenceDirectory 'verification.log') `
        $paths.TranscriptPath `
        'Retained verification transcript'

    [void][IO.Directory]::CreateDirectory($paths.ScratchDirectory)
    [IO.File]::WriteAllText((Join-Path $paths.ScratchDirectory 'window.xml'), '<hierarchy />')
    Remove-DeviceVerificationScratch `
        -Root $testRoot `
        -ScratchDirectory $paths.ScratchDirectory `
        -RunId $runId
    if (Test-Path -LiteralPath $paths.ScratchDirectory) {
        throw 'Device verification scratch directory was not removed.'
    }

    $outside = Join-Path $testRoot '.local/tests/keep-this-directory'
    [void][IO.Directory]::CreateDirectory($outside)
    try {
        Remove-DeviceVerificationScratch `
            -Root $testRoot `
            -ScratchDirectory $outside `
            -RunId $runId
        throw 'Unsafe scratch directory was accepted.'
    } catch {
        if ($_.Exception.Message -eq 'Unsafe scratch directory was accepted.') { throw }
    }
    if (!(Test-Path -LiteralPath $outside -PathType Container)) {
        throw 'Refused scratch directory was unexpectedly removed.'
    }

    Write-Output 'DeviceVerificationArchiveTest passed'
} finally {
    $resolvedTestRoot = [IO.Path]::GetFullPath($testRoot)
    $expectedParent = [IO.Path]::GetFullPath((Join-Path $root '.local/tests')).TrimEnd('\')
    if ([IO.Path]::GetDirectoryName($resolvedTestRoot).TrimEnd('\') -ne $expectedParent -or
        [IO.Path]::GetFileName($resolvedTestRoot) -ne "verification-archive-test-$testRunId") {
        throw "Refusing to clean an unexpected test directory: $resolvedTestRoot"
    }
    if (Test-Path -LiteralPath $resolvedTestRoot) {
        Remove-Item -LiteralPath $resolvedTestRoot -Recurse -Force
    }
    if (Test-Path -LiteralPath $resolvedTestRoot) { throw "Tool self-test temporary directory remains" }
}
