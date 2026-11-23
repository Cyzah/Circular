[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidateSet(
        'CheckSigningSecrets',
        'PrepareWorkspace',
        'ValidateVersion',
        'CheckReleaseHistory',
        'BuildSignedApk',
        'VerifyAndPackageApk',
        'GenerateChangelog',
        'PublishRelease',
        'RemoveSigningFile'
    )]
    [string]$Step
)

$ErrorActionPreference = 'Stop'

switch ($Step) {
    'CheckSigningSecrets' {
        $required = @{
            SIGNING_KEYSTORE_B64 = 'Circular_KEYSTORE_B64'
            CIRCULAR_KEYSTORE_PASSWORD = 'Circular_STORE_PASS'
            CIRCULAR_KEY_ALIAS = 'Circular_KEY_ALIAS'
            CIRCULAR_KEY_PASSWORD = 'Circular_KEY_PASS'
        }
        $missing = @($required.Keys | Where-Object {
            [string]::IsNullOrEmpty([Environment]::GetEnvironmentVariable($_))
        } | ForEach-Object { $required[$_] })
        if ($missing.Count -gt 0) { throw ('Required signing Secrets are unavailable: ' + ($missing -join ', ')) }
    }

    'PrepareWorkspace' {
        $root = (Get-Location).Path
        if ([IO.Path]::GetFileName($root) -ne 'Circular_Kotlin') { throw 'Unexpected project root.' }
        # Miuix theme classes require JDK 21; the app targets JVM 17.
        $jdk = $env:JAVA_HOME_21_X64
        $sdk = $env:ANDROID_HOME
        if (!$jdk -or !(Test-Path -LiteralPath (Join-Path $jdk 'bin/java.exe'))) {
            throw 'The Windows runner must provide JDK 21 for MIUIX unit tests.'
        }
        if (!$sdk) { throw 'The Windows runner must provide the Android SDK.' }
        $config = Get-Content -LiteralPath 'app/build.gradle.kts' -Raw
        $major = [regex]::Match($config, 'compileSdk\s*=\s*(\d+)').Groups[1].Value
        $minor = [regex]::Match($config, 'compileSdkMinor\s*=\s*(\d+)').Groups[1].Value
        if (!$major) { throw 'Cannot resolve compileSdk.' }
        $platform = if ($minor -and [int]$minor -gt 0) { "$major.$minor" } else { $major }
        foreach ($relative in @(
            "platforms/android-$platform/android.jar",
            'build-tools/36.0.0/aapt2.exe',
            'build-tools/36.0.0/apksigner.bat',
            'cmdline-tools/latest/bin/apkanalyzer.bat'
        )) {
            if (!(Test-Path -LiteralPath (Join-Path $sdk $relative))) {
                throw "Runner SDK component is unavailable: $relative. Update the runner image or provision it inside .local/android-sdk/."
            }
        }
        foreach ($relative in @(
            '.local/gradle-home', '.local/gradle-project', '.local/kotlin',
            '.local/android/avd', '.local/runtime-home', '.local/tmp'
        )) { [void][IO.Directory]::CreateDirectory((Join-Path $root $relative)) }
        $runtimeHome = (Join-Path $root '.local/runtime-home').Replace('\', '/')
        $temporary = (Join-Path $root '.local/tmp').Replace('\', '/')
        $settings = @{
            JAVA_HOME = $jdk
            ANDROID_HOME = $sdk
            ANDROID_SDK_ROOT = $sdk
            GRADLE_USER_HOME = Join-Path $root '.local/gradle-home'
            ANDROID_USER_HOME = Join-Path $root '.local/android'
            ANDROID_AVD_HOME = Join-Path $root '.local/android/avd'
            TEMP = $temporary
            TMP = $temporary
            JAVA_TOOL_OPTIONS = "-Duser.home=`"$runtimeHome`" -Djava.io.tmpdir=`"$temporary`" -XX:-UsePerfData"
        }
        foreach ($entry in $settings.GetEnumerator()) {
            "$($entry.Key)=$($entry.Value)" | Out-File -LiteralPath $env:GITHUB_ENV -Append -Encoding utf8
        }
        (Join-Path $jdk 'bin') | Out-File -LiteralPath $env:GITHUB_PATH -Append -Encoding utf8
        if (Test-Path -LiteralPath 'local.properties') { throw 'Refusing to overwrite local.properties.' }
        [IO.File]::WriteAllText((Join-Path $root 'local.properties'),
            ('sdk.dir=' + $sdk.Replace('\', '/').Replace(':', '\:') + "`n"), [Text.UTF8Encoding]::new($false))
    }

    'ValidateVersion' {
        $properties = ConvertFrom-StringData (Get-Content -LiteralPath 'version.properties' -Raw)
        foreach ($key in @('major', 'minor', 'patch', 'versionCode')) {
            $value = 0
            if ($properties[$key] -notmatch '^(0|[1-9][0-9]*)$' -or
                ![int]::TryParse($properties[$key], [ref]$value)) {
                throw "version.properties must define a canonical integer $key."
            }
        }
        if ([int]$properties.minor -ge 26 -or [int]$properties.patch -ge 26 -or
            [int]$properties.versionCode -le 0) { throw 'Invalid base-26 version or versionCode.' }
        $version = "$($properties.major).$($properties.minor).$($properties.patch)"
        $tag = "v$version"
        if ($env:GITHUB_REF_TYPE -eq 'tag' -and $env:GITHUB_REF_NAME -cne $tag) {
            throw "Tag must be $tag, matching version.properties."
        }
        if ($env:RELEASE_MODE -notin @('none', 'draft', 'prerelease', 'release')) {
            throw 'Invalid release mode.'
        }
        if ($env:RELEASE_EDITION -cnotin @('github', 'lspRepo')) { throw 'Invalid APK edition.' }
        if ($env:RELEASE_EDITION -eq 'lspRepo' -and $env:RELEASE_MODE -ne 'none') {
            throw 'Choose none to download the repository APK for upload to LSPosed.'
        }
        $commit = git rev-parse HEAD
        if ($LASTEXITCODE -ne 0 -or $commit -notmatch '^[0-9a-f]{40}$') { throw 'Cannot resolve source commit.' }
        . ./tools/ApkPaths.ps1
        $apk = Get-CircularApkPath -Root (Get-Location).Path -Edition $env:RELEASE_EDITION -Variant release
        $apkName = [IO.Path]::GetFileName($apk)
        $artifactBase = [IO.Path]::GetFileNameWithoutExtension($apkName)
        @(
            "version=$version", "version_code=$($properties.versionCode)",
            "tag=$tag", "commit=$commit", "apk_name=$apkName",
            "artifact_name=$artifactBase-$($env:GITHUB_RUN_ID)-$($env:GITHUB_RUN_ATTEMPT)"
        ) | Out-File -LiteralPath $env:GITHUB_OUTPUT -Append -Encoding utf8
    }

    'CheckReleaseHistory' {
        function Invoke-GitHubJson([string[]]$Arguments) {
            $result = & gh @Arguments
            if ($LASTEXITCODE -ne 0) { throw 'GitHub API request failed; no existing release will be replaced.' }
            return (($result -join "`n") | ConvertFrom-Json -Depth 50 -NoEnumerate)
        }
        [void][IO.Directory]::CreateDirectory($env:GH_CONFIG_DIR)
        $repo = $env:GITHUB_REPOSITORY
        $tag = $env:RELEASE_TAG
        $commit = $env:SOURCE_COMMIT
        # Existing drafts and published releases are both preserved. Never use --clobber.
        $pages = Invoke-GitHubJson @('api', '--paginate', '--slurp', "repos/$repo/releases?per_page=100")
        $baseCommit = ''
        $basePublishedAt = [DateTimeOffset]::MinValue
        foreach ($page in $pages) {
            foreach ($release in $page) {
                if ($release.tag_name -ceq $tag) {
                    throw "Release $tag already exists. Review it on GitHub; its assets will not be overwritten."
                }
                if ($release.draft) { continue }
                $previousTag = $release.tag_name
                $body = [string]$release.body
                $match = [regex]::Match($body, '(?m)^versionCode:\s*([0-9]+)\s*$')
                if ($match.Success) {
                    $previousCode = [int]$match.Groups[1].Value
                } else {
                    $encoded = [Uri]::EscapeDataString($previousTag)
                    $previous = Invoke-GitHubJson @('api', "repos/$repo/contents/version.properties?ref=$encoded")
                    if ($previous.encoding -ne 'base64') { throw "Cannot check versionCode for $previousTag." }
                    $text = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($previous.content))
                    $properties = ConvertFrom-StringData $text
                    $previousCode = 0
                    if (![int]::TryParse($properties.versionCode, [ref]$previousCode) -or $previousCode -le 0) {
                        throw "Cannot check versionCode for $previousTag."
                    }
                }
                if ([int]$env:VERSION_CODE -le $previousCode) {
                    throw "versionCode must be greater than the published version $previousTag. Bump locally before starting a new release."
                }
                if ($previousTag -match '^v([0-9]+\.[0-9]+\.[0-9]+)$' -and
                    [version]$env:VERSION -le [version]$Matches[1]) {
                    throw "Version must be newer than the published version $previousTag."
                }
                $publishedAt = [DateTimeOffset]$release.published_at
                if ($publishedAt -le $basePublishedAt) { continue }
                $candidate = & git rev-parse --verify "refs/tags/$previousTag^{commit}"
                if ($LASTEXITCODE -ne 0 -or $candidate -notmatch '^[0-9a-f]{40}$') {
                    throw "Cannot resolve the published tag $previousTag in the checkout."
                }
                & git merge-base --is-ancestor $candidate $commit
                if ($LASTEXITCODE -eq 1) { continue }
                if ($LASTEXITCODE -ne 0) { throw "Cannot compare $previousTag with the source commit." }
                $baseCommit = $candidate
                $basePublishedAt = $publishedAt
            }
        }

        # Resolve annotated tags too; a version tag may never point at different source.
        $refs = Invoke-GitHubJson @('api', "repos/$repo/git/matching-refs/tags/$tag")
        $matching = @($refs | Where-Object { $_.ref -ceq "refs/tags/$tag" })
        if ($matching.Count -gt 1) { throw 'Ambiguous version tag.' }
        if ($matching.Count -eq 1) {
            $object = $matching[0].object
            $depth = 0
            while ($object.type -eq 'tag') {
                if (++$depth -gt 10) { throw 'Too many nested annotated tags.' }
                $annotated = Invoke-GitHubJson @('api', "repos/$repo/git/tags/$($object.sha)")
                $object = $annotated.object
            }
            if ($object.type -ne 'commit' -or $object.sha -cne $commit) {
                throw "Tag $tag does not point to the commit that produced this APK."
            }
        }
        "base_commit=$baseCommit" | Out-File -LiteralPath $env:GITHUB_OUTPUT -Append -Encoding utf8
    }

    'BuildSignedApk' {
        foreach ($name in @('SIGNING_KEYSTORE_B64', 'CIRCULAR_KEYSTORE_PASSWORD',
            'CIRCULAR_KEY_ALIAS', 'CIRCULAR_KEY_PASSWORD')) {
            if ([string]::IsNullOrEmpty([Environment]::GetEnvironmentVariable($name))) {
                throw "Required signing Secret is unavailable: $name"
            }
        }
        $root = (Get-Location).Path
        $directory = Join-Path $root '.local/keystore'
        $keystore = Join-Path $directory "github-$($env:GITHUB_RUN_ID)-$($env:GITHUB_RUN_ATTEMPT).jks"
        if (Test-Path -LiteralPath $keystore) { throw 'Refusing to overwrite a signing file.' }
        $bytes = $null
        try {
            [void][IO.Directory]::CreateDirectory($directory)
            try { $bytes = [Convert]::FromBase64String($env:SIGNING_KEYSTORE_B64) }
            catch { throw 'Circular_KEYSTORE_B64 must contain a valid Base64 keystore.' }
            if ($bytes.Length -eq 0) { throw 'The decoded signing file is empty.' }
            [IO.File]::WriteAllBytes($keystore, $bytes)
            $env:CIRCULAR_KEYSTORE = $keystore
            $task = if ($env:RELEASE_EDITION -eq 'github') { ':app:assembleGithubRelease' }
                else { ':app:assembleLspRepoRelease' }
            $arguments = @(
                '--project-cache-dir', (Join-Path $root '.local/gradle-project'),
                ('-Pkotlin.project.persistent.dir=' + (Join-Path $root '.local/kotlin')),
                '-Pkotlin.project.persistent.dir.gradle.disableWrite=true',
                '-PuseReleaseSigningForDebug=false', $task
            )
            & ./tools/build.ps1 -Tasks $arguments
            if ($LASTEXITCODE -ne 0) { throw 'Signed Release build failed.' }
        } finally {
            if ($null -ne $bytes) { [Array]::Clear($bytes, 0, $bytes.Length) }
            Remove-Item -LiteralPath Env:CIRCULAR_KEYSTORE -ErrorAction SilentlyContinue
            if (Test-Path -LiteralPath $keystore) { Remove-Item -LiteralPath $keystore -Force }
            if (Test-Path -LiteralPath $keystore) { throw 'Signing file cleanup failed.' }
        }
    }

    'VerifyAndPackageApk' {
        . ./tools/ApkPaths.ps1
        $root = (Get-Location).Path
        $original = Get-CircularApkPath -Root $root -Edition $env:RELEASE_EDITION -Variant release
        & ./tools/verify-apk.ps1 -Edition $env:RELEASE_EDITION -ApkPath $original
        if ($LASTEXITCODE -ne 0) { throw 'Original Release APK verification failed.' }
        $directory = Join-Path $root "delivery/$($env:VERSION)"
        if (Test-Path -LiteralPath $directory) { throw 'Refusing to overwrite a delivery directory.' }
        [void][IO.Directory]::CreateDirectory($directory)
        $fileName = [IO.Path]::GetFileName($original)
        $copy = Join-Path $directory $fileName
        Copy-Item -LiteralPath $original -Destination $copy
        & ./tools/verify-apk.ps1 -Edition $env:RELEASE_EDITION -ApkPath $copy
        if ($LASTEXITCODE -ne 0) { throw 'Download APK verification failed.' }
        $digest = (Get-FileHash -LiteralPath $original -Algorithm SHA256).Hash.ToLowerInvariant()
        if ((Get-FileHash -LiteralPath $copy -Algorithm SHA256).Hash.ToLowerInvariant() -ne $digest) {
            throw 'Original APK and download copy differ.'
        }
        [IO.File]::WriteAllText((Join-Path $directory 'SHA256SUMS.txt'),
            "$digest  $fileName`n", [Text.UTF8Encoding]::new($false))
        $notes = @(
            "# Circular $($env:VERSION)", '', "versionCode: $($env:VERSION_CODE)",
            "Commit: $($env:SOURCE_COMMIT)",
            "Build: $($env:GITHUB_SERVER_URL)/$($env:GITHUB_REPOSITORY)/actions/runs/$($env:GITHUB_RUN_ID)", '',
            'Checks passed: Release APK identity/version, Xposed entry and metadata,',
            'nine fonts, V3 signature,',
            'and SHA-256 equality of the original APK and download copy.', '',
            'Unit tests, Debug Lint, Debug build and maintainability run independently in the Tests workflow.',
            'Their status is not checked by this Release run. Device/SystemUI Hook validation was not run.'
        ) -join "`n"
        [IO.File]::WriteAllText((Join-Path $directory 'RELEASE.md'), $notes + "`n", [Text.UTF8Encoding]::new($false))
    }

    'GenerateChangelog' {
        $utf8 = [Text.UTF8Encoding]::new($false)
        [Console]::OutputEncoding = $utf8
        $range = if ($env:BASE_COMMIT) { "$($env:BASE_COMMIT)..$($env:SOURCE_COMMIT)" } else { $env:SOURCE_COMMIT }
        $commits = @(& git -c i18n.logOutputEncoding=UTF-8 --no-pager log --no-color --no-show-signature --format='%H%x09%s' $range --)
        if ($LASTEXITCODE -ne 0) { throw 'Cannot read commits for the release description.' }
        $repositoryUrl = "$($env:GITHUB_SERVER_URL)/$($env:GITHUB_REPOSITORY)"
        $notes = foreach ($line in $commits) {
            $sha, $subject = $line -split "`t", 2
            $subject = $subject -replace '([\\`*_{}\[\]()<>#+.!|@~&])', '\$1'
            "- [$($sha.Substring(0, 7))]($repositoryUrl/commit/$sha) $subject"
        }
        $body = if ($commits.Count -gt 0) { $notes -join "`n" } else { '本次发布没有新增提交。' }
        [IO.File]::WriteAllText((Join-Path (Get-Location).Path "delivery/$($env:VERSION)/CHANGELOG.md"), $body + "`n", $utf8)
    }

    'PublishRelease' {
        $prerelease = if ($env:RELEASE_MODE -eq 'prerelease') { '--prerelease=true' } else { '--prerelease=false' }
        $latest = if ($env:RELEASE_MODE -eq 'release') { '--latest=true' } else { '--latest=false' }
        & gh release edit $env:RELEASE_TAG --repo $env:GITHUB_REPOSITORY --draft=false $prerelease $latest
        if ($LASTEXITCODE -ne 0) { throw 'The complete draft was retained, but publishing failed.' }
    }

    'RemoveSigningFile' {
        $root = (Get-Location).Path
        $directory = [IO.Path]::GetFullPath((Join-Path $root '.local/keystore'))
        $keystore = [IO.Path]::GetFullPath((Join-Path $directory "github-$($env:GITHUB_RUN_ID)-$($env:GITHUB_RUN_ATTEMPT).jks"))
        if ([IO.Path]::GetDirectoryName($keystore) -ne $directory) { throw 'Unsafe signing cleanup path.' }
        if (Test-Path -LiteralPath $keystore) { Remove-Item -LiteralPath $keystore -Force }
        if (Test-Path -LiteralPath $keystore) { throw 'Signing file still exists.' }
    }
}
