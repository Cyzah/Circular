param(
    [Parameter(Mandatory = $true)][ValidateNotNullOrEmpty()][string]$Serial,
    [int]$TraceSeconds = 0,
    [int]$Cycles = 3,
    [switch]$KeepTrace
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$packageName = "com.cyzah.circular"
$activityName = "$packageName/.Launcher"
$systemUiPackage = "com.android.systemui"

function Get-AndroidSdk {
    $sdk = $env:ANDROID_HOME
    if (-not $sdk) {
        $sdk = $env:ANDROID_SDK_ROOT
    }
    if (-not $sdk) {
        $localProperties = Join-Path $root "local.properties"
        $sdkLine = Get-Content -LiteralPath $localProperties |
            Where-Object { $_ -match '^sdk\.dir=' } |
            Select-Object -First 1
        if (-not $sdkLine) {
            throw "Android SDK location is unavailable."
        }
        $sdk = $sdkLine.Substring("sdk.dir=".Length).Replace('\:', ':').Replace('\\', '\')
    }
    return $sdk
}

function Invoke-Adb {
    param([string[]]$Arguments)

    $previousErrorAction = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $exitCode = 1
    try {
        $output = & $script:adb -s $script:deviceSerial @Arguments 2>&1
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorAction
    }
    if ($exitCode -ne 0) {
        throw "adb failed: $($output -join [Environment]::NewLine)"
    }
    return ($output -join "`n")
}

function Get-FrameSummary {
    param([string]$PackageName)

    $text = Invoke-Adb @("shell", "dumpsys", "gfxinfo", $PackageName)
    function Get-Metric {
        param([string]$Pattern, [string]$Label)

        $match = [regex]::Match($text, $Pattern)
        if (-not $match.Success) {
            throw "Unable to parse $Label from gfxinfo for $PackageName."
        }
        return [double]::Parse(
            $match.Groups[1].Value,
            [Globalization.NumberStyles]::Float,
            [Globalization.CultureInfo]::InvariantCulture
        )
    }

    return [ordered]@{
        totalFrames = [int](Get-Metric 'Total frames rendered:\s+(\d+)' 'total frames')
        jankyFrames = Get-Metric 'Janky frames: \d+ \(([\d.]+)%\)' 'janky frames'
        p90Ms = [int](Get-Metric '90th percentile: (\d+)ms' 'p90')
        p95Ms = [int](Get-Metric '95th percentile: (\d+)ms' 'p95')
        p99Ms = [int](Get-Metric '99th percentile: (\d+)ms' 'p99')
        slowUiThread = [int](Get-Metric 'Number Slow UI thread:\s+(\d+)' 'slow UI thread count')
        missedVsync = [int](Get-Metric 'Number Missed Vsync:\s+(\d+)' 'missed Vsync count')
    }
}

function Get-ColdStartTimes {
    param([int]$Count)

    $times = [Collections.Generic.List[int]]::new()
    for ($index = 0; $index -lt $Count; $index++) {
        Invoke-Adb @("shell", "am", "force-stop", $packageName) | Out-Null
        Start-Sleep -Milliseconds 300
        $output = Invoke-Adb @("shell", "am", "start", "-W", "-n", $activityName)
        $match = [regex]::Match($output, "TotalTime:\s+(\d+)")
        if (-not $match.Success) {
            throw "Unable to parse settings cold-start time."
        }
        $times.Add([int]$match.Groups[1].Value)
        Start-Sleep -Milliseconds 500
    }
    return $times.ToArray()
}

$sdk = Get-AndroidSdk
$script:adb = Join-Path $sdk "platform-tools/adb.exe"
if (-not (Test-Path -LiteralPath $script:adb)) {
    throw "adb is unavailable."
}

$script:deviceSerial = $Serial

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$runId = [Guid]::NewGuid().ToString('N')
$outputDirectory = Join-Path $root ".local/performance/$timestamp-$runId"
[IO.Directory]::CreateDirectory($outputDirectory) | Out-Null
$remoteTrace = "/data/misc/perfetto-traces/circular-$runId.pftrace"
$localTrace = Join-Path $outputDirectory "circular.pftrace"
$traceStarted = $false
$traceAttempted = $false
$traceProcessId = $null
$deviceVerified = $false
$scratchDirectory = Join-Path $root ".local/tests/profile-$runId"
$environmentBefore = @{}
foreach ($key in @('TEMP', 'TMP', 'ANDROID_USER_HOME')) {
    $environmentBefore[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
}

try {
    [void][IO.Directory]::CreateDirectory($scratchDirectory)
    $env:TEMP = $scratchDirectory
    $env:TMP = $scratchDirectory
    $env:ANDROID_USER_HOME = Join-Path $scratchDirectory 'android'
    [void][IO.Directory]::CreateDirectory($env:ANDROID_USER_HOME)
    $actualSerial = Invoke-Adb @('get-serialno')
    if ($actualSerial.Trim() -ne $Serial) { throw 'Explicit device identity does not match; confirm before retrying' }
    $deviceVerified = $true
    Invoke-Adb @("shell", "cmd", "statusbar", "collapse") | Out-Null
    Invoke-Adb @("shell", "dumpsys", "gfxinfo", $systemUiPackage, "reset") | Out-Null
    for ($index = 0; $index -lt [Math]::Max(1, $Cycles); $index++) {
        Invoke-Adb @("shell", "cmd", "statusbar", "expand-notifications") | Out-Null
        Start-Sleep -Milliseconds 400
        if ($deviceVerified) { Invoke-Adb @("shell", "cmd", "statusbar", "collapse") | Out-Null }
        Start-Sleep -Milliseconds 400
    }
    $systemUiFrames = Get-FrameSummary $systemUiPackage

    $coldStarts = Get-ColdStartTimes 5
    $averageColdStart = [Math]::Round(
        ($coldStarts | Measure-Object -Average).Average,
        1
    )
    $settingsFrames = Get-FrameSummary $packageName

    if ($TraceSeconds -gt 0) {
        $traceAttempted = $true
        $traceProcessId = Invoke-Adb @(
            "shell", "perfetto", "--background-wait",
            "-o", $remoteTrace,
            "-t", "$($TraceSeconds)s",
            "-b", "64mb",
            "sched", "freq", "gfx", "view", "wm", "am", "binder_driver"
        )
        $traceStarted = $true
        Start-Sleep -Milliseconds 800
        for ($index = 0; $index -lt [Math]::Max(1, $Cycles); $index++) {
            Invoke-Adb @("shell", "cmd", "statusbar", "expand-notifications") | Out-Null
            Start-Sleep -Milliseconds 450
            if ($deviceVerified) { Invoke-Adb @("shell", "cmd", "statusbar", "collapse") | Out-Null }
            Start-Sleep -Milliseconds 450
        }
        Start-Sleep -Seconds ([Math]::Max(1, $TraceSeconds))
        Invoke-Adb @("pull", $remoteTrace, $localTrace) | Out-Null
    }

    $summary = [ordered]@{
        timestamp = $timestamp
        serial = $Serial
        systemUi = $systemUiFrames
        settingsColdStartMs = $coldStarts
        settingsColdStartAverageMs = $averageColdStart
        settings = $settingsFrames
        perfettoTrace = if ($traceStarted -and $KeepTrace) { $localTrace } else { $null }
    }
    $summary | ConvertTo-Json -Depth 5 |
        Set-Content -LiteralPath (Join-Path $outputDirectory "summary.json") -Encoding UTF8

    @(
        "serial=$Serial"
        "systemUi.frames=$($systemUiFrames.totalFrames)"
        "systemUi.jankyPercent=$($systemUiFrames.jankyFrames)"
        "systemUi.p90Ms=$($systemUiFrames.p90Ms)"
        "systemUi.p95Ms=$($systemUiFrames.p95Ms)"
        "systemUi.p99Ms=$($systemUiFrames.p99Ms)"
        "settings.coldStartMs=$($coldStarts -join ',')"
        "settings.coldStartAverageMs=$averageColdStart"
        "settings.frames=$($settingsFrames.totalFrames)"
        "settings.jankyPercent=$($settingsFrames.jankyFrames)"
        "settings.p90Ms=$($settingsFrames.p90Ms)"
        "settings.p95Ms=$($settingsFrames.p95Ms)"
        "settings.p99Ms=$($settingsFrames.p99Ms)"
        "trace=$($summary.perfettoTrace)"
    ) | Set-Content -LiteralPath (Join-Path $outputDirectory "summary.txt") -Encoding UTF8

    Write-Host "Performance profile written to $outputDirectory"
}
finally {
    $cleanupErrors = [Collections.Generic.List[string]]::new()
    try {
        if ($deviceVerified) { Invoke-Adb @("shell", "cmd", "statusbar", "collapse") | Out-Null }
    } catch {
        $cleanupErrors.Add("restore status bar: $($_.Exception.Message)")
    }
    try {
        if ($traceAttempted) {
            # Stop only a still-running trace whose command includes this run's unique path.
            if ($traceProcessId -match '^\s*(\d+)\s*$') {
                $perfettoPid = $Matches[1]
                try {
                    Invoke-Adb @('shell', "if grep -F -q -- '$remoteTrace' /proc/$perfettoPid/cmdline 2>/dev/null; then kill -9 $perfettoPid; fi") | Out-Null
                } catch {
                    $cleanupErrors.Add("stop trace process: $($_.Exception.Message)")
                }
            }
            Invoke-Adb @("shell", "rm", "-f", $remoteTrace) | Out-Null
            Invoke-Adb @("shell", "test", "!", "-e", $remoteTrace) | Out-Null
        }
    } catch {
        $cleanupErrors.Add("remove trace $remoteTrace : $($_.Exception.Message)")
    } finally {
        foreach ($key in $environmentBefore.Keys) {
            if ($null -eq $environmentBefore[$key]) {
                Remove-Item -LiteralPath "Env:$key" -ErrorAction SilentlyContinue
            } else {
                [Environment]::SetEnvironmentVariable($key, $environmentBefore[$key], 'Process')
            }
        }
        if (-not $KeepTrace -and (Test-Path -LiteralPath $localTrace)) {
            Remove-Item -LiteralPath $localTrace -Force
        }
        if (-not $KeepTrace -and (Test-Path -LiteralPath $localTrace)) { throw 'Local performance trace remains.' }
        $resolvedScratch = [IO.Path]::GetFullPath($scratchDirectory)
        $expectedRoot = [IO.Path]::GetFullPath((Join-Path $root '.local/tests')).TrimEnd('\') + '\'
        if (-not $resolvedScratch.StartsWith($expectedRoot, [StringComparison]::OrdinalIgnoreCase) -or
            [IO.Path]::GetFileName($resolvedScratch) -ne "profile-$runId") {
            throw "Refusing to clean an unexpected test directory: $resolvedScratch"
        }
        if (Test-Path -LiteralPath $resolvedScratch) {
            Remove-Item -LiteralPath $resolvedScratch -Recurse -Force
        }
        if (Test-Path -LiteralPath $resolvedScratch) { throw "Performance-test temporary directory remains: $resolvedScratch" }
    }
    if ($cleanupErrors.Count -gt 0) { throw "Device cleanup failed: $($cleanupErrors -join '; ')" }
}
