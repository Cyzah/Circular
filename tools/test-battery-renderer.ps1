param(
    [Parameter(Mandatory = $true)][ValidateNotNullOrEmpty()][string]$Serial,
    [string]$Apk = '',
    [int]$AdbPort = 5037,
    [ValidateSet('github', 'lspRepo')][string]$Edition = 'github'
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'TestEnvironment.ps1')
. (Join-Path $PSScriptRoot 'ApkPaths.ps1')
$appApk = Get-CircularApkPath -Root $root -Variant debug -Edition $Edition -ApkPath $Apk
if (!(Test-Path -LiteralPath $appApk -PathType Leaf)) { throw "Debug APK not found: $appApk" }
$sdk = Get-CircularTestSdk -Root $root
$adb = Join-Path $sdk 'platform-tools/adb.exe'
$androidJar = Get-CircularTestAndroidJar -Root $root
$d8 = Get-CircularTestD8 -Root $root
$appClasses = Get-CircularTestAppClassesJar -Root $root -Edition $Edition
$stdlib = Get-CircularTestKotlinStdlib -Root $root
$runId = [Guid]::NewGuid().ToString('N')
$testRoot = Join-Path $root ".local/tests/battery-render-$runId"
$deviceRoot = "/data/local/tmp/circular-battery-render-$runId"
$deviceDirectoryAttempted = $false
$environmentBefore = @{}
foreach ($key in @('TEMP', 'TMP', 'JAVA_TOOL_OPTIONS', 'ANDROID_USER_HOME')) {
    $environmentBefore[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
}
$classes = Join-Path $testRoot "classes"
$classJar = Join-Path $testRoot "tests-classes.jar"
$dexJar = Join-Path $testRoot "tests.jar"
$sources = @(
    (Join-Path $PSScriptRoot "tests/com/cyzah/circular/core/graphics/BatteryNumberRenderingTest.java"),
    (Join-Path $PSScriptRoot "tests/com/cyzah/circular/core/graphics/ClosingBatteryReadoutTest.java"),
    (Join-Path $PSScriptRoot "tests/com/cyzah/circular/hook/runtime/SignalEndpointRenderingTest.java")
)
try {
    [IO.Directory]::CreateDirectory($classes) | Out-Null
    $env:TEMP = $testRoot
    $env:TMP = $testRoot
    $env:ANDROID_USER_HOME = Join-Path $testRoot 'android'
    [void][IO.Directory]::CreateDirectory($env:ANDROID_USER_HOME)
    $javaHome = $testRoot.Replace('\', '/')
    $env:JAVA_TOOL_OPTIONS = "-Djava.io.tmpdir=`"$javaHome`" -Duser.home=`"$javaHome`" -XX:-UsePerfData"
    $actualSerial = & $adb -P $AdbPort -s $Serial get-serialno
    if ($LASTEXITCODE -ne 0 -or $actualSerial.Trim() -ne $Serial) { throw 'Explicit device identity does not match; confirm before retrying' }
    & javac --release 17 -encoding UTF-8 -cp "$androidJar;$appClasses;$stdlib" -d $classes @sources
    if ($LASTEXITCODE -ne 0) { throw "Rendering test compilation failed" }
    & jar --create --file $classJar -C $classes .
    if ($LASTEXITCODE -ne 0) { throw "Rendering test archive failed" }
    & $d8 --min-api 35 --lib $androidJar --classpath $appClasses --classpath $stdlib --output $dexJar $classJar
    if ($LASTEXITCODE -ne 0) { throw "Rendering test dex compilation failed" }
    $deviceDirectoryAttempted = $true
    & $adb -P $AdbPort -s $Serial shell mkdir -p $deviceRoot
    if ($LASTEXITCODE -ne 0) { throw "Cannot prepare device test directory" }
    & $adb -P $AdbPort -s $Serial push $dexJar "$deviceRoot/tests.jar"
    if ($LASTEXITCODE -ne 0) { throw "Cannot copy rendering test" }
    & $adb -P $AdbPort -s $Serial push $appApk "$deviceRoot/app.apk"
    if ($LASTEXITCODE -ne 0) { throw "Cannot copy app under test" }
    & $adb -P $AdbPort -s $Serial shell chmod 444 "$deviceRoot/tests.jar" "$deviceRoot/app.apk"
    if ($LASTEXITCODE -ne 0) { throw 'Cannot mark task-owned dynamic code read-only' }
    # Offscreen Bitmap/Canvas assertions only; this does not capture the screen.
    & $adb -P $AdbPort -s $Serial shell "CLASSPATH=$deviceRoot/tests.jar`:$deviceRoot/app.apk app_process /system/bin com.cyzah.circular.core.graphics.BatteryNumberRenderingTest"
    if ($LASTEXITCODE -ne 0) { throw "Battery rendering checks failed" }
    & $adb -P $AdbPort -s $Serial shell "CLASSPATH=$deviceRoot/tests.jar`:$deviceRoot/app.apk app_process /system/bin com.cyzah.circular.core.graphics.ClosingBatteryReadoutTest"
    if ($LASTEXITCODE -ne 0) { throw "Battery readout handoff checks failed" }
    & $adb -P $AdbPort -s $Serial shell "CLASSPATH=$deviceRoot/tests.jar`:$deviceRoot/app.apk app_process /system/bin com.cyzah.circular.hook.runtime.SignalEndpointRenderingTest"
    if ($LASTEXITCODE -ne 0) { throw "Signal endpoint checks failed" }
} finally {
    $deviceCleanupFailed = $false
    try {
        if ($deviceDirectoryAttempted) {
            & $adb -P $AdbPort -s $Serial shell rm -rf $deviceRoot
            if ($LASTEXITCODE -ne 0) { throw "Cannot remove task-owned device directory: $deviceRoot" }
            & $adb -P $AdbPort -s $Serial shell test ! -e $deviceRoot
            if ($LASTEXITCODE -ne 0) { throw "Task-owned device directory remains: $deviceRoot" }
        }
    } catch {
        $deviceCleanupFailed = $true
        Write-Warning "Device cleanup failed for $deviceRoot : $($_.Exception.Message)"
    } finally {
        foreach ($key in $environmentBefore.Keys) {
            if ($null -eq $environmentBefore[$key]) {
                Remove-Item -LiteralPath "Env:$key" -ErrorAction SilentlyContinue
            } else {
                [Environment]::SetEnvironmentVariable($key, $environmentBefore[$key], 'Process')
            }
        }
        $resolvedTestRoot = [IO.Path]::GetFullPath($testRoot)
        $expectedRoot = [IO.Path]::GetFullPath((Join-Path $root '.local/tests')).TrimEnd('\') + '\'
        if (-not $resolvedTestRoot.StartsWith($expectedRoot, [StringComparison]::OrdinalIgnoreCase) -or
            [IO.Path]::GetFileName($resolvedTestRoot) -ne "battery-render-$runId") {
            throw "Refusing to clean an unexpected test directory: $resolvedTestRoot"
        }
        if (Test-Path -LiteralPath $resolvedTestRoot) {
            Remove-Item -LiteralPath $resolvedTestRoot -Recurse -Force
        }
    }
    if (Test-Path -LiteralPath $testRoot) { throw "Rendering-test temporary directory remains: $testRoot" }
    if ($deviceCleanupFailed) { throw "Rendering test device cleanup failed: $deviceRoot" }
}
