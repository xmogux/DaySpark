@echo off
setlocal
cd /d "%~dp0"
if exist ".tools\server_startup.log" (
  start "" notepad.exe ".tools\server_startup.log"
) else (
  echo No error log found yet. Run the app first.
  pause
)
