@echo off
setlocal
cd /d "%~dp0.."
title DaySpark Background Server
set "hidden=0"
if /i "%~1"=="--hidden" set "hidden=1"
if not exist ".tools" mkdir ".tools"
if exist ".tools\one_click_server_failed.flag" del ".tools\one_click_server_failed.flag"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1" > ".tools\server_startup.log" 2>&1
set "rc=%ERRORLEVEL%"
if not "%rc%"=="0" (
    > ".tools\one_click_server_failed.flag" echo failed
    if "%hidden%"=="0" (
        echo.
        echo ===== DaySpark startup error - last 45 log lines =====
        powershell.exe -NoProfile -Command "Get-Content -LiteralPath '.tools\server_startup.log' -Tail 45"
        echo ====================================================
        pause
    )
    exit /b %rc%
)
if "%hidden%"=="0" (
    echo [DaySpark] Server stopped. Full log: .tools\server_startup.log
    pause
)
exit /b 0
