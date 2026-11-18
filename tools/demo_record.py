from datetime import datetime, timedelta
from zoneinfo import ZoneInfo
from pathlib import Path
from playwright.sync_api import sync_playwright
import json
import sys

BASE = "http://127.0.0.1:8080"
DIR = Path("demo-output")
DIR.mkdir(exist_ok=True)
TODAY = datetime.now(ZoneInfo("Asia/Seoul")).date()
NAME = "CS231n 소프트맥스 학습 · 시연"

def step(message):
    print("DEMO_STEP:", message, flush=True)

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True, args=["--no-sandbox"])
    context = browser.new_context(
        viewport={"width": 1440, "height": 810},
        device_scale_factor=1,
        record_video_dir=str(DIR),
        record_video_size={"width": 1440, "height": 810},
        locale="ko-KR",
        timezone_id="Asia/Seoul",
        reduced_motion="reduce",
    )
    page = context.new_page()
    video = page.video
    def goto(path):
        page.goto(BASE + path, wait_until="domcontentloaded", timeout=30000)
        page.locator("body").wait_for()
        page.wait_for_timeout(700)
        assert page.locator("body").inner_text().strip()
    def caption(section, detail):
        page.evaluate("""({a,b}) => {
          let box = document.getElementById('real-demo-caption');
          if (!box) {
            box=document.createElement('div');
            box.id='real-demo-caption';
            document.body.appendChild(box);
          }
          box.setAttribute('style',
            'position:fixed;bottom:20px;left:25px;right:25px;z-index:2147483647;' +
            'background:rgba(20,26,20,.91);color:#f8f9f2;border-left:6px solid #d7ff63;' +
            'padding:15px 23px;border-radius:14px;font-family:sans-serif;pointer-events:none;' +
            'box-shadow:0 8px 32px rgba(0,0,0,.25)');
          box.innerHTML='';
          let h=document.createElement('div');h.textContent=a;h.style='font-size:20px;font-weight:750';
          let s=document.createElement('div');s.textContent=b;s.style='font-size:14px;color:#d3d9cc;margin-top:3px';
          box.append(h,s);
        }""", {"a":section,"b":detail})
    def hold(t=2):
        page.wait_for_timeout(int(t*1000))
    try:
        step("Home actual server")
        goto("/")
        caption("DaySpark · 실제 프로그램 시연", "GitHub 원본 Spring Boot 실행 · 브라우저 자동 조작 · 음성 없음")
        hold(4)
        step("Create challenge")
        goto("/challenges/new")
        caption("01. 목표 생성", "반복 요일 · 수치 목표 · 복습 계획을 함께 설정")
        page.locator('input[name="title"]').fill(NAME)
        page.locator('textarea[name="description"]').fill("매일 소프트맥스 개념을 복습하고 연습문제 풀기")
        page.locator('select[name="category"]').select_option("STUDY")
        page.locator('input[name="minutes"]').fill("25")
        page.locator('input[name="targetValue"]').fill("30")
        page.locator('input[name="weeklyTarget"]').fill("5")
        page.locator('input[name="startDate"]').fill(TODAY.isoformat())
        page.locator('input[name="repeatType"][value="DAILY"]').check()
        page.locator("#reviewEnabled").check()
        page.locator("#reviewBaseDate").fill((TODAY - timedelta(days=1)).isoformat())
        page.locator("#reviewIntervals").fill("1,3,7,14")
        hold(6)
        page.get_by_role("button", name="도전 만들기").click()
        page.wait_for_url("**/challenges")
        assert page.get_by_text(NAME).count() > 0, "Created challenge not visible"
        caption("목표 저장 완료", "입력한 반복 일정이 도전 목록에 반영된 것을 확인")
        hold(5)

        step("Challenge detail, measured progress")
        card = page.locator(".manage-card").filter(has_text=NAME)
        assert card.count() == 1
        card.get_by_role("link", name="상세·통계").click()
        page.wait_for_url("**/challenges/*")
        caption("02. 수치 진행량 기록", "입력·저장 후 진행률이 실제 서버 DB에 반영")
        page.locator('input[name="amount"]').fill("10")
        hold(2)
        page.get_by_role("button", name="직접 기록").click()
        assert "10 / 30" in page.locator(".today-quant").inner_text(), "Progress not persisted"
        hold(4)

        step("Schedule exception")
        caption("03. 일정 예외 처리", "반복되는 일정 중 하루를 쉬는 날로 변경")
        section = page.locator("section.panel").filter(has_text="일정 변경·휴식일").first
        section.locator('input[name="date"]').fill((TODAY + timedelta(days=2)).isoformat())
        section.locator('select[name="mode"]').select_option("SKIP")
        section.locator('input[name="note"]').fill("시연용 휴식일")
        section.get_by_role("button", name="일정 적용").click()
        assert "시연용 휴식일" in page.locator("body").inner_text()
        hold(5)

        step("Review plan")
        caption("04. 학습 복습 플랜", "설정한 복습 간격의 첫 번째 항목 완료")
        page.get_by_role("button", name="복습 완료").first.click()
        assert page.get_by_role("button", name="완료 취소").count() >= 1
        hold(4)

        step("Home completion")
        goto("/")
        caption("05. 오늘의 도전 완료", "완료 처리와 한 줄 메모를 실제로 저장")
        card = page.locator(".task-card").filter(has_text=NAME)
        assert card.count() == 1, "New challenge not on today's schedule"
        card.locator(".complete-trigger").click()
        assert "open" in page.locator("#completeModal").get_attribute("class")
        page.locator("#note").fill("소프트맥스 미분과 cross-entropy 복습 완료!")
        hold(3)
        page.get_by_role("button", name="완료 기록 저장").click()
        assert page.locator(".task-card").filter(has_text=NAME).get_by_text("완료됨").count() >= 1
        caption("완료 기록 저장 성공", "도전 상태가 완료로 바뀌고 달성률에 반영")
        hold(6)

        step("Timer real API")
        goto("/focus")
        caption("06. 포모도로 타이머", "실제 서버 세션 시작 → 일시정지 → 재개 → 종료")
        page.locator("#focusDare").select_option(label=NAME)
        page.locator("#focusMinutes").fill("1")
        page.locator("#breakMinutes").fill("0")
        page.locator("#repeatCount").fill("1")
        page.locator("#startTimer").click()
        page.wait_for_function("document.querySelector('#focusPhase').textContent.includes('FOCUS MODE')", timeout=18000)
        hold(6)
        page.locator("#pauseTimer").click()
        page.wait_for_timeout(1200)
        assert page.get_by_role("button",name="재개").is_visible()
        hold(3)
        page.locator("#pauseTimer").click()
        hold(4)
        page.locator("#resetTimer").click()
        hold(4)
        caption("타이머 조작 확인", "실제 경과한 집중 시간 기준으로 종료 기록")
        hold(3)

        step("Calendar")
        goto("/calendar")
        caption("07. 달력", "저장한 목표·완료 상태가 날짜별로 표시")
        hold(8)
        step("Stats")
        goto("/stats")
        caption("08. 통계", "주간·월간 달성률과 카테고리별 활동 내역")
        hold(8)
        step("History")
        goto("/history")
        assert page.get_by_text(NAME).count() > 0
        caption("09. 기록 조회", "앞서 실제로 저장한 도전 완료 기록 확인")
        hold(6)
        step("Finished")
        goto("/")
        caption("DaySpark · 시연 종료", "Spring Boot · Thymeleaf · JPA · H2 / 실제 앱 조작 완료")
        hold(4)
        print("DEMO_SUCCESS", flush=True)
    except Exception as e:
        print("DEMO_FAILURE", repr(e), flush=True)
        page.screenshot(path=str(DIR / "error.png"), full_page=True)
        raise
    finally:
        context.close()
        browser.close()
        src = Path(video.path())
        if src.exists():
            dst = DIR / "DaySpark_real_browser.webm"
            src.replace(dst)
            print("DEMO_VIDEO_PATH", str(dst), dst.stat().st_size, flush=True)
