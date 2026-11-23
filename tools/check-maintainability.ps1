param(
    [string]$Root = ""
)

$ErrorActionPreference = "Stop"
if (-not $Root) {
    $Root = Split-Path -Parent $PSScriptRoot
}
$Root = [IO.Path]::GetFullPath($Root)
$productionRoots = @('main', 'github', 'lspRepo') | ForEach-Object { Join-Path $Root "app/src/$_" }
$readme = Join-Path $Root "README.md"
$syntheticPattern = '\bm[0-9]+lambda\$[0-9]+'
$lambdaPattern = '->'
$violations = [Collections.Generic.List[string]]::new()
$maintainedFiles = @()
foreach ($relativeRoot in @("app/src", "tools", "gradle")) {
    $directory = Join-Path $Root $relativeRoot
    if (Test-Path -LiteralPath $directory) {
        $maintainedFiles += Get-ChildItem -LiteralPath $directory -Recurse -File |
            Where-Object { $_.Extension -in @('.java', '.kt', '.kts', '.ps1', '.gradle') }
    }
}
$maintainedFiles += Get-ChildItem -LiteralPath $Root -File |
    Where-Object { $_.Extension -in @('.java', '.kt', '.kts', '.ps1', '.gradle') }
foreach ($file in $maintainedFiles) {
    $count = [IO.File]::ReadAllLines($file.FullName).Length
    if ($count -gt 1500) {
        $relative = $file.FullName.Substring($Root.TrimEnd('\').Length + 1).Replace('\', '/')
        $violations.Add("$relative has $count lines; limit is 1500")
    }
}

function Get-RelativePath {
    param(
        [string]$Base,
        [string]$Path
    )

    $baseUri = [Uri]::new(($Base.TrimEnd('\') + '\'))
    $pathUri = [Uri]::new($Path)
    return [Uri]::UnescapeDataString(
        $baseUri.MakeRelativeUri($pathUri).ToString())
}

function Test-MojibakeLine {
    param([string]$Line)

    if ($Line -notmatch '[\u3400-\u9FFF]') {
        return $false
    }
    try {
        $gbk = [Text.Encoding]::GetEncoding(936)
        $decoded = [Text.Encoding]::UTF8.GetString($gbk.GetBytes($Line))
        return $decoded -notmatch '[\uFFFD]' -and
            $decoded -match '[\u3400-\u9FFF]'
    } catch {
        return $false
    }
}

$productionFiles = @(foreach ($sourceRoot in $productionRoots) {
    if (Test-Path -LiteralPath $sourceRoot -PathType Container) {
        Get-ChildItem -LiteralPath $sourceRoot -Recurse -File |
            Where-Object { $_.Extension -in @('.java', '.kt') } | Sort-Object FullName
    }
})
foreach ($file in $productionFiles) {
    $relative = Get-RelativePath $Root $file.FullName
    $lines = @(Get-Content -LiteralPath $file.FullName -Encoding UTF8)
    for ($index = 0; $index -lt $lines.Count; $index++) {
        if ($lines[$index] -match $syntheticPattern) {
            $violations.Add("$relative`:$($index + 1) contains a synthetic callback name")
        }
        if ($file.Extension -eq '.java' -and $lines[$index].Contains($lambdaPattern)) {
            $violations.Add("$relative`:$($index + 1) contains a production Java lambda")
        }
    }
}

$textFiles = @()
if (Test-Path -LiteralPath $readme) {
    $textFiles += Get-Item -LiteralPath $readme
}
$textFiles += $productionFiles
foreach ($file in $textFiles) {
    $relative = Get-RelativePath $Root $file.FullName
    $lines = @(Get-Content -LiteralPath $file.FullName -Encoding UTF8)
    for ($index = 0; $index -lt $lines.Count; $index++) {
        if (Test-MojibakeLine $lines[$index]) {
            $violations.Add(
                "$relative`:$($index + 1) contains mojibake text")
        }
    }
}

if ($violations.Count -gt 0) {
    Write-Output "Maintainability check failed:"
    foreach ($violation in $violations) {
        Write-Output " - $violation"
    }
    exit 1
}

Write-Output "Maintainability check passed"
