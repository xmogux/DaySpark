# One-click Windows test runner. ASCII-only source for Windows PowerShell 5.1.
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location -LiteralPath $root
$result = Join-Path $root 'test-results'
$tools = Join-Path $root '.tools'
$logs = Join-Path $result 'logs'
$javaReports = Join-Path $result 'junit'
$browserReports = Join-Path $result 'browser'
$coverage = Join-Path $result 'coverage'
$issues = [System.Collections.Generic.List[string]]::new()
$server = $null
$nodeExe = $null
$npmCmd = $null
$oldNativeError = $ErrorActionPreference

function Step([string]$message) {
    Write-Host ('[DaySpark] ' + $message)
}

function NativeToLog([string]$exe, [string[]]$arguments, [string]$logPath) {
    $previous = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        & $exe @arguments 2>&1 | Out-File -LiteralPath $logPath -Encoding UTF8
        return [int]$LASTEXITCODE
    } finally { $ErrorActionPreference = $previous }
}

function SetupNode {
    $node = Get-Command node.exe -ErrorAction SilentlyContinue
    $npm = Get-Command npm.cmd -ErrorAction SilentlyContinue
    if ($null -ne $node -and $null -ne $npm) {
        $versionString = [string](& $node.Source --version)
        $m = [regex]::Match($versionString, '^v([0-9]+)')
        if ($m.Success -and [int]$m.Groups[1].Value -ge 20) {
            return @{ Node = $node.Source; Npm = $npm.Source }
        }
    }
    $version = '22.16.0'
    $folder = 'node-v' + $version + '-win-x64'
    $nodeRoot = Join-Path $tools 'node'
    $nodePath = Join-Path (Join-Path $nodeRoot $folder) 'node.exe'
    if (-not (Test-Path -LiteralPath $nodePath)) {
        Step 'Downloading verified portable Node.js (initial setup only).'
        New-Item -ItemType Directory -Path $nodeRoot -Force | Out-Null
        $file = $folder + '.zip'
        $baseUrl = 'https://nodejs.org/dist/v' + $version + '/'
        $zipPath = Join-Path $nodeRoot $file
        $sumPath = Join-Path $nodeRoot 'SHASUMS256.txt'
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -UseBasicParsing -Uri ($baseUrl + $file) -OutFile $zipPath
        Invoke-WebRequest -UseBasicParsing -Uri ($baseUrl + 'SHASUMS256.txt') -OutFile $sumPath
        $sums = Get-Content -LiteralPath $sumPath -Raw
        $expected = [regex]::Match($sums, '(?m)^([a-fA-F0-9]{64})\s+' + [regex]::Escape($file) + '\s*$')
        if (-not $expected.Success) { throw 'Missing official SHA-256 checksum for Node.js.' }
        $actual = (Get-FileHash -LiteralPath $zipPath -Algorithm SHA256).Hash
        if ($actual -ine $expected.Groups[1].Value) { throw 'Node.js SHA-256 verification failed.' }
        Expand-Archive -LiteralPath $zipPath -DestinationPath $nodeRoot -Force
        Remove-Item -LiteralPath $zipPath,$sumPath -Force
    }
    if (-not (Test-Path -LiteralPath $nodePath)) { throw 'Node.js could not be installed.' }
    $nodeDir = Split-Path -Parent $nodePath
    $env:PATH = $nodeDir + ';' + $env:PATH
    return @{ Node = $nodePath; Npm = (Join-Path $nodeDir 'npm.cmd') }
}

function CheckChrome {
    $candidates = @(
        (Join-Path $env:ProgramFiles 'Google\Chrome\Application\chrome.exe'),
        (Join-Path ([Environment]::GetEnvironmentVariable('ProgramFiles(x86)')) 'Google\Chrome\Application\chrome.exe'),
        (Join-Path $env:LOCALAPPDATA 'Google\Chrome\Application\chrome.exe')
    )
    $found = $false
    foreach ($c in $candidates) {
        if ($null -ne $c -and (Test-Path -LiteralPath $c)) { $found = $true; break }
    }
    if (-not $found) { throw 'Google Chrome was not found. Install Chrome and rerun.' }
}

function SetupPlaywright {
    $installDir = Join-Path $tools 'playwright'
    $manifest = Join-Path $installDir 'node_modules\playwright\package.json'
    $valid = $false
    if (Test-Path -LiteralPath $manifest) {
        try { $valid = ((Get-Content -LiteralPath $manifest -Raw | ConvertFrom-Json).version -eq '1.56.1') }
        catch { $valid = $false }
    }
    if (-not $valid) {
        Step 'Installing Playwright 1.56.1 (initial setup only).'
        New-Item -ItemType Directory -Path $installDir -Force | Out-Null
        $exit = NativeToLog $npmCmd @('install','--prefix',$installDir,'--no-save','--package-lock=false','--ignore-scripts','--no-audit','--no-fund','playwright@1.56.1') (Join-Path $logs 'npm.log')
        if ($exit -ne 0 -or -not (Test-Path -LiteralPath $manifest)) { throw 'Playwright setup failed. See test-results/logs/npm.log.' }
    }
    $env:NODE_PATH = Join-Path $installDir 'node_modules'
}

function RunJava {
    Step 'Running 28 unit + 6 integration tests.'
    $mvn = Join-Path $root 'mvnw.cmd'
    if (-not (Test-Path -LiteralPath $mvn)) { throw 'mvnw.cmd is missing.' }
    $exit = NativeToLog $mvn @('-B','-ntp','clean','verify') (Join-Path $logs 'java-tests.log')
    $source = Join-Path $root 'target\surefire-reports'
    if (Test-Path -LiteralPath $source) {
        Get-ChildItem -LiteralPath $source -Filter 'TEST-*.xml' -File | Copy-Item -Destination $javaReports -Force
    }
    $jaCoCo = Join-Path $root 'target\site\jacoco'
    if (Test-Path -LiteralPath $jaCoCo) {
        Get-ChildItem -LiteralPath $jaCoCo -Force | Copy-Item -Destination $coverage -Recurse -Force
    }
    if ($exit -ne 0) { throw 'Maven build/tests failed. See test-results/logs/java-tests.log.' }
}

function StartIsolatedServer {
    Step 'Starting test-only Spring Boot server and database.'
    $jarFiles = @(Get-ChildItem -LiteralPath (Join-Path $root 'target') -Filter 'dayspark-*.jar' -File | Where-Object { $_.Name -notlike '*.original' })
    if ($jarFiles.Count -ne 1) { throw 'Could not find the single executable DaySpark jar.' }
    $listener = [System.Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback,0)
    $listener.Start()
    try { $port = $listener.LocalEndpoint.Port } finally { $listener.Stop() }
    $env:SERVER_PORT = [string]$port
    $env:SERVER_ADDRESS = '127.0.0.1'
    $env:SPRING_DATASOURCE_URL = 'jdbc:h2:file:./target/system-test-db'
    $env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'create-drop'
    $env:APP_SEED_DEMO_DATA = 'false'
    $env:APP_UPLOAD_DIR = 'target/system-test-uploads'
    $env:DAYSPARK_URL = 'http://127.0.0.1:' + $port
    $env:TZ = 'Asia/Seoul'
    $java = (Get-Command java.exe -ErrorAction Stop).Source
    $script:server = Start-Process -FilePath $java -ArgumentList @('-jar',('"' + $jarFiles[0].FullName + '"')) -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logs 'server-out.log') -RedirectStandardError (Join-Path $logs 'server-err.log')
    for ($i=0; $i -lt 120; $i++) {
        if ($script:server.HasExited) { break }
        try {
            $response = Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 -Uri ($env:DAYSPARK_URL + '/')
            if ($response.StatusCode -eq 200) { return }
        } catch { }
        Start-Sleep -Seconds 1
    }
    throw 'Isolated server did not start. See test-results/logs/server-err.log.'
}

function RunBrowser {
    Step 'Running 14 Chrome system tests.'
    $groups = @(
        @{ Name='basic'; Script='system_browser_test.cjs' },
        @{ Name='expanded'; Script='system_browser_more_test.cjs' },
        @{ Name='critical'; Script='system_browser_critical_test.cjs' }
    )
    foreach($g in $groups) {
        $env:SYSTEM_EVIDENCE_DIR = Join-Path $browserReports $g.Name
        New-Item -ItemType Directory -Path $env:SYSTEM_EVIDENCE_DIR -Force | Out-Null
        $exit = NativeToLog $nodeExe @((Join-Path $PSScriptRoot $g.Script)) (Join-Path $logs ($g.Name + '.log'))
        if ($exit -ne 0) { $issues.Add(($g.Name + ' browser tests failed (exit ' + $exit + ').')) }
    }
}

try {
    foreach($dir in @($javaReports,$browserReports,$coverage,$logs)) {
        if (Test-Path -LiteralPath $dir) { Remove-Item -LiteralPath $dir -Recurse -Force }
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
    if ($null -eq (Get-Command java.exe -ErrorAction SilentlyContinue)) {
        throw 'JDK 17 or newer is required.'
    }
    CheckChrome
    $nodeInfo = SetupNode
    $nodeExe = $nodeInfo.Node
    $npmCmd = $nodeInfo.Npm
    SetupPlaywright
    RunJava
    StartIsolatedServer
    RunBrowser
} catch {
    $issues.Add($_.Exception.Message)
    Write-Host ('[DaySpark] ERROR: ' + $_.Exception.Message) -ForegroundColor Red
} finally {
    if ($null -ne $server) {
        try {
            if (-not $server.HasExited) {
                Stop-Process -Id $server.Id -Force
                $server.WaitForExit(5000) | Out-Null
            }
        } catch { $issues.Add(('Could not stop isolated server: ' + $_.Exception.Message)) }
    }
    $reporter = Join-Path $PSScriptRoot 'write_test_report.ps1'
    $reportArgs = @('-NoProfile','-ExecutionPolicy','Bypass','-File',$reporter)
    foreach($issue in $issues) { $reportArgs += @('-Problem', [string]$issue) }
    # Pass reported problems via a UTF-8 file instead of repeated argument names.
    $problemsPath = Join-Path $result 'problems.txt'
    New-Item -ItemType Directory -Path $result -Force | Out-Null
    $issues.ToArray() | Set-Content -LiteralPath $problemsPath -Encoding UTF8
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $reporter -Root $root
    $reportExit = $LASTEXITCODE
    Write-Host ('[DaySpark] HTML report: ' + (Join-Path $result 'summary.html'))
    Write-Host ('[DaySpark] Text report: ' + (Join-Path $result 'summary.txt'))
    if ($env:CI -ne 'true' -and (Test-Path -LiteralPath (Join-Path $result 'summary.html'))) {
        try { Start-Process -FilePath (Join-Path $result 'summary.html') | Out-Null } catch { }
    }
}
exit $reportExit
