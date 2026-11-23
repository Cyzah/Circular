$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$versionFile = Join-Path $root "version.properties"
$bumpSource = Join-Path $PSScriptRoot "VersionBump.java"

$scratch = Join-Path $root ('.local/tmp/version-bump-' + [Guid]::NewGuid().ToString('N'))
$environmentBefore = @{}
foreach ($key in @('TEMP', 'TMP', 'JAVA_TOOL_OPTIONS')) {
    $environmentBefore[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
}
try {
    [void][IO.Directory]::CreateDirectory($scratch)
    $env:TEMP = $scratch
    $env:TMP = $scratch
    $javaHome = $scratch.Replace('\', '/')
    $env:JAVA_TOOL_OPTIONS = "-Djava.io.tmpdir=`"$javaHome`" -Duser.home=`"$javaHome`" -XX:-UsePerfData"
    & java $bumpSource $versionFile
    if ($LASTEXITCODE -ne 0) { throw "Version bump failed." }
} finally {
    foreach ($key in $environmentBefore.Keys) {
        if ($null -eq $environmentBefore[$key]) {
            Remove-Item -LiteralPath "Env:$key" -ErrorAction SilentlyContinue
        } else {
            [Environment]::SetEnvironmentVariable($key, $environmentBefore[$key], 'Process')
        }
    }
    $resolvedScratch = [IO.Path]::GetFullPath($scratch)
    $expectedParent = [IO.Path]::GetFullPath((Join-Path $root '.local/tmp')).TrimEnd('\')
    if ([IO.Path]::GetDirectoryName($resolvedScratch).TrimEnd('\') -ne $expectedParent -or
        [IO.Path]::GetFileName($resolvedScratch) -notlike 'version-bump-*') {
        throw "Refusing to clean an unexpected version directory: $resolvedScratch"
    }
    if (Test-Path -LiteralPath $resolvedScratch) { Remove-Item -LiteralPath $resolvedScratch -Recurse -Force }
}
