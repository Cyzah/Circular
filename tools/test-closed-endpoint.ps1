param([string]$TemporaryRoot = '')
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot 'TestEnvironment.ps1')
if (-not $TemporaryRoot) { $TemporaryRoot = Join-Path $root '.local/tests' }
$safeRoot = [IO.Path]::GetFullPath($TemporaryRoot).TrimEnd('\')
$projectTestRoot = [IO.Path]::GetFullPath((Join-Path $root '.local/tests')).TrimEnd('\')
if (-not [string]::Equals($safeRoot,$projectTestRoot,[StringComparison]::OrdinalIgnoreCase) -and
    -not $safeRoot.StartsWith($projectTestRoot + '\',[StringComparison]::OrdinalIgnoreCase)) {
    throw '-TemporaryRoot must stay inside this project .local/tests directory.'
}
$temporary = Join-Path $safeRoot ('closed-endpoint-' + [Guid]::NewGuid().ToString('N'))
$before = @{}
foreach ($key in @('TEMP','TMP','JAVA_TOOL_OPTIONS','GRADLE_USER_HOME','ANDROID_USER_HOME','ANDROID_AVD_HOME')) {
    $before[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
}
try {
    $fixtures = Join-Path $temporary 'fixtures'
    [void][IO.Directory]::CreateDirectory($fixtures)
    $env:TEMP = $temporary; $env:TMP = $temporary
    $env:GRADLE_USER_HOME = Join-Path $root '.local/gradle-home'
    $env:ANDROID_USER_HOME = Join-Path $temporary 'android'
    $env:ANDROID_AVD_HOME = Join-Path $env:ANDROID_USER_HOME 'avd'
    [void][IO.Directory]::CreateDirectory($env:ANDROID_AVD_HOME)
    $homePath = $temporary.Replace('\', '/')
    $env:JAVA_TOOL_OPTIONS = "-Djava.io.tmpdir=`"$homePath`" -Duser.home=`"$homePath`" -Duser.language=en -Duser.country=US -XX:-UsePerfData"
    $androidJar = Get-CircularTestAndroidJar -Root $root
    $appClasses = Get-CircularTestAppClassesJar -Root $root
    $apiJar = Get-CircularTestApiJar -Root $root -TemporaryDirectory $temporary
    $stdlib = Get-CircularTestKotlinStdlib -Root $root
    $classpath = "$appClasses;$stdlib;$androidJar;$apiJar"
    $tests = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'tests/closed-endpoint-fixtures') -Recurse -Filter '*.java' |
        Select-Object -ExpandProperty FullName)
    $tests += Join-Path $PSScriptRoot 'tests/com/cyzah/circular/hook/runtime/ClosedEndpointDrawTest.java'
    $ErrorActionPreference = 'Continue'
    & javac --release 17 -encoding UTF-8 -cp "$classpath" -d $fixtures @tests
    $code = $LASTEXITCODE
    if ($code -eq 0) {
        & java -cp "$fixtures;$classpath" com.cyzah.circular.hook.runtime.ClosedEndpointDrawTest
        $code = $LASTEXITCODE
    }
    $ErrorActionPreference = 'Stop'
    if ($code -ne 0) { throw 'Actual closed-endpoint draw regression failed' }
} finally {
    foreach ($key in $before.Keys) { [Environment]::SetEnvironmentVariable($key, $before[$key], 'Process') }
    $resolved = [IO.Path]::GetFullPath($temporary)
    if (-not $resolved.StartsWith($safeRoot + '\', [StringComparison]::OrdinalIgnoreCase) -or
            [IO.Path]::GetFileName($resolved) -notlike 'closed-endpoint-*') { throw 'Unsafe endpoint test cleanup target' }
    if (Test-Path -LiteralPath $resolved) { Remove-Item -LiteralPath $resolved -Recurse -Force }
    if (Test-Path -LiteralPath $resolved) { throw 'Endpoint test temporary files remain' }
    Write-Output 'Task endpoint temporary files removed; no device files used'
}
