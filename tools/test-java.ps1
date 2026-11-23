$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'TestEnvironment.ps1')
$appClasses = Get-CircularTestAppClassesJar -Root $root
$stdlib = Get-CircularTestKotlinStdlib -Root $root
$classpath = "$appClasses;$stdlib"
$testDir = Join-Path $PSScriptRoot "tests"
$buildDir = Join-Path ((Join-Path $root '.local/tests')) ("circular-java-tests-" + [Guid]::NewGuid())
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

    $testClasses = @(
        "com.cyzah.circular.core.graphics.geometry.DuoLayoutTest",
        "com.cyzah.circular.domain.settings.NetworkTechnologyOptionsTest",
        "com.cyzah.circular.ui.preview.PreviewSceneCodecTest",
        "com.cyzah.circular.core.graphics.geometry.CapsuleLayoutTest",
        "com.cyzah.circular.domain.settings.NoticePolicyTest",
        "com.cyzah.circular.core.graphics.SignalCrossPolicyTest",
        "com.cyzah.circular.domain.settings.DualStyleTest",
        "com.cyzah.circular.core.graphics.geometry.SignalSpacingTest",
        "com.cyzah.circular.core.graphics.ChargeBoltPolicyTest",
        "com.cyzah.circular.core.graphics.geometry.RingGapPolicyTest",
        "com.cyzah.circular.domain.settings.StyleProfileTest",
        "com.cyzah.circular.domain.settings.SizePolicyTest",
        "VersionBumpTest"
    )
    $sources = @((Join-Path $PSScriptRoot 'VersionBump.java'))
    $sources += $testClasses | ForEach-Object { Join-Path $testDir ($_.Replace('.', '/') + '.java') }
    & javac --release 17 -encoding UTF-8 -cp $classpath -d $buildDir @sources
    if ($LASTEXITCODE -ne 0) { throw "Java test sources did not compile." }

    foreach ($testClass in $testClasses) {
        & java -cp "$buildDir;$classpath" $testClass
        if ($LASTEXITCODE -ne 0) {
            throw "Java behavior tests failed: $testClass"
        }
    }
} finally {
    foreach ($key in $environmentBefore.Keys) {
        if ($null -eq $environmentBefore[$key]) {
            Remove-Item -LiteralPath "Env:$key" -ErrorAction SilentlyContinue
        } else {
            [Environment]::SetEnvironmentVariable($key, $environmentBefore[$key], 'Process')
        }
    }
    $resolvedBuild = [IO.Path]::GetFullPath($buildDir)
    $expectedRoot = [IO.Path]::GetFullPath((Join-Path $root '.local/tests')).TrimEnd('\') + '\'
    if (-not $resolvedBuild.StartsWith($expectedRoot, [StringComparison]::OrdinalIgnoreCase)) { throw "Unexpected test directory: $resolvedBuild" }
    if (Test-Path -LiteralPath $buildDir) {
        Remove-Item -LiteralPath $buildDir -Recurse -Force
    }
    if (Test-Path -LiteralPath $buildDir) { throw "Java-test temporary directory remains: $buildDir" }
}
