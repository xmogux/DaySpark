# Produce a truthful 48-case HTML/text report from actual JUnit XML and Chrome JSON.
param([Parameter(Mandatory=$true)][string]$Root)
$ErrorActionPreference = 'Stop'
$result = Join-Path $Root 'test-results'
$problems = [System.Collections.Generic.List[string]]::new()
$java = [System.Collections.Generic.List[object]]::new()
$browser = [System.Collections.Generic.List[object]]::new()

function EscapeText([object]$value) {
    return [System.Net.WebUtility]::HtmlEncode([string]$value)
}

$problemFile = Join-Path $result 'problems.txt'
if (Test-Path -LiteralPath $problemFile) {
    foreach ($line in @(Get-Content -LiteralPath $problemFile -Encoding UTF8)) {
        if (-not [string]::IsNullOrWhiteSpace($line)) { $problems.Add($line) }
    }
}

$unitFiles = 0
$integrationFiles = 0
$junitPath = Join-Path $result 'junit'
foreach ($file in @(Get-ChildItem -LiteralPath $junitPath -Filter 'TEST-*.xml' -File -ErrorAction SilentlyContinue)) {
    try {
        [xml]$doc = Get-Content -LiteralPath $file.FullName -Raw -Encoding UTF8
        $kind = if ($file.Name -match 'DaySpark(More)?IntegrationTest') { 'Integration' } else { 'Unit' }
        if ($kind -eq 'Integration') { $integrationFiles++ } else { $unitFiles++ }
        foreach ($test in @($doc.testsuite.testcase)) {
            if ($null -eq $test) { continue }
            $state = 'PASS'
            if ($null -ne $test.failure -or $null -ne $test.error) { $state = 'FAIL' }
            elseif ($null -ne $test.skipped) { $state = 'SKIP' }
            $java.Add([pscustomobject]@{
                Id = [string]$test.name
                Label = [string]$test.classname
                Category = $kind
                State = $state
            })
        }
    } catch {
        $problems.Add(('Could not read JUnit report ' + $file.Name + ': ' + $_.Exception.Message))
    }
}

$allBrowser = @{}
$browserFiles = @(
    (Join-Path $result 'browser\basic\results.json'),
    (Join-Path $result 'browser\expanded\results.json'),
    (Join-Path $result 'browser\critical\results.json')
)
foreach($path in $browserFiles) {
    if (-not (Test-Path -LiteralPath $path)) { continue }
    try {
        $json = Get-Content -LiteralPath $path -Encoding UTF8 -Raw | ConvertFrom-Json
        foreach($item in @($json.cases)) {
            $id = [string]$item.id
            if ($allBrowser.ContainsKey($id)) {
                $problems.Add(('Duplicate system-test ID: ' + $id))
            } else {
                $allBrowser[$id] = $item
            }
        }
    } catch {
        $problems.Add(('Invalid browser result JSON: ' + $path))
    }
}
for($n=1; $n -le 14; $n++) {
    $id = ('SYS-{0:00}' -f $n)
    if ($allBrowser.ContainsKey($id)) {
        $case = $allBrowser[$id]
        $browser.Add([pscustomobject]@{
            Id = $id
            Label = [string]$case.name
            Category = 'Chrome'
            State = if ([string]$case.status -eq 'PASS') { 'PASS' } else { 'FAIL' }
        })
    } else {
        $browser.Add([pscustomobject]@{
            Id = $id
            Label = 'Not executed'
            Category = 'Chrome'
            State = 'NOT RUN'
        })
    }
}

$units = @($java.ToArray() | Where-Object { $_.Category -eq 'Unit' })
$integrations = @($java.ToArray() | Where-Object { $_.Category -eq 'Integration' })
$unitPass = @($units | Where-Object { $_.State -eq 'PASS' }).Count
$integrationPass = @($integrations | Where-Object { $_.State -eq 'PASS' }).Count
$browserPass = @($browser.ToArray() | Where-Object { $_.State -eq 'PASS' }).Count
$totalPass = $unitPass + $integrationPass + $browserPass

if ($units.Count -ne 28) { $problems.Add(('Unit tests: expected 28, executed ' + $units.Count)) }
if ($integrations.Count -ne 6) { $problems.Add(('Integration tests: expected 6, executed ' + $integrations.Count)) }
if ($unitPass -ne 28) { $problems.Add(('Unit tests passed: ' + $unitPass + '/28')) }
if ($integrationPass -ne 6) { $problems.Add(('Integration tests passed: ' + $integrationPass + '/6')) }
if ($browserPass -ne 14) { $problems.Add(('Browser tests passed: ' + $browserPass + '/14')) }

$ok = ($totalPass -eq 48 -and $problems.Count -eq 0)
$overall = if ($ok) { 'PASS' } else { 'FAIL / INCOMPLETE' }
$summary = @(
  'DaySpark - Windows automated test results'
  ('Overall: ' + $overall)
  ('Unit: ' + $unitPass + '/28 passed')
  ('Integration: ' + $integrationPass + '/6 passed')
  ('Chrome: ' + $browserPass + '/14 passed')
  ('Total: ' + $totalPass + '/48 passed')
  'Personal database: not used (isolated test DB)'
  ''
  'Errors and missing checks:'
)
if ($problems.Count -eq 0) { $summary += 'None' }
else { $summary += $problems.ToArray() }
$summary += @('', 'JUnit XML: test-results/junit', 'Chrome JSON/screenshots: test-results/browser', 'JaCoCo coverage: test-results/coverage', 'Logs: test-results/logs')
Set-Content -LiteralPath (Join-Path $result 'summary.txt') -Value $summary -Encoding UTF8

$rows = [System.Collections.Generic.List[string]]::new()
foreach ($item in (@($java.ToArray()) + @($browser.ToArray()))) {
    $style = if ($item.State -eq 'PASS') { 'ok' } elseif ($item.State -eq 'NOT RUN') { 'skipped' } else { 'bad' }
    $rows.Add('<tr><td>' + (EscapeText $item.Category) + '</td><td>' +
        (EscapeText $item.Id) + '</td><td>' +
        (EscapeText $item.Label) + '</td><td class="' + $style + '">' +
        (EscapeText $item.State) + '</td></tr>')
}
$errorRows = if ($problems.Count -eq 0) { '<li>None</li>' }
    else { ($problems.ToArray() | ForEach-Object { '<li>' + (EscapeText $_) + '</li>' }) -join [Environment]::NewLine }
$overallStyle = if ($ok) { 'ok' } else { 'bad' }
$h = @"
<!doctype html>
<html lang="ko">
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>DaySpark | 48 automated tests</title>
<style>
:root{font-family:"Malgun Gothic",Arial,sans-serif;background:#f4f6f9;color:#1b2535}
body{max-width:1100px;margin:36px auto;padding:0 24px}
h1{margin:0 0 7px}p{color:#586779;line-height:1.5}
.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(165px,1fr));gap:13px;margin:26px 0}
.card{background:#fff;border:1px solid #dce2e9;border-radius:12px;padding:20px}
.card b{display:block;font-size:27px;margin-top:9px}
.card small{color:#5c6576}
table{width:100%;border-collapse:collapse;background:#fff;font-size:13px}
th,td{text-align:left;padding:11px;border-bottom:1px solid #e8ebf0;vertical-align:top}
th{background:#eef2f6}.ok{color:#157a48;font-weight:700}.bad{color:#b12929;font-weight:700}
.skipped{color:#726b58;font-weight:700}section{margin:28px 0}
a{color:#205d9f}
</style></head>
<body>
<h1>DaySpark - automated test results</h1>
<p>Windows / JUnit 5 / Google Chrome + Playwright / isolated Spring Boot H2 test DB</p>
<div class="cards">
<div class="card"><small>Overall</small><b class="$overallStyle">$overall</b></div>
<div class="card"><small>Unit tests</small><b>$unitPass / 28</b></div>
<div class="card"><small>Integration tests</small><b>$integrationPass / 6</b></div>
<div class="card"><small>Chrome system tests</small><b>$browserPass / 14</b></div>
<div class="card"><small>Total passed</small><b>$totalPass / 48</b></div>
</div>
<section><h2>Individual test results</h2>
<table><thead><tr><th>Type</th><th>Test or scenario</th><th>Class / description</th><th>Status</th></tr></thead>
<tbody>
$($rows -join [Environment]::NewLine)
</tbody></table></section>
<section><h2>Errors or tests not executed</h2><ul>$errorRows</ul></section>
<section><h2>Evidence</h2><p>
<a href="junit/">JUnit XML</a> |
<a href="browser/">Chrome JSON and screenshots</a> |
<a href="coverage/index.html">JaCoCo line and branch coverage</a> |
<a href="logs/">Execution logs</a>
</p><p>Results are derived from test output files, not hardcoded. Failed and missing tests never count as PASS.</p></section>
</body></html>
"@
Set-Content -LiteralPath (Join-Path $result 'summary.html') -Value $h -Encoding UTF8
Write-Host ('[DaySpark] ' + $overall + ' (' + $totalPass + '/48 passed)')
if ($ok) { exit 0 } else { exit 1 }
