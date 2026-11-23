$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$parent = [IO.Path]::GetFullPath((Join-Path $projectRoot '.local/tests')).TrimEnd('\')
$runId = [Guid]::NewGuid().ToString('N')
$testRoot = Join-Path $parent "test-environment-$runId"
$before = @{}
foreach ($name in @('TEMP','TMP')) { $before[$name] = [Environment]::GetEnvironmentVariable($name,'Process') }
function Write-Fixture([string]$Path,[string]$Text) {
    [void][IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($Path))
    [IO.File]::WriteAllText($Path,$Text,[Text.UTF8Encoding]::new($false))
}
function Assert-Equal([string]$Expected,[string]$Actual,[string]$Label) {
    if ($Expected -cne $Actual) { throw "$Label expected '$Expected', got '$Actual'." }
}
function Assert-Rejected([scriptblock]$Action,[string]$Label) {
    $rejected = $false
    try { & $Action | Out-Null } catch { $rejected = $true }
    if (-not $rejected) { throw "$Label was unexpectedly accepted." }
}
try {
    [void][IO.Directory]::CreateDirectory($testRoot)
    $env:TEMP = $testRoot
    $env:TMP = $testRoot
    . (Join-Path $projectRoot 'tools/TestEnvironment.ps1')
    $sdk = Join-Path $testRoot 'sdk'
    $config = Join-Path $testRoot 'app/build.gradle.kts'
    $nl = [Environment]::NewLine
    Write-Fixture $config ('compileSdk = 37' + $nl + 'compileSdkMinor = 2')
    Write-Fixture (Join-Path $testRoot 'local.properties') ('sdk.dir=' + $sdk.Replace('\','/'))
    $oldJar = Join-Path $sdk 'platforms/android-37.0/android.jar'
    $jar = Join-Path $sdk 'platforms/android-37.2/android.jar'
    Write-Fixture $oldJar 'wrong minor'
    Assert-Rejected { Get-CircularTestAndroidJar -Root $testRoot } 'Wrong SDK minor fallback'
    Write-Fixture $jar 'expected minor'
    Assert-Equal $jar (Get-CircularTestAndroidJar -Root $testRoot) 'Exact SDK minor'
    $d8 = Join-Path $sdk 'build-tools/36.0.0/d8.bat'
    Assert-Rejected { Get-CircularTestD8 -Root $testRoot } 'Missing Build Tools'
    Write-Fixture $d8 'test fixture only'
    Assert-Equal $d8 (Get-CircularTestD8 -Root $testRoot) 'Pinned default Build Tools'
    Write-Fixture $config ('compileSdk = 37' + $nl + 'compileSdkMinor = 2' + $nl + 'buildToolsVersion = "99.0.0"')
    Assert-Rejected { Get-CircularTestD8 -Root $testRoot } 'Explicit Build Tools must not fall back'
    $explicitD8 = Join-Path $sdk 'build-tools/99.0.0/d8.bat'
    Write-Fixture $explicitD8 'test fixture only'
    Assert-Equal $explicitD8 (Get-CircularTestD8 -Root $testRoot) 'Explicit Build Tools'
    Write-Fixture (Join-Path $testRoot 'gradle/libs.versions.toml') 'libxposed = "102.0.0"'
    $task = Join-Path $testRoot '.local/tests/api-extraction'
    [void][IO.Directory]::CreateDirectory($task)
    Assert-Rejected { Get-CircularTestApiJar -Root $testRoot -TemporaryDirectory $task } 'Missing project AAR cache'
    $aar = Join-Path $testRoot '.local/gradle-home/caches/modules-2/files-2.1/io.github.libxposed/api/102.0.0/fixture/api-102.0.0.aar'
    [void][IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($aar))
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::Open($aar,[IO.Compression.ZipArchiveMode]::Create)
    try {
        $stream = $archive.CreateEntry('classes.jar').Open()
        try {
            $bytes = [Text.Encoding]::UTF8.GetBytes('expected cached API bytes')
            $stream.Write($bytes,0,$bytes.Length)
        } finally { $stream.Dispose() }
    } finally { $archive.Dispose() }
    Assert-Rejected { Get-CircularTestApiJar -Root $testRoot -TemporaryDirectory $testRoot } 'Extraction outside task scratch'
    $api = Get-CircularTestApiJar -Root $testRoot -TemporaryDirectory $task
    Assert-Equal 'expected cached API bytes' ([IO.File]::ReadAllText($api)) 'Cached AAR classes.jar extraction'
    Assert-Rejected { Get-CircularTestApiJar -Root $testRoot -TemporaryDirectory $task } 'Do not overwrite an existing extracted file'
    Assert-Equal 'expected cached API bytes' ([IO.File]::ReadAllText($api)) 'Existing extracted file preserved'
    $appClasses = Join-Path $testRoot 'app/build/intermediates/compile_app_classes_jar/githubDebug/bundleGithubDebugClassesToCompileJar/classes.jar'
    Assert-Rejected { Get-CircularTestAppClassesJar -Root $testRoot } 'Missing Debug app classes JAR'
    Write-Fixture $appClasses 'application classes'
    Assert-Equal $appClasses (Get-CircularTestAppClassesJar -Root $testRoot) 'Debug app classes JAR'
    $repoAppClasses = Join-Path $testRoot 'app/build/intermediates/compile_app_classes_jar/lspRepoDebug/bundleLspRepoDebugClassesToCompileJar/classes.jar'
    Assert-Rejected { Get-CircularTestAppClassesJar -Root $testRoot -Edition lspRepo } 'Repository Debug classes must not fall back to GitHub classes'
    Write-Fixture $repoAppClasses 'repository application classes'
    Assert-Equal $repoAppClasses (Get-CircularTestAppClassesJar -Root $testRoot -Edition lspRepo) 'Repository Debug app classes JAR'
    $versionCatalog = Join-Path $testRoot 'gradle/libs.versions.toml'
    Write-Fixture $versionCatalog ('libxposed = "102.0.0"' + $nl + 'kotlin = "9.8.7"')
    $cache = Join-Path $testRoot '.local/gradle-home/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib'
    Write-Fixture (Join-Path $cache '8.7.6/fixture/kotlin-stdlib-8.7.6.jar') 'different Kotlin version'
    Assert-Rejected { Get-CircularTestKotlinStdlib -Root $testRoot } 'Missing catalog stdlib version'
    $stdlib = Join-Path $cache '9.8.7/fixture/kotlin-stdlib-9.8.7.jar'
    Write-Fixture $stdlib 'catalog stdlib version'
    Assert-Equal $stdlib (Get-CircularTestKotlinStdlib -Root $testRoot) 'Catalog Kotlin stdlib'
    Write-Output 'TestEnvironmentTest passed'
} finally {
    foreach ($name in $before.Keys) { [Environment]::SetEnvironmentVariable($name,$before[$name],'Process') }
    $resolved = [IO.Path]::GetFullPath($testRoot)
    if ([IO.Path]::GetDirectoryName($resolved).TrimEnd('\') -ne $parent -or
        [IO.Path]::GetFileName($resolved) -ne "test-environment-$runId") { throw 'Unsafe test-environment cleanup target' }
    if (Test-Path -LiteralPath $resolved) { Remove-Item -LiteralPath $resolved -Recurse -Force }
    if (Test-Path -LiteralPath $resolved) { throw 'Test-environment temporary directory remains' }
}
