function New-DeviceVerificationPaths {
    param(
        [Parameter(Mandatory = $true)][string]$Root,
        [Parameter(Mandatory = $true)][ValidatePattern('^\d+\.\d+\.\d+$')][string]$Version,
        [Parameter(Mandatory = $true)][ValidatePattern('^[a-f0-9]{32}$')][string]$RunId,
        [Parameter(Mandatory = $true)][ValidatePattern('^\d{8}-\d{6}$')][string]$Timestamp
    )

    $projectRoot = [IO.Path]::GetFullPath($Root)
    $evidenceName = "device-ui-$Timestamp-$RunId"
    $evidenceDirectory = [IO.Path]::GetFullPath(
        (Join-Path $projectRoot "delivery/verification/$Version/$evidenceName"))
    $scratchDirectory = [IO.Path]::GetFullPath(
        (Join-Path $projectRoot ".local/tests/verify-device-$RunId"))

    return [pscustomobject]@{
        EvidenceDirectory = $evidenceDirectory
        ScratchDirectory = $scratchDirectory
        TranscriptPath = Join-Path $evidenceDirectory 'verification.log'
    }
}

function Remove-DeviceVerificationScratch {
    param(
        [Parameter(Mandatory = $true)][string]$Root,
        [Parameter(Mandatory = $true)][string]$ScratchDirectory,
        [Parameter(Mandatory = $true)][ValidatePattern('^[a-f0-9]{32}$')][string]$RunId
    )

    $projectRoot = [IO.Path]::GetFullPath($Root)
    $resolvedScratch = [IO.Path]::GetFullPath($ScratchDirectory)
    $expectedParent = [IO.Path]::GetFullPath((Join-Path $projectRoot '.local/tests')).TrimEnd('\')
    $actualParent = [IO.Path]::GetDirectoryName($resolvedScratch).TrimEnd('\')
    if (-not [string]::Equals($actualParent, $expectedParent, [StringComparison]::OrdinalIgnoreCase) -or
        [IO.Path]::GetFileName($resolvedScratch) -ne "verify-device-$RunId") {
        throw "Refusing to clean an unexpected test directory: $resolvedScratch"
    }

    if (Test-Path -LiteralPath $resolvedScratch) {
        $item = Get-Item -LiteralPath $resolvedScratch -Force
        if (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
            throw "Refusing to clean a redirected test directory: $resolvedScratch"
        }
        Remove-Item -LiteralPath $resolvedScratch -Recurse -Force
    }
    if (Test-Path -LiteralPath $resolvedScratch) {
        throw "Device verification temporary directory remains: $resolvedScratch"
    }
}
