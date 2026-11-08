param([switch]$DeviceTests)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    & "$PSScriptRoot/gradlew.bat" --no-daemon testDebugUnitTest lintDebug assembleDebug
    if ($LASTEXITCODE -ne 0) { throw 'Android build or verification failed' }
    if ($DeviceTests) {
        & "$PSScriptRoot/gradlew.bat" --no-daemon connectedDebugAndroidTest
        if ($LASTEXITCODE -ne 0) { throw 'Device tests failed' }
    }
    New-Item -ItemType Directory -Force 'dist' | Out-Null
    Copy-Item -LiteralPath 'app/build/outputs/apk/debug/app-debug.apk' -Destination 'dist/CostAccounting-Android.apk'
    Write-Host 'Ready: dist/CostAccounting-Android.apk'
} finally { Pop-Location }
