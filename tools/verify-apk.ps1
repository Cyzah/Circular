param(
    [string]$ApkPath = '',
    [ValidateSet('github', 'lspRepo')][string]$Edition = 'github'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot 'ApkPaths.ps1')
. (Join-Path $PSScriptRoot 'ApkContract.ps1')
$apk = Get-CircularApkPath -Root $root -Edition $Edition -Variant release -ApkPath $ApkPath
$tools = Get-CircularApkTools $root
Assert-CircularApkIdentity -Root $root -ApkPath $apk -Analyzer $tools.Analyzer -Edition $Edition

$badging = Invoke-ApkTool $tools.Aapt @('dump', 'badging', $apk)
Assert-Contract ($badging -match "application-label:'Circular'") 'Circular application label'
$releaseVersion = ConvertFrom-StringData (Get-Content -LiteralPath (Join-Path $root 'version.properties') -Raw)
$expectedName = "$($releaseVersion.major).$($releaseVersion.minor).$($releaseVersion.patch)"
$expectedCode = $releaseVersion.versionCode
if (!$expectedCode) { throw 'version.properties must define versionCode.' }
Assert-Contract ($badging.Contains("versionName='$expectedName'")) "release version name $expectedName"
Assert-Contract ($badging.Contains("versionCode='$expectedCode'")) "Android update code $expectedCode"
$entryClass = 'com.cyzah.circular.hook.runtime.ModuleMain'
$activityClass = 'com.cyzah.circular.ui.activity.MainActivity'
$archive = [IO.Compression.ZipFile]::OpenRead($apk)
try {
    $fonts = @($archive.Entries | Where-Object { $_.FullName -match '^fonts/SF-Pro-Rounded-[A-Za-z]+\.otf$' })
    Assert-Contract ($fonts.Count -eq 9) 'nine status font weights retained as Java resources'
    foreach ($weight in @('Black','Bold','Heavy','Light','Medium','Regular','Semibold','Thin','Ultralight')) {
        $font = $archive.GetEntry("fonts/SF-Pro-Rounded-$weight.otf")
        Assert-Contract ($null -ne $font -and $font.Length -gt 0) "SF-Pro-Rounded-$weight exists"
    }
    $fontBytes = ($fonts | Measure-Object Length -Sum).Sum
    Assert-Contract ($fontBytes -lt 600000) 'only subset fonts packaged'
    $dexBytes = ($archive.Entries | Where-Object { $_.FullName -match '^classes\d*\.dex$' } | Measure-Object Length -Sum).Sum
} finally { $archive.Dispose() }
$listing = Invoke-ApkTool $tools.Analyzer @('dex', 'packages', '--defined-only', $apk)
$classes = @(Get-DefinedClassNames $listing)
Assert-Contract ($classes.Count -gt 0) 'defined-class listing parsed'
Assert-Contract ($classes -ccontains $entryClass) 'LSPosed entry class survives shrinking'
Assert-Contract ($classes -ccontains $activityClass) 'exact MainActivity class survives shrinking'
Assert-Contract (@($classes | Where-Object { $_ -match '^io\.github\.libxposed\.api(?:\.|$)' }).Count -eq 0) 'framework API remains compileOnly (class definitions only)'

$signature = Invoke-ApkTool $tools.Signer @('verify', '--verbose', $apk)
Assert-Contract ($signature -match 'Verified using v3 scheme.*true') 'release V3 signature valid'
[pscustomobject]@{Apk=$apk;Bytes=(Get-Item -LiteralPath $apk).Length;DefinedClasses=$classes.Count;DexUncompressedBytes=$dexBytes;FontUncompressedBytes=$fontBytes;EntryClass=$entryClass} | Format-List
