@echo off
setlocal
cd /d "%~dp0"
title DaySpark - Automated Tests (48)
echo.
echo ==================================================
echo  DaySpark: 28 unit + 6 integration + 14 Chrome
echo ==================================================
echo  All data is stored in a separate test database.
echo  Requires JDK 17+ and Google Chrome.
echo  Node.js and Playwright are installed if needed.
echo.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\run_all_tests.ps1"
set "RESULT=%ERRORLEVEL%"
echo.
if "%RESULT%"=="0" (
  echo [PASS] 48/48 - test-results\summary.html
) else (
  echo [CHECK] Some tests failed or did not run.
  echo         See test-results\summary.html
)
echo.
if /I not "%CI%"=="true" pause
exit /b %RESULT%
