@echo off
setlocal
rem ---------------------------------------------------------------------------
rem DaySpark launcher for Windows 10/11
rem
rem  1. Checks that Java is installed and that port 8080 is free.
rem  2. Starts the Spring Boot server in THIS window (Maven Wrapper).
rem  3. Opens the default browser once http://localhost:8080/ responds.
rem
rem Stop the server: press Ctrl+C in this window, or close the window.
rem Messages are in English on purpose: cmd.exe code pages garble UTF-8 Korean.
rem ---------------------------------------------------------------------------
cd /d "%~dp0"
title DaySpark server

set "PORT=8080"
set "URL=http://localhost:%PORT%/"

where java >nul 2>nul
if errorlevel 1 (
    echo [DaySpark] Java was not found. Install JDK 17 or newer, then run this file again.
    echo            Download: https://adoptium.net/
    pause
    exit /b 1
)

set "MVN="
if exist "mvnw.cmd" set "MVN=mvnw.cmd"
if not defined MVN (
    where mvn >nul 2>nul
    if not errorlevel 1 set "MVN=mvn"
)
if not defined MVN (
    echo [DaySpark] mvnw.cmd is missing and Maven is not installed.
    echo            See README.md, section "Maven Wrapper".
    pause
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -Command "try { if (Get-NetTCPConnection -LocalPort %PORT% -State Listen -ErrorAction SilentlyContinue) { exit 1 } } catch { }; exit 0"
if errorlevel 1 (
    echo [DaySpark] Port %PORT% is already in use.
    echo            Close the other server window ^(for example an older DaySpark^) and try again.
    pause
    exit /b 1
)

echo [DaySpark] Starting the server with %MVN% ...
echo [DaySpark] The first run downloads dependencies and can take a few minutes.
echo [DaySpark] Your default browser opens automatically when the server is ready.
echo [DaySpark] To stop the server: press Ctrl+C here, or close this window.
echo.

rem Background watcher: polls the server and opens the default browser once.
start "" /b powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference = 'SilentlyContinue'; for ($i = 0; $i -lt 900; $i++) { try { $r = Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 -Uri '%URL%'; if ($r.StatusCode -eq 200) { Start-Process '%URL%'; break } } catch { }; Start-Sleep -Seconds 1 }"

call %MVN% spring-boot:run
echo.
echo [DaySpark] Server stopped.
pause
endlocal
