$ErrorActionPreference = "Stop"

$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$checker = Join-Path $root "tools/check-maintainability.ps1"
$temp = Join-Path ((Join-Path $root '.local/tests')) ("circular-maintainability-" + [Guid]::NewGuid())

function Invoke-Check {
    param([string]$ProjectRoot)

    $output = & powershell -NoProfile -ExecutionPolicy Bypass `
        -File $checker -Root $ProjectRoot 2>&1
    return [pscustomobject]@{
        ExitCode = $LASTEXITCODE
        Output = ($output -join "`n")
    }
}

try {
    $javaDir = Join-Path $temp "app/src/main/java/com/example"
    [IO.Directory]::CreateDirectory($javaDir) | Out-Null
    [IO.File]::WriteAllText(
        (Join-Path $javaDir "Dirty.java"),
        "final class Dirty { void m12lambda`$0`$example() {} }",
        [Text.UTF8Encoding]::new($false))

    $dirty = Invoke-Check $temp
    if ($dirty.ExitCode -eq 0) {
        throw "Checker accepted a synthetic callback name."
    }
    if (-not $dirty.Output.Contains("app/src/main/java/com/example/Dirty.java")) {
        throw "Checker did not report the offending file."
    }

    [IO.File]::WriteAllText(
        (Join-Path $javaDir "Dirty.java"),
        "final class Clean { void installHook() {} }",
        [Text.UTF8Encoding]::new($false))
    $clean = Invoke-Check $temp
    if ($clean.ExitCode -ne 0) {
        throw "Checker rejected clean source:`n$($clean.Output)"
    }

    [IO.File]::WriteAllText(
        (Join-Path $javaDir "Dirty.java"),
        "final class Dirty { Runnable value = () -> {}; }",
        [Text.UTF8Encoding]::new($false))
    $lambda = Invoke-Check $temp
    if ($lambda.ExitCode -eq 0 -or -not $lambda.Output.Contains("Java lambda")) {
        throw "Checker accepted a production Java lambda."
    }

    [IO.File]::WriteAllText((Join-Path $javaDir "Dirty.java"), "final class Clean {}")
    $kotlinDir = Join-Path $temp 'app/src/main/kotlin/com/example'
    [IO.Directory]::CreateDirectory($kotlinDir) | Out-Null
    $kotlinFile = Join-Path $kotlinDir 'Callback.kt'
    [IO.File]::WriteAllText($kotlinFile, 'val callback = { value: Int -> value }')
    $kotlinLambda = Invoke-Check $temp
    if ($kotlinLambda.ExitCode -ne 0) {
        throw "Checker rejected a Kotlin lambda: $($kotlinLambda.Output)"
    }
    [IO.File]::WriteAllText($kotlinFile, 'fun `m12lambda$0$example`() {}')
    $syntheticKotlin = Invoke-Check $temp
    if ($syntheticKotlin.ExitCode -eq 0 -or
        -not $syntheticKotlin.Output.Contains('app/src/main/kotlin/com/example/Callback.kt')) {
        throw 'Checker accepted a synthetic callback name in Kotlin.'
    }
    $text = [string][char]0x4E2D + [char]0x6587
    $garbled = [Text.Encoding]::GetEncoding(936).GetString([Text.Encoding]::UTF8.GetBytes($text))
    [IO.File]::WriteAllText($kotlinFile, "// $garbled")
    $kotlinMojibake = Invoke-Check $temp
    if ($kotlinMojibake.ExitCode -eq 0 -or -not $kotlinMojibake.Output.Contains('mojibake text')) {
        throw 'Checker accepted mojibake in Kotlin.'
    }
    [IO.File]::WriteAllText($kotlinFile, "// $text")
    foreach ($relative in @(
            "app/src/main/java/com/example/Boundary.java",
            "app/src/test/java/com/example/BoundaryTest.java",
            "tools/boundary.ps1",
            "build.gradle.kts")) {
        $boundary = Join-Path $temp $relative
        [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($boundary)) | Out-Null
        [IO.File]::WriteAllLines($boundary, [string[]](1..1500 | ForEach-Object { "// line" }))
        $atLimit = Invoke-Check $temp
        if ($atLimit.ExitCode -ne 0) {
            throw "Checker rejected 1500 lines in ${relative}: $($atLimit.Output)"
        }
        [IO.File]::AppendAllText($boundary, "// extra`n")
        $aboveLimit = Invoke-Check $temp
        if ($aboveLimit.ExitCode -eq 0 -or -not $aboveLimit.Output.Contains("1501 lines")) {
            throw "Checker accepted 1501 lines in $relative"
        }
        [IO.File]::WriteAllText($boundary, "// small again")
    }
} finally {
    $resolvedTemp = [IO.Path]::GetFullPath($temp)
    $expectedParent = [IO.Path]::GetFullPath((Join-Path $root '.local/tests')).TrimEnd('\')
    if ([IO.Path]::GetDirectoryName($resolvedTemp).TrimEnd('\') -ne $expectedParent `
            -or [IO.Path]::GetFileName($resolvedTemp) -notlike 'circular-maintainability-*') {
        throw "Unexpected maintenance test directory: $resolvedTemp"
    }
    if (Test-Path -LiteralPath $resolvedTemp) {
        Remove-Item -LiteralPath $resolvedTemp -Recurse -Force
    }
    if (Test-Path -LiteralPath $resolvedTemp) { throw "Tool self-test temporary directory remains" }
}

Write-Output "MaintainabilityCheckTest passed"
