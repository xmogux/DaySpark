$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue' # Do not display blue per-download progress windows.
$ProjectDir = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $ProjectDir

Write-Host '[DaySpark] Checking Java...' -ForegroundColor Cyan
$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) {
    Write-Host 'Java was not found in PATH. Install JDK 17+ or fix PATH.' -ForegroundColor Red
    exit 1
}
try {
    $javaLines = @(& $java.Source --version)
    if ($LASTEXITCODE -ne 0 -or $javaLines.Count -eq 0) { throw 'java --version failed.' }
    $javaVersion = [string]$javaLines[0]
    if ($javaVersion -notmatch '^(?:openjdk|java)\s+(\d+)') { throw "Unrecognized Java version: $javaVersion" }
    $major = [int]$Matches[1]
    if ($major -lt 17) { throw "Java $major is too old. Java 17 or newer is required." }
    Write-Host "[DaySpark] $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "[DaySpark] Java check failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Reuse an existing Maven installation first; download once per PC if none exists.
$maven = Get-Command mvn -ErrorAction SilentlyContinue
if ($maven) {
    $mavenCmd = $maven.Source
    Write-Host '[DaySpark] Reusing Maven in PATH.' -ForegroundColor Cyan
} else {
    $version = '3.9.9'
    $localMaven = Join-Path $ProjectDir ".tools\apache-maven-$version"
    $localCmd = Join-Path $localMaven 'bin\mvn.cmd'
    $cacheBase = Join-Path $env:LOCALAPPDATA 'DaySpark\tools'
    $sharedMaven = Join-Path $cacheBase "apache-maven-$version"
    $sharedCmd = Join-Path $sharedMaven 'bin\mvn.cmd'
    if (Test-Path -LiteralPath $localCmd -PathType Leaf) {
        $mavenCmd = $localCmd
        Write-Host '[DaySpark] Reusing Maven from this project folder.' -ForegroundColor Cyan
        # Preserve the Maven distribution in a shared location for future DaySpark ZIPs.
        if (-not (Test-Path -LiteralPath $sharedCmd)) {
            try {
                New-Item -ItemType Directory -Path $cacheBase -Force | Out-Null
                Copy-Item -LiteralPath $localMaven -Destination $cacheBase -Recurse -Force
                Write-Host '[DaySpark] Maven cached for future DaySpark versions.' -ForegroundColor DarkGray
            } catch {
                Write-Host '[DaySpark] Could not copy Maven to shared cache; current version can still run.' -ForegroundColor Yellow
            }
        }
    } elseif (Test-Path -LiteralPath $sharedCmd -PathType Leaf) {
        $mavenCmd = $sharedCmd
        Write-Host '[DaySpark] Reusing shared Maven (no download).' -ForegroundColor Cyan
    } else {
        try {
            New-Item -ItemType Directory -Force -Path $cacheBase | Out-Null
            $zip = Join-Path $cacheBase "apache-maven-$version-bin.zip"
            $url = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$version/apache-maven-$version-bin.zip"
            Write-Host '[DaySpark] First-time Maven download (once per PC)...' -ForegroundColor Yellow
            Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
            Expand-Archive -Path $zip -DestinationPath $cacheBase -Force
            Remove-Item -LiteralPath $zip -Force
        } catch {
            Write-Host "[DaySpark] Maven installation failed: $($_.Exception.Message)" -ForegroundColor Red
            exit 1
        }
        if (-not (Test-Path -LiteralPath $sharedCmd)) {
            Write-Host '[DaySpark] Maven extraction failed.' -ForegroundColor Red
            exit 1
        }
        $mavenCmd = $sharedCmd
    }
}

Write-Host '[DaySpark] Starting Spring Boot...' -ForegroundColor Cyan
# Existing Maven dependency downloads remain cached in the normal ~/.m2/repository.
& $mavenCmd spring-boot:run
exit $LASTEXITCODE
