function Get-CircularApkPath {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][ValidateNotNullOrEmpty()][string]$Root,
        [ValidateSet('debug', 'release')][string]$Variant = 'release',
        [string]$ApkPath = '',
        [ValidateSet('github', 'lspRepo')][string]$Edition = 'github'
    )

    $projectRoot = [IO.Path]::GetFullPath($Root)
    if (![string]::IsNullOrWhiteSpace($ApkPath)) {
        if ([IO.Path]::IsPathRooted($ApkPath)) { return $ApkPath }
        return Join-Path $projectRoot $ApkPath
    }

    $version = Get-Content -LiteralPath (Join-Path $projectRoot 'version.properties') -Raw -ErrorAction Stop |
        ConvertFrom-StringData
    $parts = foreach ($key in @('major', 'minor', 'patch')) {
        if (!$version.ContainsKey($key) -or $version[$key] -notmatch '^\d+$') {
            throw "version.properties must define a non-negative integer $key."
        }
        $version[$key]
    }
    $versionName = $parts -join '.'
    $editionSuffix = if ($Edition -eq 'lspRepo') { '-lsp-repo' } else { '' }
    $variantSuffix = if ($Variant -eq 'debug') { '-debug' } else { '' }
    Join-Path $projectRoot "app/build/outputs/apk/$Edition/$Variant/Circular-$versionName$editionSuffix$variantSuffix.apk"
}
