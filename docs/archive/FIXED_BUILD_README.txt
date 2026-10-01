DaySpark build fix (2026-09)
================================
The prior zip omitted the required version for
org.thymeleaf.extras:thymeleaf-extras-java8time.

This version includes <version>3.0.4.RELEASE</version> in pom.xml.
Java 25 settings do not need to change. The Jansi/Unsafe warnings
are not the cause of the previous build failure.

TO RUN (Windows + Chrome)
1. Extract this zip into a NEW folder (avoid mixing old files).
2. Double click dayspark\01_OPEN_DAYSPARK_CHROME.bat.
3. The launcher starts Spring Boot, then opens Google Chrome
   only after localhost:8080 responds.
4. If there is another error, see the displayed server log.

QUICK FIX FOR YOUR ALREADY EXTRACTED FOLDER:
Copy the separately supplied pom.xml into the dayspark folder
and replace the old file, then rerun 01_OPEN_DAYSPARK_CHROME.bat.

LIMIT: Static POM and ZIP validation passed. A Windows end-to-end
Spring Boot launch is NOT verified in the authoring environment.
