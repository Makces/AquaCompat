param(
    [string]$Workspace = "C:\Users\user\IdeaProjects\Accept My Seed",
    [switch]$Resume
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
$PSNativeCommandUseErrorActionPreference = $false

$workspaceRoot = (Resolve-Path $Workspace).Path
$tmpRoot = Join-Path $workspaceRoot "tmp\compat-matrix"
$cacheRoot = Join-Path $tmpRoot "cache"
$reportRoot = Join-Path $tmpRoot "reports"
$runRoot = Join-Path $tmpRoot "runs"

New-Item -ItemType Directory -Force -Path $cacheRoot, $reportRoot, $runRoot | Out-Null

function Invoke-JsonRequest {
    param([string]$Uri)
    return Invoke-RestMethod -Uri $Uri
}

function Get-AquacultureVersions {
    $versions = Invoke-JsonRequest "https://api.modrinth.com/v2/project/aquaculture/version?loaders=%5B%22neoforge%22%5D&game_versions=%5B%221.21.1%22%5D"
    return $versions |
        Sort-Object {[datetime]("$($_.date_published)") } |
        ForEach-Object { $_.version_number }
}

function Get-FarmersDelightVersions {
    $versions = Invoke-JsonRequest "https://api.modrinth.com/v2/project/farmers-delight/version?loaders=%5B%22neoforge%22%5D&game_versions=%5B%221.21.1%22%5D"
    return $versions |
        Sort-Object {[datetime]("$($_.date_published)") } |
        ForEach-Object {
            [pscustomobject]@{
                version = $_.version_number
                url = ($_.files | Where-Object primary | Select-Object -First 1).url
                filename = ($_.files | Where-Object primary | Select-Object -First 1).filename
            }
        }
}

function Get-JeiVersionsToTest {
    [xml]$metadata = Invoke-WebRequest "https://maven.blamejared.com/mezz/jei/jei-1.21.1-common-api/maven-metadata.xml"
    $allVersions = @($metadata.metadata.versioning.versions.version)

    $distinctPatches = [System.Collections.Generic.List[string]]::new()
    $seenPatches = [System.Collections.Generic.HashSet[string]]::new()
    foreach ($version in ($allVersions | Sort-Object {[version](($_ -split '\.')[0..2] -join '.')} -Descending)) {
        $patch = (($version -split '\.')[0..2] -join '.')
        if ($seenPatches.Add($patch)) {
            $distinctPatches.Add($version)
        }
        if ($distinctPatches.Count -ge 10) {
            break
        }
    }

    $distinctMinors = [System.Collections.Generic.List[string]]::new()
    $seenMinors = [System.Collections.Generic.HashSet[string]]::new()
    foreach ($version in ($allVersions | Sort-Object {[version](($_ -split '\.')[0..1] -join '.')} -Descending)) {
        $minor = (($version -split '\.')[0..1] -join '.')
        if ($seenMinors.Add($minor)) {
            $distinctMinors.Add($version)
        }
        if ($distinctMinors.Count -ge 5) {
            break
        }
    }

    return @($distinctPatches + $distinctMinors | Select-Object -Unique)
}

function Get-CachedFile {
    param(
        [string]$Uri,
        [string]$FileName
    )

    $path = Join-Path $cacheRoot $FileName
    if (-not (Test-Path $path)) {
        Invoke-WebRequest -Uri $Uri -OutFile $path
    }
    return $path
}

function Reset-RunDirectory {
    param([string]$Path)

    $resolved = [IO.Path]::GetFullPath($Path)
    $allowedRoot = [IO.Path]::GetFullPath($runRoot)
    if (-not $resolved.StartsWith($allowedRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove path outside run root: $resolved"
    }

    if (Test-Path $resolved) {
        Remove-Item -LiteralPath $resolved -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path (Join-Path $resolved "mods") | Out-Null
}

function Invoke-GradleCase {
    param(
        [string]$Label,
        [string[]]$GradleArgs,
        [string]$RunDir,
        [string]$ExtraModJar
    )

    $logPath = Join-Path $reportRoot "$Label.log"
    $resultPath = Join-Path $reportRoot "$Label.json"

    if ($Resume -and (Test-Path $resultPath)) {
        return Get-Content $resultPath | ConvertFrom-Json
    }

    Reset-RunDirectory -Path $RunDir
    if ($ExtraModJar) {
        Copy-Item -LiteralPath $ExtraModJar -Destination (Join-Path $RunDir "mods")
    }

    $relativeRunDir = $RunDir
    if ($relativeRunDir.StartsWith($workspaceRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        $relativeRunDir = $relativeRunDir.Substring($workspaceRoot.Length).TrimStart('\', '/')
    }
    $normalizedArgs = $GradleArgs | ForEach-Object {
        if ($_ -like "-Pmatrix_run_dir=*") {
            "-Pmatrix_run_dir=$relativeRunDir"
        } else {
            $_
        }
    }
    $fullArgs = @("--console=plain", "--no-configuration-cache") + $normalizedArgs
    $stdoutPath = Join-Path $reportRoot "$Label.stdout.log"
    $stderrPath = Join-Path $reportRoot "$Label.stderr.log"
    $argumentString = ($fullArgs | ForEach-Object {
        if ($_ -match '\s') {
            '"' + ($_ -replace '"', '\"') + '"'
        } else {
            $_
        }
    }) -join ' '

    $start = Get-Date
    $process = Start-Process -FilePath ".\gradlew.bat" -ArgumentList $argumentString -WorkingDirectory $workspaceRoot -NoNewWindow -Wait -PassThru -RedirectStandardOutput $stdoutPath -RedirectStandardError $stderrPath
    $exitCode = $process.ExitCode
    $end = Get-Date

    $combinedLog = @()
    if (Test-Path $stdoutPath) {
        $combinedLog += Get-Content $stdoutPath
    }
    if (Test-Path $stderrPath) {
        $combinedLog += Get-Content $stderrPath
    }
    $combinedLog | Set-Content -Path $logPath

    $result = [pscustomobject]@{
        label = $Label
        exitCode = $exitCode
        success = ($exitCode -eq 0)
        startedAt = $start.ToString("o")
        endedAt = $end.ToString("o")
        durationSeconds = [math]::Round(($end - $start).TotalSeconds, 2)
        args = $fullArgs
        log = $logPath
    }

    $result | ConvertTo-Json -Depth 4 | Set-Content -Path $resultPath
    return $result
}

Set-Location $workspaceRoot

$aquacultureVersions = Get-AquacultureVersions
$farmersDelightVersions = Get-FarmersDelightVersions
$jeiVersions = Get-JeiVersionsToTest

$latestAquaculture = $aquacultureVersions[-1]
$oldestAquaculture = $aquacultureVersions[0]
$latestFd = $farmersDelightVersions[-1]
$oldestFd = $farmersDelightVersions[0]
$latestJei = ($jeiVersions | Sort-Object {[version]$_})[-1]

$results = [System.Collections.Generic.List[object]]::new()

$latestFdJar = Get-CachedFile -Uri $latestFd.url -FileName $latestFd.filename
foreach ($version in $aquacultureVersions) {
    $label = "aquaculture-$version"
    $runDir = Join-Path $runRoot $label
    $results.Add((Invoke-GradleCase -Label $label -RunDir $runDir -ExtraModJar $latestFdJar -GradleArgs @(
        "runGameTestServer",
        "-Paquaculture_version=1.21.1-$version",
        "-Pjei_version=$latestJei",
        "-Pmatrix_run_dir=$runDir"
    )))
}

foreach ($fd in $farmersDelightVersions) {
    $label = "farmersdelight-$($fd.version)"
    $runDir = Join-Path $runRoot $label
    $fdJar = Get-CachedFile -Uri $fd.url -FileName $fd.filename
    $results.Add((Invoke-GradleCase -Label $label -RunDir $runDir -ExtraModJar $fdJar -GradleArgs @(
        "runGameTestServer",
        "-Paquaculture_version=1.21.1-$latestAquaculture",
        "-Pjei_version=$latestJei",
        "-Pmatrix_run_dir=$runDir"
    )))
}

$oldestFdJar = Get-CachedFile -Uri $oldestFd.url -FileName $oldestFd.filename
$comboLabel = "combo-floor-floor"
$comboRunDir = Join-Path $runRoot $comboLabel
$results.Add((Invoke-GradleCase -Label $comboLabel -RunDir $comboRunDir -ExtraModJar $oldestFdJar -GradleArgs @(
    "runGameTestServer",
    "-Paquaculture_version=1.21.1-$oldestAquaculture",
    "-Pjei_version=$latestJei",
    "-Pmatrix_run_dir=$comboRunDir"
)))

foreach ($version in $jeiVersions) {
    $label = "jei-$version"
    $runDir = Join-Path $runRoot $label
    $results.Add((Invoke-GradleCase -Label $label -RunDir $runDir -GradleArgs @(
        "compileJava",
        "-Paquaculture_version=1.21.1-$latestAquaculture",
        "-Pjei_version=$version",
        "-Pmatrix_run_dir=$runDir"
    )))
}

$summary = [pscustomobject]@{
    aquacultureVersions = $aquacultureVersions
    farmersDelightVersions = $farmersDelightVersions.version
    jeiVersions = $jeiVersions
    total = $results.Count
    passed = @($results | Where-Object success).Count
    failed = @($results | Where-Object { -not $_.success }).Count
    results = $results
}

$summaryPath = Join-Path $reportRoot "summary.json"
$summary | ConvertTo-Json -Depth 6 | Set-Content -Path $summaryPath

$markdown = @()
$markdown += "# Compatibility Matrix"
$markdown += ""
$markdown += "Generated: $(Get-Date -Format s)"
$markdown += ""
$markdown += "| Case | Result | Seconds |"
$markdown += "| --- | --- | ---: |"
foreach ($result in $results) {
    $status = if ($result.success) { "PASS" } else { "FAIL" }
    $markdown += "| $($result.label) | $status | $($result.durationSeconds) |"
}
$markdown += ""
$markdown += "Summary: $($summary.passed) passed / $($summary.total) total"
$markdownPath = Join-Path $reportRoot "summary.md"
$markdown | Set-Content -Path $markdownPath

Write-Host "Summary written to $summaryPath"
Write-Host "Markdown written to $markdownPath"
if ($summary.failed -gt 0) {
    exit 1
}
