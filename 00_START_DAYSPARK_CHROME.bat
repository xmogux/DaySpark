@echo off
setlocal
cd /d "%~dp0"
title DaySpark - Real Web App
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\launch_chrome.ps1" -Mode Full
if errorlevel 1 (
    echo.
    echo DaySpark could not start. Check the message above.
    pause
)
