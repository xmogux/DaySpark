DaySpark Windows Java detection fix (JDK 25)

Problem
  Previous scripts/run.ps1 ran: java -version 2>&1
  On Windows PowerShell 5.1, Java's normal stderr version output can be treated
  as a terminating error when $ErrorActionPreference='Stop'. Java was installed!

Fix
  - Detect Java with Get-Command java.
  - Check JDK 17+ using java --version, which outputs to stdout.
  - Update Spring Boot 3.3.5 -> 3.5.16 for officially supported JDK 25.
  - Retain the existing app source and report templates.

To run
  1. Extract all files in this ZIP into a NEW folder.
  2. Open the extracted dayspark folder.
  3. Double-click run.bat; first launch requires Internet for Maven deps.
  4. When server has started, open http://localhost:8080

Troubleshooting in CMD
  where java
  java --version
  echo %JAVA_HOME%
  mvn -version     (only if manually installed)

Note: Static demo.html is a separate browser-only demo. It is not the DB-backed app.
The full server startup on your Windows/JDK 25 machine has NOT been verified here.
Source: https://docs.spring.io/spring-boot/3.5/system-requirements.html
