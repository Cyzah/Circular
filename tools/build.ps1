[CmdletBinding(PositionalBinding = $false)]
param(
    [string]$ProjectDirectory = (Split-Path $PSScriptRoot -Parent),
    [Parameter(Position = 0, ValueFromRemainingArguments = $true)][string[]]$Tasks = @(':app:assembleGithubRelease')
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
foreach ($folder in @('gradle-home','android','tmp','runtime-home')) {
    [void][IO.Directory]::CreateDirectory((Join-Path $root ".local/$folder"))
}
$env:GRADLE_USER_HOME = Join-Path $root '.local/gradle-home'
$env:ANDROID_USER_HOME = Join-Path $root '.local/android'
$env:ANDROID_AVD_HOME = Join-Path $env:ANDROID_USER_HOME 'avd'
[void][IO.Directory]::CreateDirectory($env:ANDROID_AVD_HOME)
$env:TEMP = Join-Path $root '.local/tmp'
$env:TMP = $env:TEMP
$runtimeHome = (Join-Path $root '.local/runtime-home').Replace('\','/')
$tempPath = $env:TEMP.Replace('\','/')
$env:JAVA_TOOL_OPTIONS = "-Djava.io.tmpdir=`"$tempPath`" -Duser.home=`"$runtimeHome`" -Duser.language=en -Duser.country=US -XX:-UsePerfData"
$wrapperProperties = [IO.File]::ReadAllText((Join-Path $root 'gradle/wrapper/gradle-wrapper.properties'))
$gradleVersion = [regex]::Match($wrapperProperties, 'gradle-([0-9.]+)-bin\.zip').Groups[1].Value
if (-not $gradleVersion) { throw 'Cannot resolve the pinned Gradle version from the Wrapper.' }
$localGradle = Join-Path $root ".local/toolchains/gradle-$gradleVersion/bin/gradle.bat"
$launcher = if (Test-Path -LiteralPath $localGradle) { $localGradle } else { Join-Path $root 'gradlew.bat' }
if (!(Test-Path -LiteralPath $launcher -PathType Leaf)) { throw "Gradle launcher is unavailable: $launcher" }
$previousPreference = $ErrorActionPreference
$nativeExitCode = $null
try {
    # Windows PowerShell 5.1 turns harmless Java stderr into NativeCommandError when captured.
    $ErrorActionPreference = 'Continue'
    $global:LASTEXITCODE = $null
    & $launcher -p $ProjectDirectory @Tasks --no-daemon --console=plain 2>&1 |
        ForEach-Object { $_.ToString() }
    $nativeExitCode = $global:LASTEXITCODE
} finally { $ErrorActionPreference = $previousPreference }
if ($null -eq $nativeExitCode) { throw 'The Gradle launcher did not return an exit code.' }
exit $nativeExitCode
