# DaySpark: one-click Chrome, visible error messages

1. Extract the entire ZIP into a NEW folder, not over an older project.
2. Double-click `01_OPEN_DAYSPARK_CHROME.bat` to start the real Spring Boot app. When it responds at port 8080, Chrome opens automatically.
3. If startup fails, the **actual error** is displayed in the launcher and in the now-visible `DaySpark Server` window, and is saved to `.tools/server_startup.log`.
4. To reproduce the error clearly, close other `DaySpark Server` windows and click `03_SHOW_STARTUP_ERROR.bat`.
5. Click `04_OPEN_STARTUP_LOG.bat` to see the complete log in Notepad.
6. To open the browser-only offline demo instead, click `02_OPEN_DEMO_CHROME.bat`. This does NOT use the real Spring Boot server or H2 database.

If troubleshooting with ChatGPT, copy the last 20-45 lines from the log, especially lines containing `[ERROR]`, `Caused by`, or `Exception`.

Requirements: Windows, Java 17+, Internet on first run if Maven and Spring dependencies are not already installed; Google Chrome optional (falls back to default browser). Java 25 and Spring Boot 3.5.16 should be compatible, but the actual runtime start has not been tested in this authoring environment.

The `reports/` folder contains the pre-existing report templates and reference materials.
