$ErrorActionPreference = 'Stop'
$ProjectDir = Split-Path -Parent $PSScriptRoot
$PidFile = Join-Path $ProjectDir '.tools\server_pid.txt'
if (-not (Test-Path -LiteralPath $PidFile)) {
    Write-Host '[DaySpark] No background server PID file found.' -ForegroundColor Yellow
    exit 0
}
$serverPid = 0
if (-not [int]::TryParse((Get-Content -LiteralPath $PidFile -Raw).Trim(), [ref]$serverPid)) {
    Write-Host '[DaySpark] Invalid server PID file.' -ForegroundColor Red
    exit 1
}
$proc = Get-CimInstance Win32_Process -Filter "ProcessId=$serverPid" -ErrorAction SilentlyContinue
if ($null -eq $proc) {
    Remove-Item -LiteralPath $PidFile -Force
    Write-Host '[DaySpark] Server was already stopped.' -ForegroundColor Green
    exit 0
}
# Avoid terminating an unrelated process if the PID was recycled.
if ($proc.Name -ne 'cmd.exe' -or $proc.CommandLine -notlike '*server_console.bat*') {
    Write-Host '[DaySpark] Saved PID no longer belongs to DaySpark. No process was terminated.' -ForegroundColor Yellow
    exit 1
}
& taskkill.exe /PID $serverPid /T /F | Out-Host
if ($LASTEXITCODE -eq 0) {
    Remove-Item -LiteralPath $PidFile -Force
    Write-Host '[DaySpark] Stopped.' -ForegroundColor Green
} else {
    Write-Host '[DaySpark] Could not stop the process. Inspect Task Manager.' -ForegroundColor Red
    exit 1
}
