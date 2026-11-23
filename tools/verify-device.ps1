param(
    [Parameter(Mandatory = $true)][ValidateNotNullOrEmpty()][string]$Serial,
    [int]$AdbPort = 5037,
    [ValidateSet('github', 'lspRepo')][string]$Edition = 'github'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$isGithub = $Edition -eq 'github'
$package = if ($isGithub) { 'com.cyzah.circular' } else { 'io.github.cyzah.circular' }
$environmentBefore = @{}
foreach ($key in @('TEMP', 'TMP', 'ANDROID_USER_HOME')) {
    $environmentBefore[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
}
$runId = [Guid]::NewGuid().ToString('N')
$versionProperties = ConvertFrom-StringData (Get-Content -LiteralPath (Join-Path $root 'version.properties') -Raw)
$version = "$($versionProperties.major).$($versionProperties.minor).$($versionProperties.patch)"
$archiveHelpers = Join-Path $PSScriptRoot 'VerificationArchive.ps1'
. $archiveHelpers
. (Join-Path $PSScriptRoot 'TestEnvironment.ps1')
$verificationPaths = New-DeviceVerificationPaths `
    -Root $root `
    -Version $version `
    -RunId $runId `
    -Timestamp (Get-Date -Format 'yyyyMMdd-HHmmss')
$runDirectory = $verificationPaths.EvidenceDirectory
$scratchDirectory = $verificationPaths.ScratchDirectory
$transcriptPath = $verificationPaths.TranscriptPath
$transcriptStarted = $false
$evidenceDirectoryCreated = $false
$remote = "/sdcard/circular-ui-$runId.xml"
$script:dumpNumber = 0
$deviceVerified = $false
$lastTab = if ($isGithub) { '工具' } else { '关于' }
$lastPageMarker = if ($isGithub) { '诊断与重载' } else { '关于 Circular' }
$labels = @('预览', '外观', '显示', $lastTab)
$pageMarkers = @{ '预览' = '启用Circular'; '外观' = '双卡样式'; '显示' = '下拉显示' }
$pageMarkers[$lastTab] = $lastPageMarker
$toolsActions = @('复制诊断信息', '导出 SystemUI 适配包', '分享适配包', '导出说明')

function Invoke-Adb([string[]]$Arguments) {
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = & $adb -P $AdbPort -s $Serial @Arguments 2>&1
        $code = $LASTEXITCODE
    } finally { $ErrorActionPreference = $previousPreference }
    $text = ($output | ForEach-Object { $_.ToString() }) -join [Environment]::NewLine
    if ($code -ne 0) { throw "adb exited $code : $text" }
    return $text
}
function Assert-Ui([bool]$Condition, [string]$Description) {
    if (!$Condition) { throw "Settings UI verification failed: $Description" }
    Write-Output "PASS $Description"
}
function Get-Bounds([string]$Value) {
    if ($Value -notmatch '^\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]$') {
        throw "Unexpected bounds: $Value"
    }
    @([int]$Matches[1], [int]$Matches[2], [int]$Matches[3], [int]$Matches[4])
}
function Get-Ui {
    $script:dumpNumber++
    $path = Join-Path $scratchDirectory ("window-{0:D3}.xml" -f $script:dumpNumber)
    for ($attempt = 0; $attempt -lt 4; $attempt++) {
        try {
            Invoke-Adb @('shell','rm','-f',$remote) | Out-Null
            $result = Invoke-Adb @('shell','uiautomator','dump',$remote)
            if ($result -notmatch 'dumped to:') { throw "No fresh accessibility dump: $result" }
            Invoke-Adb @('pull',$remote,$path) | Out-Null
            $ui = [xml](Get-Content -LiteralPath $path -Raw -Encoding UTF8)
            if ($null -ne $ui.SelectSingleNode("//node[@package='$package']")) { return $ui }
        } catch {
            if ($attempt -eq 3) { throw }
        }
        Start-Sleep -Milliseconds 300
    }
    throw 'Application accessibility window was not available.'
}
function Get-SemanticNode($Ui, [string]$Label, [switch]$Actionable) {
    foreach ($node in $Ui.SelectNodes("//node[@package='$package']")) {
        # A clickable preference can merge its title and summary into one multiline node.
        $nodeLabels = @($node.GetAttribute('text') -split '\r?\n') +
            @($node.GetAttribute('content-desc') -split '\r?\n')
        if ($Label -notin $nodeLabels) { continue }
        $candidate = $node
        # Compose may expose a label separately from its ancestor's action/selection.
        # An already selected Tab has selected=true but no click action.
        if ($Actionable) {
            while ($candidate -and $candidate.Name -eq 'node' -and
                   $candidate.GetAttribute('package') -eq $package -and
                   $candidate.GetAttribute('clickable') -ne 'true' -and
                   $candidate.GetAttribute('selected') -ne 'true') {
                $candidate = $candidate.ParentNode
            }
        }
        if ($candidate -and $candidate.Name -eq 'node' -and
            $candidate.GetAttribute('package') -eq $package -and
            $candidate.GetAttribute('enabled') -ne 'false') {
            $b = Get-Bounds $candidate.bounds
            if ($b[2] -gt $b[0] -and $b[3] -gt $b[1]) { return $candidate }
        }
    }
}
function Invoke-Tap($Node) {
    if ($null -eq $Node) { throw 'Clickable semantic node is missing.' }
    $b = Get-Bounds $Node.bounds
    Invoke-Adb @('shell','input','tap',"$([int](($b[0]+$b[2])/2))","$([int](($b[1]+$b[3])/2))") | Out-Null
}
function Get-VisibleContentNodes($Ui) {
    $bottom = [int]::MaxValue
    foreach ($label in $labels) {
        $nav = Get-SemanticNode $Ui $label -Actionable
        if ($nav) { $bottom = [Math]::Min($bottom, (Get-Bounds $nav.bounds)[1]) }
    }
    $scroll = Get-ScrollNode $Ui
    # A short page can fit entirely and correctly expose no scroll action.
    $contentNodes = if ($scroll) { $scroll.SelectNodes('.//node') }
        else { $Ui.SelectNodes("//node[@package='$package']") }
    foreach ($node in $contentNodes) {
        $b = Get-Bounds $node.bounds
        if ($b[2] -gt $b[0] -and $b[3] -gt $b[1] -and $b[3] -gt 0 -and $b[1] -lt $bottom) { $node }
    }
}
function Get-SemanticText($Ui) {
    $parts = foreach ($node in (Get-VisibleContentNodes $Ui)) {
        $node.GetAttribute('text')
        $node.GetAttribute('content-desc')
    }
    (($parts -join ' ') -replace '\s+', ' ').Trim()
}
function Assert-LastPageActions($Ui) {
    if ($isGithub) { return }
    foreach ($action in $toolsActions) {
        if (Get-SemanticNode $Ui $action) {
            throw "Settings UI verification failed: $Edition shows GitHub-only action '$action'."
        }
    }
}
function Get-ScrollNode($Ui) {
    $Ui.SelectNodes("//node[@package='$package' and @scrollable='true']") |
        Sort-Object { $b = Get-Bounds $_.bounds; ($b[2]-$b[0])*($b[3]-$b[1]) } -Descending |
        Select-Object -First 1
}
function Get-ScrollAnchors($Ui) {
    $anchors = @{}
    $scroll = Get-ScrollNode $Ui
    if ($null -eq $scroll) { return $anchors }
    foreach ($node in (Get-VisibleContentNodes $Ui)) {
        $key = ($node.GetAttribute('text') + ' ' + $node.GetAttribute('content-desc')).Trim()
        if ($key -and $key -notin $labels) {
            $b = Get-Bounds $node.bounds
            if ($b[2] -gt $b[0] -and $b[3] -gt $b[1]) { $anchors[$key] = $b[1] }
        }
    }
    return $anchors
}
function Invoke-PageScroll($Ui, [switch]$ToTop) {
    $scroll = Get-ScrollNode $Ui
    if ($null -eq $scroll) { throw 'No scrollable accessibility node was found.' }
    $b = Get-Bounds $scroll.bounds
    foreach ($label in $labels) {
        $nav = Get-SemanticNode $Ui $label -Actionable
        if ($nav) { $b[3] = [Math]::Min($b[3], (Get-Bounds $nav.bounds)[1]) }
    }
    if ($b[3] - $b[1] -lt 80) { throw 'No usable content area for a swipe.' }
    # Swipe within the content's side padding, away from sliders and the fixed preview.
    $x = [int]($b[0] + ($b[2]-$b[0])*0.04)
    $high = [int]($b[1]+($b[3]-$b[1])*0.50)
    $low = [int]($b[1]+($b[3]-$b[1])*0.85)
    $from = if ($ToTop) { $high } else { $low }
    $to = if ($ToTop) { $low } else { $high }
    Invoke-Adb @('shell','input','swipe',"$x","$from","$x","$to",'350') | Out-Null
    Start-Sleep -Milliseconds 350
}
function Find-LastPageAction([string]$Label) {
    $ui = Get-Ui
    for ($attempt = 0; $attempt -lt 12; $attempt++) {
        if ((Get-SemanticText $ui).Contains($lastPageMarker) -or $null -eq (Get-ScrollNode $ui)) { break }
        Invoke-PageScroll $ui -ToTop
        $ui = Get-Ui
    }
    for ($attempt = 0; $attempt -lt 12; $attempt++) {
        Assert-LastPageActions $ui
        $node = Get-SemanticNode $ui $Label -Actionable
        if ($node) { return $node }
        if ($null -eq (Get-ScrollNode $ui)) { break }
        Invoke-PageScroll $ui
        $ui = Get-Ui
    }
    throw "Action on the '$lastTab' tab is missing or disabled: $Label"
}

try {
    [void][IO.Directory]::CreateDirectory($scratchDirectory)
    [void][IO.Directory]::CreateDirectory($runDirectory)
    $evidenceDirectoryCreated = $true
    Start-Transcript -LiteralPath $transcriptPath -NoClobber | Out-Null
    $transcriptStarted = $true
    $env:TEMP = $scratchDirectory
    $env:TMP = $scratchDirectory
    $env:ANDROID_USER_HOME = Join-Path $scratchDirectory 'android'
    [void][IO.Directory]::CreateDirectory($env:ANDROID_USER_HOME)
    $sdk = Get-CircularTestSdk -Root $root
    $adb = Join-Path $sdk 'platform-tools/adb.exe'
    if (!(Test-Path -LiteralPath $adb)) { throw 'adb is unavailable.' }
    $actualSerial = Invoke-Adb @('get-serialno')
    if ($actualSerial.Trim() -ne $Serial) { throw 'Explicit device identity does not match; confirm before retrying' }
    $deviceVerified = $true
    Write-Output "Checking the $Edition settings UI on device $Serial"
    Invoke-Adb @('shell','input','keyevent','KEYCODE_WAKEUP') | Out-Null
    Invoke-Adb @('shell','wm','dismiss-keyguard') | Out-Null
    Invoke-Adb @('shell','cmd','statusbar','collapse') | Out-Null
    $launch = Invoke-Adb @('shell','am','start','-W','-n',
        "$package/com.cyzah.circular.ui.activity.MainActivity")
    Assert-Ui ($launch -notmatch 'Error:|Exception|Status:\s*(timeout|error)') 'MainActivity launched directly'
    Start-Sleep -Milliseconds 800
    $ui = Get-Ui
    $notice = Get-SemanticNode $ui '明白'
    if ($notice) {
        Assert-Ui ($null -ne (Get-SemanticNode $ui '使用须知')) 'startup notice uses the current product title'
        # Dismiss the first-run notice without changing its opt-out preference.
        Invoke-Tap $notice
        Start-Sleep -Milliseconds 350
        $ui = Get-Ui
    }
    foreach ($label in $labels) {
        Assert-Ui ($null -ne (Get-SemanticNode $ui $label -Actionable)) "navigation semantic label: $label"
    }
    if (!$isGithub) {
        Assert-Ui ($null -eq (Get-SemanticNode $ui '工具' -Actionable)) 'repository navigation omits the Tools tab'
    }
    foreach ($label in $labels) {
        Invoke-Tap (Get-SemanticNode $ui $label -Actionable)
        for ($attempt = 0; $attempt -lt 4; $attempt++) {
            Start-Sleep -Milliseconds 250
            $ui = Get-Ui
            $selected = Get-SemanticNode $ui $label -Actionable
            if ($selected -and $selected.GetAttribute('selected') -eq 'true') { break }
        }
        Assert-Ui ($selected -and $selected.GetAttribute('selected') -eq 'true') "navigation selects: $label"
        $otherSelected = @($labels | Where-Object { $_ -ne $label } | ForEach-Object {
            $node = Get-SemanticNode $ui $_ -Actionable
            if ($node -and $node.GetAttribute('selected') -eq 'true') { $_ }
        })
        Assert-Ui ($otherSelected.Count -eq 0) "navigation selection is exclusive: $label"
        $marker = $pageMarkers[$label]
        for ($topAttempt = 0; $topAttempt -lt 8; $topAttempt++) {
            if ((Get-SemanticText $ui).Contains($marker)) { break }
            if ($null -eq (Get-ScrollNode $ui)) { break }
            Invoke-PageScroll $ui -ToTop
            $ui = Get-Ui
        }
        Assert-Ui ((Get-SemanticText $ui).Contains($marker)) "page content: $marker"
    }

    $targets = @('关于 Circular', '开发与维护', '使用须知', '参考与开源说明', '重启系统界面')
    if ($isGithub) {
        $targets += @('诊断与重载', '复制诊断信息', '机型适配包', '导出 SystemUI 适配包', '导出说明')
    }
    $seen = @{}
    $scrollVerified = $false
    $scrollRequired = $false
    # Return to the top first because Compose preserves a page's previous offset.
    for ($attempt = 0; $attempt -lt 6; $attempt++) {
        if ((Get-SemanticText $ui).Contains($lastPageMarker) -or $null -eq (Get-ScrollNode $ui)) { break }
        Invoke-PageScroll $ui -ToTop
        $ui = Get-Ui
    }
    for ($attempt = 0; $attempt -lt 12; $attempt++) {
        Assert-LastPageActions $ui
        $text = Get-SemanticText $ui
        foreach ($target in $targets) { if ($text.Contains($target)) { $seen[$target] = $true } }
        $hasScroll = $null -ne (Get-ScrollNode $ui)
        $scrollRequired = $scrollRequired -or $hasScroll
        if ($seen.Count -eq $targets.Count -and ($scrollVerified -or !$scrollRequired) -and $isGithub) { break }
        if (!$hasScroll) { break }
        $before = Get-ScrollAnchors $ui
        Invoke-PageScroll $ui
        $ui = Get-Ui
        $after = Get-ScrollAnchors $ui
        $moved = $false
        foreach ($key in $before.Keys) {
            if ($after.ContainsKey($key) -and $before[$key] - $after[$key] -gt 12) {
                if (!$scrollVerified) { Write-Output "Scroll evidence: '$key' y=$($before[$key]) -> $($after[$key])" }
                $scrollVerified = $true
                $moved = $true
            }
        }
        if ($scrollVerified -and !$moved -and (Get-SemanticText $ui) -ceq $text) { break }
    }
    foreach ($target in $targets) { Assert-Ui ($seen.ContainsKey($target)) "$Edition page text: $target" }
    Assert-Ui (!$scrollRequired -or $scrollVerified) 'last page supports its reported scrolling action'

    Invoke-Tap (Find-LastPageAction '重启系统界面')
    Start-Sleep -Milliseconds 350
    $confirmation = Get-Ui
    Assert-Ui ($null -ne (Get-SemanticNode $confirmation '授权并重启' -Actionable)) 'restart opens an explicit authorization confirmation'
    Invoke-Tap (Get-SemanticNode $confirmation '取消' -Actionable)
    Start-Sleep -Milliseconds 350
    $ui = Get-Ui
    Assert-Ui ($null -eq (Get-SemanticNode $ui '授权并重启' -Actionable)) 'cancelling closes the restart confirmation'

    $dialogActions = if ($isGithub) { @('导出说明', '使用须知') } else { @('使用须知') }
    foreach ($dialogAction in $dialogActions) {
        Invoke-Tap (Find-LastPageAction $dialogAction)
        Start-Sleep -Milliseconds 350
        $dialog = Get-Ui
        Assert-Ui ($null -ne (Get-SemanticNode $dialog '明白' -Actionable)) "$dialogAction opens a dismissible explanation"
        Invoke-Tap (Get-SemanticNode $dialog '明白' -Actionable)
        Start-Sleep -Milliseconds 350
        $ui = Get-Ui
        Assert-Ui ($null -eq (Get-SemanticNode $ui '明白' -Actionable)) "$dialogAction closes after acknowledgement"
    }
    Invoke-Tap (Find-LastPageAction '参考与开源说明')
    Start-Sleep -Milliseconds 350
    $credits = Get-Ui
    Assert-Ui ($null -ne (Get-SemanticNode $credits '项目沿革与致谢')) 'credits opens its own settings page'
    Invoke-Adb @('shell', 'input', 'keyevent', 'KEYCODE_BACK') | Out-Null
    Start-Sleep -Milliseconds 350
    $returnedUi = Get-Ui
    $returnedTab = Get-SemanticNode $returnedUi $lastTab -Actionable
    $returnedAction = Get-SemanticNode $returnedUi '参考与开源说明' -Actionable
    Assert-Ui ($null -ne $returnedAction -and $null -ne $returnedTab -and
        $returnedTab.GetAttribute('selected') -eq 'true') 'back returns from credits to the original tab'

    Write-Output "Settings UI verification passed for $Edition on $Serial."
    Write-Output "Verification transcript: $transcriptPath"
} finally {
    try {
        if ($deviceVerified -and $adb -and $Serial) {
            Invoke-Adb @('shell','rm','-f',$remote) | Out-Null
            Invoke-Adb @('shell','test','!','-e',$remote) | Out-Null
        }
    } finally {
        try {
            if ($transcriptStarted) {
                Stop-Transcript | Out-Null
            }
        } finally {
            try {
                foreach ($key in $environmentBefore.Keys) {
                    if ($null -eq $environmentBefore[$key]) {
                        Remove-Item -LiteralPath "Env:$key" -ErrorAction SilentlyContinue
                    } else {
                        [Environment]::SetEnvironmentVariable($key, $environmentBefore[$key], 'Process')
                    }
                }
            } finally {
                Remove-DeviceVerificationScratch `
                    -Root $root `
                    -ScratchDirectory $scratchDirectory `
                    -RunId $runId
                if (!$transcriptStarted -and $evidenceDirectoryCreated -and
                    (Test-Path -LiteralPath $runDirectory -PathType Container)) {
                    $entries = @(Get-ChildItem -LiteralPath $runDirectory -Force)
                    if ($entries.Count -eq 0) { Remove-Item -LiteralPath $runDirectory -Force }
                }
            }
        }
    }
}
