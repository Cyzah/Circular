# Shared read-only tool/dependency lookup. Never downloads or invokes Gradle.
function Get-CircularTestSdk {
    param([Parameter(Mandatory=$true)][string]$Root)
    $sdk = $null
    $properties = Join-Path $Root 'local.properties'
    if (Test-Path -LiteralPath $properties) {
        $line = Get-Content -LiteralPath $properties | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if ($line) { $sdk = $line.Substring(8).Replace('\:', ':').Replace('\\', '\') }
    }
    if (-not $sdk) { $sdk = $env:ANDROID_HOME }
    if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
    if (-not $sdk -or -not (Test-Path -LiteralPath $sdk -PathType Container)) {
        throw 'Prepare the Android SDK using the build guide and set local.properties or ANDROID_HOME.'
    }
    return [IO.Path]::GetFullPath($sdk)
}
function Get-CircularTestAndroidJar {
    param([Parameter(Mandatory=$true)][string]$Root)
    $sdk = Get-CircularTestSdk -Root $Root
    $config = [IO.File]::ReadAllText((Join-Path $Root 'app/build.gradle.kts'))
    $major = [regex]::Match($config, 'compileSdk\s*=\s*(\d+)').Groups[1].Value
    if (-not $major) { throw 'Cannot resolve compileSdk from app/build.gradle.kts.' }
    $minor = [regex]::Match($config, 'compileSdkMinor\s*=\s*(\d+)').Groups[1].Value
    $platform = if ($minor -and [int]$minor -gt 0) { "$major.$minor" } else { $major }
    $jar = Join-Path $sdk "platforms/android-$platform/android.jar"
    if (-not (Test-Path -LiteralPath $jar -PathType Leaf) -and $platform -eq $major) {
        $jar = Join-Path $sdk "platforms/android-$major.0/android.jar"
    }
    if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) { throw "Required Android platform jar is missing: $jar" }
    return $jar
}
function Get-CircularTestD8 {
    param([Parameter(Mandatory=$true)][string]$Root)
    $sdk = Get-CircularTestSdk -Root $Root
    $config = [IO.File]::ReadAllText((Join-Path $Root 'app/build.gradle.kts'))
    $version = [regex]::Match($config, 'buildToolsVersion\s*=\s*"([^"]+)"').Groups[1].Value
    # The AGP 9.5 series defaults to Build Tools 36.0.0.
    if (-not $version) { $version = '36.0.0' }
    $d8 = Join-Path $sdk "build-tools/$version/d8.bat"
    if (-not (Test-Path -LiteralPath $d8 -PathType Leaf)) { throw "Required Build Tools d8 is missing: $d8" }
    return $d8
}
function Get-CircularTestCacheArtifact {
    param(
        [Parameter(Mandatory=$true)][string]$Root,
        [Parameter(Mandatory=$true)][string]$Group,
        [Parameter(Mandatory=$true)][string]$Module,
        [Parameter(Mandatory=$true)][string]$Version,
        [Parameter(Mandatory=$true)][string]$FileName,
        [string]$GradleUserHome = ''
    )
    if (-not $GradleUserHome) { $GradleUserHome = Join-Path $Root '.local/gradle-home' }
    $directory = Join-Path $GradleUserHome "caches/modules-2/files-2.1/$Group/$Module/$Version"
    $artifact = $null
    if (Test-Path -LiteralPath $directory -PathType Container) {
        $artifact = Get-ChildItem -LiteralPath $directory -Recurse -File -Filter $FileName |
            Sort-Object FullName | Select-Object -First 1 -ExpandProperty FullName
    }
    if (-not $artifact) { throw "Missing project-cached $Group/$Module/$Version/$FileName. Complete the explicit Debug build using tools/build.ps1 first." }
    return $artifact
}
function Get-CircularTestApiJar {
    param(
        [Parameter(Mandatory=$true)][string]$Root,
        [Parameter(Mandatory=$true)][string]$TemporaryDirectory,
        [string]$GradleUserHome = ''
    )
    $directory = [IO.Path]::GetFullPath($TemporaryDirectory)
    $safeRoot = [IO.Path]::GetFullPath((Join-Path $Root '.local/tests')).TrimEnd('\') + '\'
    if (-not $directory.StartsWith($safeRoot, [StringComparison]::OrdinalIgnoreCase) -or
        -not (Test-Path -LiteralPath $directory -PathType Container)) { throw 'API extraction requires an existing task directory under project .local/tests.' }
    $versions = [IO.File]::ReadAllText((Join-Path $Root 'gradle/libs.versions.toml'))
    $version = [regex]::Match($versions, '(?m)^libxposed\s*=\s*"([^"]+)"').Groups[1].Value
    if (-not $version) { throw 'libxposed version is missing from the version catalog.' }
    $aar = Get-CircularTestCacheArtifact -Root $Root -Group 'io.github.libxposed' -Module 'api' -Version $version -FileName "api-$version.aar" -GradleUserHome $GradleUserHome
    $jar = Join-Path $directory "libxposed-api-$version.jar"
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($aar)
    try {
        $entry = $archive.GetEntry('classes.jar')
        if (-not $entry) { throw 'Cached libxposed AAR has no classes.jar.' }
        $source = $entry.Open()
        try {
            $destination = [IO.File]::Open($jar, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write)
            try { $source.CopyTo($destination) } finally { $destination.Dispose() }
        } finally { $source.Dispose() }
    } finally { $archive.Dispose() }
    return $jar
}
function Get-CircularTestAppClassesJar {
    param(
        [Parameter(Mandatory=$true)][string]$Root,
        [ValidateSet('github', 'lspRepo')][string]$Edition = 'github'
    )
    $variant = if ($Edition -eq 'lspRepo') { 'lspRepoDebug' } else { 'githubDebug' }
    $taskVariant = $variant.Substring(0, 1).ToUpperInvariant() + $variant.Substring(1)
    $jar = Join-Path $Root "app/build/intermediates/compile_app_classes_jar/$variant/bundle$($taskVariant)ClassesToCompileJar/classes.jar"
    if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) {
        throw "Missing app classes JAR for $variant. Run :app:test$($taskVariant)UnitTest with tools/build.ps1 first."
    }
    return $jar
}
function Get-CircularTestKotlinStdlib {
    param(
        [Parameter(Mandatory=$true)][string]$Root,
        [string]$GradleUserHome = ''
    )
    $versions = [IO.File]::ReadAllText((Join-Path $Root 'gradle/libs.versions.toml'))
    $version = [regex]::Match($versions, '(?m)^kotlin\s*=\s*"([^"]+)"').Groups[1].Value
    if (-not $version) { throw 'Kotlin version is missing from the version catalog.' }
    return Get-CircularTestCacheArtifact -Root $Root -Group 'org.jetbrains.kotlin' -Module 'kotlin-stdlib' -Version $version -FileName "kotlin-stdlib-$version.jar" -GradleUserHome $GradleUserHome
}
