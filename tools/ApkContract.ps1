. (Join-Path $PSScriptRoot 'TestEnvironment.ps1')

function Get-CircularApkTools([string]$Root) {
    foreach ($folder in @('tmp', 'android/avd', 'runtime-home')) {
        [void][IO.Directory]::CreateDirectory((Join-Path $Root ".local/$folder"))
    }
    $env:TEMP = Join-Path $Root '.local/tmp'
    $env:TMP = $env:TEMP
    $env:ANDROID_USER_HOME = Join-Path $Root '.local/android'
    $env:ANDROID_AVD_HOME = Join-Path $Root '.local/android/avd'
    $tempPath = $env:TEMP.Replace('\', '/')
    $runtimeHome = (Join-Path $Root '.local/runtime-home').Replace('\', '/')
    $env:JAVA_TOOL_OPTIONS = '-Djava.io.tmpdir="' + $tempPath + '" -Duser.home="' + $runtimeHome + '" -XX:-UsePerfData'
    $sdk = Get-CircularTestSdk -Root $Root
    $buildTools = Join-Path $sdk 'build-tools/36.0.0'
    @{
        Analyzer = Join-Path $sdk 'cmdline-tools/latest/bin/apkanalyzer.bat'
        Aapt = Join-Path $buildTools 'aapt2.exe'
        Signer = Join-Path $buildTools 'apksigner.bat'
    }
}

function Invoke-ApkTool([string]$Executable, [string[]]$Arguments) {
    if (-not (Test-Path -LiteralPath $Executable -PathType Leaf)) { throw "APK tool is missing: $Executable" }
    # Windows PowerShell 5 reports Java stderr as records even when the tool succeeds.
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = & $Executable @Arguments 2>&1
        $code = $LASTEXITCODE
    } finally { $ErrorActionPreference = $previousPreference }
    $lines = @($output | ForEach-Object { $_.ToString() })
    if ($code -ne 0) { throw "$Executable exited $code : $($lines -join [Environment]::NewLine)" }
    return ($lines | Where-Object { $_ -notmatch '^Picked up (JAVA_TOOL_OPTIONS|JDK_JAVA_OPTIONS):' }) -join "`n"
}

function Assert-Contract([bool]$Condition, [string]$Description) {
    if (!$Condition) { throw "APK contract failed: $Description" }
    Write-Output "PASS $Description"
}

function Get-DefinedClassNames([string]$PackageListing) {
    # C d rows are defined classes. M/F rows may reference compileOnly types.
    foreach ($line in ($PackageListing -split '\r?\n')) {
        if ($line -match '^C\s+d\s+') { ($line.Trim() -split '\s+')[-1] }
    }
}

function Assert-CircularApkIdentity {
    param(
        [Parameter(Mandatory = $true)][string]$Root,
        [Parameter(Mandatory = $true)][string]$ApkPath,
        [Parameter(Mandatory = $true)][string]$Analyzer,
        [Parameter(Mandatory = $true)][ValidateSet('github', 'lspRepo')][string]$Edition
    )

    if (!(Test-Path -LiteralPath $ApkPath -PathType Leaf)) { throw "APK not found: $ApkPath" }
    $appId = if ($Edition -eq 'github') { 'com.cyzah.circular' } else { 'io.github.cyzah.circular' }
    [xml]$manifest = Invoke-ApkTool $Analyzer @('manifest', 'print', $ApkPath)
    Assert-Contract ($manifest.DocumentElement.GetAttribute('package') -ceq $appId) "$Edition APK application ID $appId"
    $android = 'http://schemas.android.com/apk/res/android'
    $ns = [Xml.XmlNamespaceManager]::new($manifest.NameTable)
    $ns.AddNamespace('android', $android)
    $activityClass = 'com.cyzah.circular.ui.activity.MainActivity'
    $activity = $manifest.SelectSingleNode("/manifest/application/activity[@android:name='$activityClass']", $ns)
    Assert-Contract ($null -ne $activity) 'exact MainActivity manifest entry'
    Assert-Contract ($null -ne $activity.SelectSingleNode("intent-filter/category[@android:name='de.robv.android.xposed.category.MODULE_SETTINGS']", $ns)) 'LSPosed settings entry retained'
    $alias = $manifest.SelectSingleNode("/manifest/application/activity-alias[@android:name='$appId.Launcher']", $ns)
    Assert-Contract ($null -ne $alias -and $alias.GetAttribute('targetActivity', $android) -eq $activityClass) 'launcher alias targets MainActivity'
    Assert-Contract ($null -ne $alias.SelectSingleNode("intent-filter/category[@android:name='android.intent.category.LAUNCHER']", $ns)) 'launcher category retained'
    $config = $manifest.SelectSingleNode("/manifest/application/provider[@android:name='com.cyzah.circular.data.settings.ConfigProvider']", $ns)
    Assert-Contract ($null -ne $config -and $config.GetAttribute('authorities', $android) -ceq "$appId.config") 'configuration provider matches application ID'
    Assert-Contract ($config.GetAttribute('directBootAware', $android) -eq 'true') 'direct-boot configuration access retained'
    $exports = $manifest.SelectSingleNode("/manifest/application/provider[@android:name='com.cyzah.circular.framework.export.ExportFileProvider']", $ns)
    if ($Edition -eq 'github') {
        Assert-Contract ($null -ne $exports -and $exports.GetAttribute('authorities', $android) -ceq "$appId.exports") 'GitHub export provider retained'
    } else {
        Assert-Contract ($null -eq $exports) 'repository APK has no export provider'
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($ApkPath)
    try {
        foreach ($name in @('java_init.list', 'module.prop', 'scope.list')) {
            $entry = $archive.GetEntry("META-INF/xposed/$name")
            Assert-Contract ($null -ne $entry) "API 102 metadata $name included"
            $reader = [IO.StreamReader]::new($entry.Open())
            try { $value = $reader.ReadToEnd().Trim() } finally { $reader.Dispose() }
            $expected = (Get-Content -LiteralPath (Join-Path $Root "app/src/main/resources/META-INF/xposed/$name") -Raw).Trim()
            Assert-Contract ($value -ceq $expected) "$name matches shared source"
            if ($name -eq 'java_init.list') { Assert-Contract ($value -ceq 'com.cyzah.circular.hook.runtime.ModuleMain') 'exact API 102 module entry' }
            if ($name -eq 'scope.list') { Assert-Contract ($value -ceq 'com.android.systemui') 'SystemUI-only scope' }
            if ($name -eq 'module.prop') {
                Assert-Contract ($value -match '(?m)^minApiVersion=102\s*$' -and $value -match '(?m)^targetApiVersion=102\s*$') 'API 102 minimum and target'
            }
        }
    } finally { $archive.Dispose() }
}
