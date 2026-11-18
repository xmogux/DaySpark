/* Real browser/system smoke tests; runs against a live Spring Boot server. */
const fs = require('fs');
const path = require('path');
const {chromium} = require('playwright');
const assert = require('node:assert/strict');

const BASE = process.env.DAYSPARK_URL || 'http://127.0.0.1:8080';
const OUT = path.resolve(process.env.SYSTEM_EVIDENCE_DIR || 'system-test-evidence');
fs.mkdirSync(OUT, {recursive: true});
const results = [];
function stamp() { return new Date().toISOString(); }
function record(id, name, status, detail) {
  const entry = {id, name, status, detail: String(detail || ''), at: stamp()};
  results.push(entry);
  console.log('[' + status + '] ' + id + ' ' + name + (detail ? ' — ' + detail : ''));
  fs.writeFileSync(path.join(OUT, 'results.json'), JSON.stringify({base: BASE, cases: results}, null, 2));
}
async function screenshot(page, name) {
  await page.screenshot({path: path.join(OUT, name + '.png'), fullPage: true});
}
async function createDare(page, title, targetValue, targetUnit) {
  await page.goto(BASE + '/challenges/new');
  assert.equal(new URL(page.url()).pathname, '/challenges/new');
  await page.locator('input[name="title"]').fill(title);
  await page.locator('select[name="category"]').selectOption('STUDY');
  await page.locator('input[name="minutes"]').fill('1');
  await page.locator('input[name="targetValue"]').fill(String(targetValue));
  await page.locator('select[name="targetUnit"]').selectOption({label: targetUnit});
  await Promise.all([
    page.waitForURL('**/challenges', {timeout: 15000}),
    page.getByRole('button', {name: /도전 만들기/}).click()
  ]);
  await page.getByRole('heading', {name: title}).waitFor({state:'visible'});
}
(async () => {
 const browser = await chromium.launch({channel:'chrome', headless:true, args:['--no-sandbox']});
 const context = await browser.newContext({viewport:{width:1440,height:900}, locale:'ko-KR', timezoneId:'Asia/Seoul', acceptDownloads:false});
 const page = await context.newPage();
 page.setDefaultTimeout(15000);
 const id = Date.now().toString().slice(-8);
 let failures = 0;
 async function runTest(testId, name, fn) {
   try { await fn(); record(testId, name, 'PASS', '브라우저 상호작용 및 화면 결과 확인'); }
   catch (e) {
     failures++;
     record(testId, name, 'FAIL', e.stack || e.message || e);
     try { await screenshot(page, testId + '-failure'); } catch (_) {}
   }
 }
 try {
   await runTest('SYS-01', '도전 생성 → 오늘 할 일 → 완료 → 완료기록', async () => {
     const title = '시스템검증-완료-' + id, note = '브라우저 자동 테스트 완료 기록';
     await createDare(page, title, 1, '회');
     await screenshot(page, 'sys01-created');
     await page.goto(BASE + '/');
     const task = page.locator('.task-card').filter({has: page.getByRole('heading', {name:title})});
     await task.waitFor();
     await task.locator('button.complete-trigger').click();
     await page.locator('#completeModal[aria-hidden="false"]').waitFor();
     await page.locator('#note').fill(note);
     await Promise.all([
       page.waitForURL(BASE + '/', {timeout:15000}),
       page.locator('#completeModal form button[type=submit]').click()
     ]);
     const done = page.locator('.task-card.completed').filter({has:page.getByRole('heading', {name:title})});
     await done.waitFor();
     await screenshot(page, 'sys01-completed');
     await page.goto(BASE + '/history');
     await page.getByRole('heading', {name:title}).waitFor();
     assert.equal(await page.getByText(note).count(), 1, '완료 기록에 메모가 정확히 1건이어야 함');
     await screenshot(page, 'sys01-history');
   });

   await runTest('SYS-02', '실제 1분 집중 타이머 → 자동 완료 → 통계', async () => {
     const title = '시스템검증-집중-' + id;
     await createDare(page, title, 1, '분');
     await page.goto(BASE + '/focus');
     await page.locator('#focusDare').selectOption({label:title});
     await page.locator('#focusMinutes').fill('1');
     await page.locator('#breakMinutes').fill('0');
     await page.locator('#repeatCount').fill('1');
     await page.locator('#startTimer').click();
     await page.waitForFunction(() => document.querySelector('#timerStatus')?.textContent.includes('집중 시작'), null, {timeout:12000});
     await screenshot(page, 'sys02-running');
     // Intentionally wait for real elapsed time. A fake/mock clock would not prove E2E timer behavior.
     await page.waitForFunction(() => document.querySelector('#timerStatus')?.textContent.includes('반복 완료'), null, {timeout:105000, polling:500});
     await screenshot(page, 'sys02-finished');
     const summary = await page.request.get(BASE + '/api/focus/summary');
     assert.equal(summary.status(), 200);
     const json = await summary.json();
     assert.ok(json.todayMinutes >= 1, '오늘 집중 시간 1분 이상 누적');
     assert.ok(json.todayCount >= 1, '완료 회차 1회 이상');
     await page.goto(BASE + '/');
     const task = page.locator('.task-card.completed').filter({has:page.getByRole('heading', {name:title})});
     await task.waitFor();
     await screenshot(page, 'sys02-auto-completed');
     await page.goto(BASE + '/stats');
     await page.getByText('통계', {exact:false}).first().waitFor();
     await screenshot(page, 'sys02-stats');
   });

   await runTest('SYS-03', '빈 제목 입력 차단 + 서버 유효성 검증', async () => {
     await page.goto(BASE + '/challenges/new');
     const title = page.locator('input[name="title"]');
     await title.fill('');
     assert.equal(await title.evaluate(el => el.checkValidity()), false, '필수 제목 입력 필드의 브라우저 검증');
     await page.locator('form.challenge-form button[type="submit"]').click();
     assert.equal(new URL(page.url()).pathname, '/challenges/new', '브라우저가 잘못된 제출을 차단해야 함');
     await screenshot(page, 'sys03-browser-blocked');
     // Bypass HTML form validity to verify the server also rejects a blank title.
     const invalid = await page.request.post(BASE + '/challenges/save', {
       form:{title:' ',category:'STUDY',minutes:'1',repeatType:'DAILY',startDate:new Date().toLocaleDateString('en-CA', {timeZone:'Asia/Seoul'}),targetValue:'1',targetUnit:'회'}
     });
     assert.equal(invalid.status(), 200, '서버 검증 실패 후 신규 생성 양식으로 돌아와야 함');
     assert.ok(invalid.url().endsWith('/challenges/new'), '빈 제목 요청은 목록으로 가지 않고 생성 화면으로 돌아와야 함');
     assert.ok((await invalid.text()).includes('도전 이름을 입력해주세요.'), '서버 검증 오류 메시지가 표시되어야 함');
     await page.goto(BASE + '/challenges/new');
     await screenshot(page, 'sys03-form');
   });
 } finally {
   await browser.close();
   fs.writeFileSync(path.join(OUT,'results.json'), JSON.stringify({base:BASE, status:failures ? 'FAIL':'PASS', cases:results}, null, 2));
 }
 if (failures) process.exitCode=1;
})().catch(err => { console.error('FATAL', err); fs.writeFileSync(path.join(OUT,'fatal.txt'),String(err.stack||err)); process.exitCode=2; });
