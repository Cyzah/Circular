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
$temporary = Join-Path $safeRoot ('pixel-reference-' + [Guid]::NewGuid().ToString('N'))
$before = @{}
foreach ($key in @('TEMP', 'TMP', 'JAVA_TOOL_OPTIONS')) { $before[$key] = [Environment]::GetEnvironmentVariable($key, 'Process') }
try {
    $fixtures = Join-Path $temporary 'fixtures'
    [void][IO.Directory]::CreateDirectory($fixtures)
    $env:TEMP = $temporary; $env:TMP = $temporary
    $homePath = $temporary.Replace('\', '/')
    $env:JAVA_TOOL_OPTIONS = "-Djava.io.tmpdir=`"$homePath`" -Duser.home=`"$homePath`" -Duser.language=en -Duser.country=US -XX:-UsePerfData"
    $androidJar = Get-CircularTestAndroidJar -Root $root
    $appClasses = Get-CircularTestAppClassesJar -Root $root
    $stdlib = Get-CircularTestKotlinStdlib -Root $root
    $classpath = "$appClasses;$stdlib;$androidJar"
    $tests = @(
        (Join-Path $PSScriptRoot 'tests/pixel-reference-fixtures/android/graphics/Canvas.java'),
        (Join-Path $PSScriptRoot 'tests/pixel-reference-fixtures/android/graphics/Paint.java'),
        (Join-Path $PSScriptRoot 'tests/pixel-reference-fixtures/android/graphics/Path.java'),
        (Join-Path $PSScriptRoot 'tests/pixel-reference-fixtures/android/graphics/Rect.java'),
        (Join-Path $PSScriptRoot 'tests/pixel-reference-fixtures/android/graphics/RectF.java'),
        (Join-Path $PSScriptRoot 'tests/com/cyzah/circular/core/graphics/ContentPixelReferenceTest.java'),
        (Join-Path $PSScriptRoot 'tests/com/cyzah/circular/core/graphics/LayeredContentGroupingTest.java'),
        (Join-Path $PSScriptRoot 'tests/com/cyzah/circular/core/graphics/DuoSignalMotionTest.java'))
    $ErrorActionPreference = 'Continue'
    & javac --release 17 -encoding UTF-8 -cp "$classpath" -d $fixtures @tests
    $code = $LASTEXITCODE
    if ($code -eq 0) {
        & java -cp "$fixtures;$classpath" com.cyzah.circular.core.graphics.ContentPixelReferenceTest
        $code = $LASTEXITCODE
        if ($code -eq 0) {
            & java -cp "$fixtures;$classpath" com.cyzah.circular.core.graphics.LayeredContentGroupingTest
            $code = $LASTEXITCODE
        }
        if ($code -eq 0) {
            & java -cp "$fixtures;$classpath" com.cyzah.circular.core.graphics.DuoSignalMotionTest
            $code = $LASTEXITCODE
        }
    }
    $ErrorActionPreference = 'Stop'
    if ($code -ne 0) { throw 'Pixel reference boundary regression failed' }
} finally {
    foreach ($key in $before.Keys) { [Environment]::SetEnvironmentVariable($key, $before[$key], 'Process') }
    $resolved = [IO.Path]::GetFullPath($temporary)
    if (-not $resolved.StartsWith($safeRoot + '\', [StringComparison]::OrdinalIgnoreCase) -or
            [IO.Path]::GetFileName($resolved) -notlike 'pixel-reference-*') { throw 'Unsafe test cleanup target' }
    if (Test-Path -LiteralPath $resolved) { Remove-Item -LiteralPath $resolved -Recurse -Force }
    if (Test-Path -LiteralPath $resolved) { throw 'Pixel reference temporary files remain' }
    Write-Output 'Task temporary files removed; no device files used'
}
