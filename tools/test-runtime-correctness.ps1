param([ValidateSet('all','host','draw','alignment','canvas','inner')][string]$Suite = 'all')
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot 'TestEnvironment.ps1')
$temporaryRoot = [IO.Path]::GetFullPath((Join-Path $root '.local/tests'))
$testRoot = Join-Path $temporaryRoot ('runtime-correctness-' + [Guid]::NewGuid().ToString('N'))
$environmentBefore = @{}
foreach ($key in @('TEMP','TMP','JAVA_TOOL_OPTIONS','GRADLE_USER_HOME','ANDROID_USER_HOME','ANDROID_AVD_HOME')) { $environmentBefore[$key] = [Environment]::GetEnvironmentVariable($key,'Process') }
try {
    $fixtures = Join-Path $testRoot 'fixtures'
    $inner = Join-Path $testRoot 'inner'
    [void][IO.Directory]::CreateDirectory($fixtures)
    [void][IO.Directory]::CreateDirectory($inner)
    $env:TEMP = $testRoot; $env:TMP = $testRoot
    $env:GRADLE_USER_HOME = Join-Path $root '.local/gradle-home'
    $env:ANDROID_USER_HOME = Join-Path $testRoot 'android'
    $env:ANDROID_AVD_HOME = Join-Path $env:ANDROID_USER_HOME 'avd'
    [void][IO.Directory]::CreateDirectory($env:ANDROID_AVD_HOME)
    $javaHome = $testRoot.Replace('\','/')
    $env:JAVA_TOOL_OPTIONS = "-Djava.io.tmpdir=`"$javaHome`" -Duser.home=`"$javaHome`" -XX:-UsePerfData"
    $appClasses = Get-CircularTestAppClassesJar -Root $root
    $androidJar = Get-CircularTestAndroidJar -Root $root
    $apiJar = Get-CircularTestApiJar -Root $root -TemporaryDirectory $testRoot
    $stdlib = Get-CircularTestKotlinStdlib -Root $root
    $classpath = "$appClasses;$stdlib;$androidJar;$apiJar"
    $testSources = @(Get-ChildItem (Join-Path $PSScriptRoot 'tests/runtime-fixtures') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName)
    $testSources += @(
        (Join-Path $PSScriptRoot 'tests/com/cyzah/circular/hook/runtime/OemHostAttachTest.java'),
        (Join-Path $PSScriptRoot 'tests/com/cyzah/circular/hook/runtime/DrawFailureOwnershipTest.java'),
        (Join-Path $PSScriptRoot 'tests/com/cyzah/circular/hook/runtime/ExpansionOffsetRestoreTest.java'),
        (Join-Path $PSScriptRoot 'tests/com/cyzah/circular/core/graphics/CanvasFailureStateTest.java'))
    & javac --release 17 -encoding UTF-8 -cp "$classpath" -d $fixtures @testSources
    if ($LASTEXITCODE -ne 0) { throw 'Runtime boundary fixture compilation failed' }
    $suites = @{
        host = 'com.cyzah.circular.hook.runtime.OemHostAttachTest'
        draw = 'com.cyzah.circular.hook.runtime.DrawFailureOwnershipTest'
        alignment = 'com.cyzah.circular.hook.runtime.ExpansionOffsetRestoreTest'
        canvas = 'com.cyzah.circular.core.graphics.CanvasFailureStateTest'
    }
    $failures = 0
    foreach ($name in @('host','draw','alignment','canvas')) {
        if ($Suite -ne 'all' -and $Suite -ne $name) { continue }
        & java -cp "$fixtures;$classpath" $suites[$name]
        if ($LASTEXITCODE -ne 0) { $failures++ }
    }
    if ($Suite -eq 'all' -or $Suite -eq 'inner') {
        $innerSources = @(Get-ChildItem (Join-Path $PSScriptRoot 'tests/runtime-inner-fixtures') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName)
        $innerSources += (Join-Path $PSScriptRoot 'tests/com/cyzah/circular/hook/runtime/InnerDrawFailureTest.java')
        foreach ($name in @('IconSession','OemIconSession')) {
            Get-ChildItem (Join-Path $fixtures 'com/cyzah/circular/hook/runtime') -Filter "$name.class" | ForEach-Object { Remove-Item -LiteralPath $_.FullName }
        }
        & javac --release 17 -encoding UTF-8 -cp "$fixtures;$classpath" -d $inner @innerSources
        if ($LASTEXITCODE -ne 0) { throw 'Inner runtime boundary fixture compilation failed' }
        & java -cp "$inner;$fixtures;$classpath" com.cyzah.circular.hook.runtime.InnerDrawFailureTest
        if ($LASTEXITCODE -ne 0) { $failures++ }
    }
    if ($failures -ne 0) { throw "$failures runtime correctness suites failed" }
} finally {
    foreach ($key in $environmentBefore.Keys) {
        if ($null -eq $environmentBefore[$key]) { Remove-Item -LiteralPath "Env:$key" -ErrorAction SilentlyContinue }
        else { [Environment]::SetEnvironmentVariable($key,$environmentBefore[$key],'Process') }
    }
    $resolved = [IO.Path]::GetFullPath($testRoot)
    if (-not $resolved.StartsWith($temporaryRoot + '\',[StringComparison]::OrdinalIgnoreCase) -or [IO.Path]::GetFileName($resolved) -notlike 'runtime-correctness-*') { throw 'Unsafe runtime-test cleanup target' }
    if (Test-Path -LiteralPath $resolved) { Remove-Item -LiteralPath $resolved -Recurse -Force }
    if (Test-Path -LiteralPath $resolved) { throw "Runtime test directory remains: $resolved" }
}
