param(
    [Parameter(Mandatory=$true)][ValidateNotNullOrEmpty()][string]$Serial,
    [int]$AdbPort = 5037,
    [switch]$AsRoot,
    [ValidateSet('github', 'lspRepo')][string]$Edition = 'github'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot 'TestEnvironment.ps1')
. (Join-Path $PSScriptRoot 'ApkPaths.ps1')
$appApk = Get-CircularApkPath -Root $root -Variant debug -Edition $Edition
if (!(Test-Path -LiteralPath $appApk -PathType Leaf)) { throw "Build the $Edition Debug APK first: $appApk" }
$sdk = Get-CircularTestSdk -Root $root
$adb = Join-Path $sdk 'platform-tools/adb.exe'
$androidJar = Get-CircularTestAndroidJar -Root $root
$d8 = Get-CircularTestD8 -Root $root
$appClasses = Get-CircularTestAppClassesJar -Root $root -Edition $Edition
$stdlib = Get-CircularTestKotlinStdlib -Root $root
$runId = [Guid]::NewGuid().ToString('N')
$temporaryRoot = [IO.Path]::GetFullPath((Join-Path $root '.local/tests'))
$testRoot = Join-Path $temporaryRoot ("canvas-state-$runId")
$deviceRoot = "/data/local/tmp/circular-canvas-state-$runId"
$remoteAttempted = $false
$environmentBefore = @{}
foreach ($key in @('TEMP','TMP','JAVA_TOOL_OPTIONS','GRADLE_USER_HOME','ANDROID_USER_HOME','ANDROID_AVD_HOME')) { $environmentBefore[$key] = [Environment]::GetEnvironmentVariable($key,'Process') }
try {
    $classes = Join-Path $testRoot 'classes'; [void][IO.Directory]::CreateDirectory($classes)
    $env:TEMP = $testRoot; $env:TMP = $testRoot; $env:ANDROID_USER_HOME = Join-Path $testRoot 'android'
    $env:ANDROID_AVD_HOME = Join-Path $env:ANDROID_USER_HOME 'avd'; $env:GRADLE_USER_HOME = Join-Path $root '.local/gradle-home'
    [void][IO.Directory]::CreateDirectory($env:ANDROID_AVD_HOME)
    $javaHome = $testRoot.Replace('\','/')
    $env:JAVA_TOOL_OPTIONS = "-Djava.io.tmpdir=`"$javaHome`" -Duser.home=`"$javaHome`" -XX:-UsePerfData"
    $actualSerial = & $adb -P $AdbPort -s $Serial get-serialno
    if ($LASTEXITCODE -ne 0 -or $actualSerial.Trim() -ne $Serial) { throw 'Explicit device identity does not match; confirm before retrying' }
    & javac --release 17 -encoding UTF-8 -cp "$androidJar;$appClasses;$stdlib" -d $classes (Join-Path $PSScriptRoot 'tests/com/cyzah/circular/core/graphics/CanvasAndroidStateTest.java')
    if ($LASTEXITCODE -ne 0) { throw 'Canvas numerical test compilation failed' }
    $classJar = Join-Path $testRoot 'classes.jar'; $dexJar = Join-Path $testRoot 'tests.jar'
    & jar --create --file $classJar -C $classes .
    if ($LASTEXITCODE -ne 0) { throw 'Canvas test archive failed' }
    & $d8 --min-api 35 --lib $androidJar --classpath $appClasses --classpath $stdlib --output $dexJar $classJar
    if ($LASTEXITCODE -ne 0) { throw 'Canvas test dex compilation failed' }
    $remoteAttempted = $true
    & $adb -P $AdbPort -s $Serial shell mkdir -p $deviceRoot
    if ($LASTEXITCODE -ne 0) { throw 'Cannot prepare task-owned remote directory' }
    & $adb -P $AdbPort -s $Serial push $dexJar "$deviceRoot/tests.jar"
    if ($LASTEXITCODE -ne 0) { throw 'Cannot copy Canvas test' }
    & $adb -P $AdbPort -s $Serial push $appApk "$deviceRoot/app.apk"
    if ($LASTEXITCODE -ne 0) { throw 'Cannot copy app under test' }
    & $adb -P $AdbPort -s $Serial shell chmod 444 "$deviceRoot/tests.jar" "$deviceRoot/app.apk"
    if ($LASTEXITCODE -ne 0) { throw 'Cannot mark task-owned dynamic code read-only' }
    $command = "CLASSPATH=$deviceRoot/tests.jar" + ':' + "$deviceRoot/app.apk app_process /system/bin com.cyzah.circular.core.graphics.CanvasAndroidStateTest"
    if ($AsRoot) { $command = "su -c '$command'" }
    & $adb -P $AdbPort -s $Serial shell $command
    if ($LASTEXITCODE -ne 0) { throw 'Android Canvas numerical state checks failed' }
} finally {
    try {
        if ($remoteAttempted) {
            & $adb -P $AdbPort -s $Serial shell rm -rf $deviceRoot
            if ($LASTEXITCODE -ne 0) { throw "Cannot clean task-owned remote path $deviceRoot" }
            & $adb -P $AdbPort -s $Serial shell test ! -e $deviceRoot
            if ($LASTEXITCODE -ne 0) { throw "Task-owned remote path remains $deviceRoot" }
        }
    } finally {
        foreach ($key in $environmentBefore.Keys) {
            if ($null -eq $environmentBefore[$key]) { Remove-Item -LiteralPath "Env:$key" -ErrorAction SilentlyContinue }
            else { [Environment]::SetEnvironmentVariable($key,$environmentBefore[$key],'Process') }
        }
        $resolved = [IO.Path]::GetFullPath($testRoot)
        if (-not $resolved.StartsWith($temporaryRoot + '\',[StringComparison]::OrdinalIgnoreCase) -or [IO.Path]::GetFileName($resolved) -ne "canvas-state-$runId") { throw 'Unsafe Canvas-test cleanup target' }
        if (Test-Path -LiteralPath $resolved) { Remove-Item -LiteralPath $resolved -Recurse -Force }
        if (Test-Path -LiteralPath $resolved) { throw "Task-owned local path remains $resolved" }
    }
}
