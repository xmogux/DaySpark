@echo off
setlocal
cd /d "%~dp0"
title DaySpark - Instant Preview in Chrome
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\launch_chrome.ps1" -Mode Demo
if errorlevel 1 (
    echo.
    echo Chrome demo could not open.
    pause
)
