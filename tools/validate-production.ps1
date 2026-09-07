[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$DeviceSerial,
    [string]$AdbPath = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe",
    [string]$ArtifactDirectory = "build/production-validation"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$previousSerial = $env:ANDROID_SERIAL
$quotaClass = 'com.streamvault.app.service.DownloadForegroundServiceQuotaInstrumentationTest'
$summaries = [System.Collections.Generic.List[object]]::new()

function Invoke-AdbCheck {
    param([string[]]$Arguments)
    $output = & $AdbPath -s $DeviceSerial @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "adb failed: $($Arguments -join ' ')"
    }
    return $output
}

function Invoke-GradleCheck {
    param([string]$Name, [string[]]$Arguments)
    Write-Host "Running $Name"
    & .\gradlew.bat @Arguments --console=plain 2>&1 |
        Tee-Object -FilePath (Join-Path $outputDirectory "$Name.log") |
        Out-Host
    if ($LASTEXITCODE -ne 0) {
        throw "$Name failed; see $outputDirectory/$Name.log"
    }
}

function Save-DeviceResults {
    param([string]$Module, [string]$Name)
    $source = Join-Path $repositoryRoot "$Module/build/outputs/androidTest-results/connected/debug"
    Copy-Item -LiteralPath $source -Destination (Join-Path $outputDirectory $Name) -Recurse
    $suites = @(Get-ChildItem -LiteralPath $source -Filter 'TEST-*.xml' | ForEach-Object {
        ([xml](Get-Content -LiteralPath $_.FullName -Raw)).testsuite
    })
    if ($suites.Count -eq 0) {
        throw "No device results were produced for $Name"
    }
    $summaries.Add([pscustomobject]@{
        Check = $Name
        Tests = ($suites | ForEach-Object { [int]$_.tests } | Measure-Object -Sum).Sum
        Failures = ($suites | ForEach-Object { [int]$_.failures } | Measure-Object -Sum).Sum
        Errors = ($suites | ForEach-Object { [int]$_.errors } | Measure-Object -Sum).Sum
        Skipped = ($suites | ForEach-Object { [int]$_.skipped } | Measure-Object -Sum).Sum
    })
    $summaries | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $outputDirectory 'device-results.json')
}

Push-Location $repositoryRoot
try {
    if (!(Test-Path -LiteralPath $AdbPath)) {
        throw "adb not found: $AdbPath"
    }
    $env:ANDROID_SERIAL = $DeviceSerial
    $outputDirectory = Join-Path (Join-Path $repositoryRoot $ArtifactDirectory) (Get-Date -Format 'yyyyMMdd-HHmmss')
    New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
    if ((Invoke-AdbCheck -Arguments @('get-state')) -ne 'device') {
        throw 'The selected Android device is not ready'
    }
    $apiLevel = [int](Invoke-AdbCheck -Arguments @('shell', 'getprop', 'ro.build.version.sdk'))
    $originalTimeout = (Invoke-AdbCheck -Arguments @(
        'shell', 'device_config', 'get', 'activity_manager', 'data_sync_fgs_timeout_duration'
    )) -join ''
    if ($apiLevel -ge 35 -and $originalTimeout.Trim() -ne 'null') {
        throw 'Use a dedicated test emulator with the default dataSync timeout before running this validation'
    }

    Invoke-GradleCheck -Name 'baseline' -Arguments @(
        'testDebugUnitTest', 'verifyLintBaseline', 'koverXmlReportCi', 'koverHtmlReportCi', ':app:assembleRelease'
    )
    Invoke-GradleCheck -Name 'lint' -Arguments @(
        ':app:lintDebug', ':data:lintDebug', ':player:lintDebug', '--no-daemon', '--max-workers=1'
    )
    Invoke-GradleCheck -Name 'data-device' -Arguments @(':data:connectedDebugAndroidTest')
    Save-DeviceResults -Module 'data' -Name 'data-device'
    Invoke-GradleCheck -Name 'player-device' -Arguments @(':player:connectedDebugAndroidTest')
    Save-DeviceResults -Module 'player' -Name 'player-device'
    Invoke-GradleCheck -Name 'app-device' -Arguments @(
        ':app:connectedDebugAndroidTest', '-PinstrumentationTimeoutMs=180000',
        "-PcompatApi=$apiLevel",
        "-PinstrumentationExcludedClasses=$quotaClass,com.streamvault.app.player.LivePlaybackValidationTest"
    )
    Save-DeviceResults -Module 'app' -Name 'app-device'

    if ($apiLevel -ge 35) {
        try {
            Invoke-AdbCheck -Arguments @(
                'shell', 'am', 'compat', 'enable', 'FGS_INTRODUCE_TIME_LIMITS', 'com.streamvault.app.debug'
            ) | Out-Host
            Invoke-AdbCheck -Arguments @(
                'shell', 'device_config', 'put', 'activity_manager', 'data_sync_fgs_timeout_duration', '5000'
            ) | Out-Host
            Invoke-GradleCheck -Name 'app-quota-device' -Arguments @(
                ':app:connectedDebugAndroidTest', '-PinstrumentationTimeoutMs=45000',
                "-Pandroid.testInstrumentationRunnerArguments.class=$quotaClass"
            )
            Save-DeviceResults -Module 'app' -Name 'app-quota-device'
        } finally {
            Invoke-AdbCheck -Arguments @(
                'shell', 'device_config', 'delete', 'activity_manager', 'data_sync_fgs_timeout_duration'
            ) | Out-Host
            Invoke-AdbCheck -Arguments @(
                'shell', 'am', 'compat', 'reset', 'FGS_INTRODUCE_TIME_LIMITS', 'com.streamvault.app.debug'
            ) | Out-Host
        }
    } else {
        Write-Warning 'Android dataSync timeout callbacks require API 35 or newer; validate that gate on a supported device.'
    }

    $summaries | Format-Table -AutoSize
    Write-Host "Validation reports: $outputDirectory"
    Write-Host 'Device skips, official signing, and sustained live playback must still be reviewed before publication.'
} finally {
    $env:ANDROID_SERIAL = $previousSerial
    Pop-Location
}