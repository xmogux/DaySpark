param(
    [ValidateSet('Full', 'Demo')]
    [string]$Mode = 'Full'
)

$ErrorActionPreference = 'Stop'
$ProjectDir = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $ProjectDir

function Find-Chrome {
    $knownPaths = @()
    if ($env:ProgramFiles) {
        $knownPaths += (Join-Path $env:ProgramFiles 'Google\Chrome\Application\chrome.exe')
    }
    if (${env:ProgramFiles(x86)}) {
        $knownPaths += (Join-Path ${env:ProgramFiles(x86)} 'Google\Chrome\Application\chrome.exe')
    }
    if ($env:LOCALAPPDATA) {
        $knownPaths += (Join-Path $env:LOCALAPPDATA 'Google\Chrome\Application\chrome.exe')
    }
    foreach ($path in $knownPaths) {
        if (Test-Path -LiteralPath $path -PathType Leaf) { return $path }
    }
    foreach ($regPath in @(
        'HKCU:\Software\Microsoft\Windows\CurrentVersion\App Paths\chrome.exe',
        'HKLM:\Software\Microsoft\Windows\CurrentVersion\App Paths\chrome.exe',
        'HKLM:\Software\WOW6432Node\Microsoft\Windows\CurrentVersion\App Paths\chrome.exe'
    )) {
        try {
            $exe = (Get-Item -LiteralPath $regPath -ErrorAction Stop).GetValue('')
            if ($exe -and (Test-Path -LiteralPath $exe -PathType Leaf)) { return $exe }
        } catch { }
    }
    $command = Get-Command 'chrome.exe' -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    return $null
}

function Open-Browser([string]$Target) {
    $chrome = Find-Chrome
    if ($chrome) {
        Write-Host '[DaySpark] Opening in Google Chrome...' -ForegroundColor Green
        Start-Process -FilePath $chrome -ArgumentList ('--new-window "{0}"' -f $Target)
    } else {
        Write-Host '[DaySpark] Chrome was not found. Opening default browser.' -ForegroundColor Yellow
        Start-Process -FilePath $Target
    }
}

if ($Mode -eq 'Demo') {
    $demo = Join-Path $ProjectDir 'demo.html'
    if (-not (Test-Path -LiteralPath $demo)) {
        Write-Host 'demo.html is missing. Extract the COMPLETE ZIP first.' -ForegroundColor Red
        exit 1
    }
    $demoUrl = [System.Uri]::new((Resolve-Path -LiteralPath $demo).Path).AbsoluteUri
    Write-Host '[DaySpark] Standalone demo: no server, no Java, browser-only local storage.' -ForegroundColor Cyan
    Open-Browser $demoUrl
    exit 0
}

$SiteUrl = 'http://127.0.0.1:8080/'
$BuildMarker = 'dayspark-ui-5.4-calendar-redesign'
function Get-DaySparkResponse {
    try { return Invoke-WebRequest -Uri ($SiteUrl + '?_layout_ver=54') -UseBasicParsing -TimeoutSec 3 }
    catch { return $null }
}

function Test-DaySparkPage {
    try {
        $response = Invoke-WebRequest -Uri $SiteUrl -UseBasicParsing -TimeoutSec 2
        return (($response.StatusCode -eq 200) -and $response.Content.Contains($BuildMarker))
    } catch {
        return $false
    }
}

function Test-PortBusy {
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $ar = $client.BeginConnect('127.0.0.1', 8080, $null, $null)
        if (-not $ar.AsyncWaitHandle.WaitOne(600, $false)) { return $false }
        $client.EndConnect($ar)
        return $true
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

if (Test-DaySparkPage) {
    Write-Host '[DaySpark] v5.4 is already running. Opening verified session.' -ForegroundColor Green
    Open-Browser ($SiteUrl + '?ui=54')
    exit 0
}

if (Test-PortBusy) {
    $existing = Get-DaySparkResponse
    if ($existing -and $existing.Content -match 'DaySpark') {
        Write-Host '[DaySpark] OLD DaySpark is still running on port 8080.' -ForegroundColor Yellow
        Write-Host '[DaySpark] Attempting to stop only this project saved DaySpark server...' -ForegroundColor Yellow
        $stopScript = Join-Path $PSScriptRoot 'stop.ps1'
        $knownPid = Join-Path $ProjectDir '.tools\server_pid.txt'
        if (Test-Path -LiteralPath $knownPid) {
            & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $stopScript
            Start-Sleep -Seconds 3
        }
    }
    if (Test-PortBusy) {
        Write-Host '[DaySpark] Port 8080 is still occupied. REFUSING to open a stale version.' -ForegroundColor Red
        Write-Host '[DaySpark] Close all older DaySpark server processes or run their 03_STOP_DAYSPARK.bat.' -ForegroundColor Yellow
        Write-Host '[DaySpark] Your source files were repaired; no data was removed.' -ForegroundColor Yellow
        exit 2
    }
}

$ToolDir = Join-Path $ProjectDir '.tools'
New-Item -ItemType Directory -Path $ToolDir -Force | Out-Null
$FailedFlag = Join-Path $ToolDir 'one_click_server_failed.flag'
if (Test-Path -LiteralPath $FailedFlag) { Remove-Item -LiteralPath $FailedFlag -Force }
$ServerRunner = Join-Path $PSScriptRoot 'server_console.bat'
if (-not (Test-Path -LiteralPath $ServerRunner)) {
    Write-Host 'scripts/server_console.bat is missing.' -ForegroundColor Red
    exit 1
}

Write-Host '[DaySpark] Starting the local web server...' -ForegroundColor Cyan
Write-Host '[DaySpark] Running quietly; startup errors will appear here.' -ForegroundColor DarkGray
try {
    # Keep the server running without an unnecessary visible download/console window.
    # Failure details remain in .tools/server_startup.log and are printed by this launcher.
    $serverArgs = '/c call "{0}" --hidden' -f $ServerRunner
    $serverProcess = Start-Process -FilePath $env:ComSpec -ArgumentList $serverArgs -WorkingDirectory $ProjectDir -WindowStyle Hidden -PassThru
    [System.IO.File]::WriteAllText((Join-Path $ToolDir 'server_pid.txt'), [string]$serverProcess.Id)
} catch {
    Write-Host ('[DaySpark] Failed to launch server: ' + $_.Exception.Message) -ForegroundColor Red
    exit 1
}

# Only open the browser after a LIVE Spring Boot page is responding.
for ($attempt = 0; $attempt -lt 450; $attempt++) {
    if (Test-DaySparkPage) {
        Write-Host '[DaySpark] The server is ready.' -ForegroundColor Green
        Open-Browser ($SiteUrl + '?ui=54')
        exit 0
    }
    if (Test-Path -LiteralPath $FailedFlag) {
        Write-Host '[DaySpark] Server startup failed. The REAL ERROR is below:' -ForegroundColor Red
        $logFile = Join-Path $ToolDir 'server_startup.log'
        if (Test-Path -LiteralPath $logFile) {
            Get-Content -LiteralPath $logFile -Tail 45 | ForEach-Object { Write-Host $_ }
            Write-Host ('[DaySpark] Full error log: ' + $logFile) -ForegroundColor Yellow
        } else {
            Write-Host '[DaySpark] No log yet. Inspect the DaySpark Server console.' -ForegroundColor Yellow
        }
        exit 1
    }
    if (($attempt % 10) -eq 0) {
        Write-Host '[DaySpark] Checking whether the new v5.4 screen is actually served...' -ForegroundColor DarkGray
    }
    Start-Sleep -Seconds 2
}
Write-Host '[DaySpark] Could not confirm that the server started.' -ForegroundColor Red
Write-Host 'Inspect .tools/server_startup.log or use 02_OPEN_ERROR_LOG.bat.' -ForegroundColor Yellow
exit 1
