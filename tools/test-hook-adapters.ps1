param(
    [string]$AndroidJar = "",
    [string]$ApiJar = "",
    [string]$AppClasses = "",
    [string]$GradleUserHome = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'TestEnvironment.ps1')
if (-not $AppClasses) {
    $AppClasses = Get-CircularTestAppClassesJar -Root $root
}
if (-not (Test-Path -LiteralPath $AppClasses)) {
    throw "Compile the debug app before running hook adapter tests, or pass -AppClasses."
}

if (-not $AndroidJar) { $AndroidJar = Get-CircularTestAndroidJar -Root $root }
$temporaryRoot = [IO.Path]::GetFullPath((Join-Path $root '.local/tests'))
$buildDir = Join-Path $temporaryRoot ("circular-hook-tests-" + [Guid]::NewGuid())
$environmentBefore = @{}
foreach ($key in @('TEMP', 'TMP', 'JAVA_TOOL_OPTIONS')) {
    $environmentBefore[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
}
try {
    [IO.Directory]::CreateDirectory($buildDir) | Out-Null
    $env:TEMP = $buildDir
    $env:TMP = $buildDir
    $javaHome = $buildDir.Replace('\', '/')
    $env:JAVA_TOOL_OPTIONS = "-Djava.io.tmpdir=`"$javaHome`" -Duser.home=`"$javaHome`" -XX:-UsePerfData"
    if (-not $ApiJar) { $ApiJar = Get-CircularTestApiJar -Root $root -TemporaryDirectory $buildDir -GradleUserHome $GradleUserHome }
    if (-not (Test-Path -LiteralPath $ApiJar -PathType Leaf)) { throw 'libxposed API jar is unavailable.' }
    if (-not (Test-Path -LiteralPath $AndroidJar -PathType Leaf)) { throw 'Android platform jar is unavailable.' }
    $stdlib = Get-CircularTestKotlinStdlib -Root $root -GradleUserHome $GradleUserHome
    $classpath = "$buildDir;$AppClasses;$stdlib;$AndroidJar;$ApiJar"
    $sources = @(
        (Join-Path $PSScriptRoot "tests/com/cyzah/circular/hook/runtime/ChargingIslandHookTest.java"),
        (Join-Path $PSScriptRoot "tests/com/cyzah/circular/hook/runtime/AlignmentControllerTest.java")
    )
    & javac --release 17 -encoding UTF-8 -cp $classpath -d $buildDir @sources
    if ($LASTEXITCODE -ne 0) { throw "Hook adapter test compilation failed." }
    & java -cp $classpath com.cyzah.circular.hook.runtime.ChargingIslandHookTest
    if ($LASTEXITCODE -ne 0) { throw "Hook adapter behavior checks failed." }
    & java -cp $classpath com.cyzah.circular.hook.runtime.AlignmentControllerTest
    if ($LASTEXITCODE -ne 0) { throw "Alignment adapter behavior checks failed." }
} finally {
    foreach ($key in $environmentBefore.Keys) {
        if ($null -eq $environmentBefore[$key]) {
            Remove-Item -LiteralPath "Env:$key" -ErrorAction SilentlyContinue
        } else {
            [Environment]::SetEnvironmentVariable($key, $environmentBefore[$key], 'Process')
        }
    }
    $resolvedBuildDir = [IO.Path]::GetFullPath($buildDir)
    if (-not $resolvedBuildDir.StartsWith($temporaryRoot, [StringComparison]::OrdinalIgnoreCase) `
            -or [IO.Path]::GetFileName($resolvedBuildDir) -notlike 'circular-hook-tests-*') {
        throw "Refusing to clean an unexpected test directory: $resolvedBuildDir"
    }
    if (Test-Path -LiteralPath $resolvedBuildDir) {
        Remove-Item -LiteralPath $resolvedBuildDir -Recurse -Force
    }
    if (Test-Path -LiteralPath $resolvedBuildDir) { throw "Hook-test temporary directory remains: $resolvedBuildDir" }
}
