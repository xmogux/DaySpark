from datetime import datetime, timedelta
from zoneinfo import ZoneInfo
from pathlib import Path
from playwright.sync_api import sync_playwright
import re, json, time

BASE = "http://127.0.0.1:8080"
OUT = Path("demo-output")
OUT.mkdir(exist_ok=True)
TODAY = datetime.now(ZoneInfo("Asia/Seoul")).date()
NAME = "딥러닝 공부 · 역전파와 CNN"
SEQ = []

def log(s):
    print("CLEAN_DEMO:", s, flush=True)

def prereg(request, title, desc, target, unit):
    r = request.post(BASE+"/challenges/save", form={
       "title":title,"description":desc,"category":"STUDY","minutes":"25",
       "targetValue":str(target),"targetUnit":unit,"weeklyTarget":"4",
       "repeatType":"DAILY","startDate":(TODAY-timedelta(days=12)).isoformat(),
    })
    assert r.ok, (title, r.status)

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True, args=["--no-sandbox"])
    context = browser.new_context(
        viewport={"width":1920,"height":930},
        device_scale_factor=1,
        record_video_dir=str(OUT),
        record_video_size={"width":1920,"height":930},
        locale="ko-KR",
        timezone_id="Asia/Seoul",
        reduced_motion="reduce",
    )

    # Seed additional meaningful example goals, using real application endpoints.
    prereg(context.request, "딥러닝 강의 복습", "손실함수와 경사하강법 개념 정리", 2, "회")
    prereg(context.request, "PyTorch 모델 구현", "작은 CNN을 직접 구현하고 실험하기", 3, "회")
    prereg(context.request, "어텐션 논문 읽기", "Transformer 핵심 수식 분석", 8, "페이지")
    log("extra study goals created via actual backend")

    page=context.new_page()
    video=page.video
    clock=time.monotonic()
    def mark(title, sub):
        t=round(time.monotonic()-clock,2)
        SEQ.append({"t":t,"title":title,"subtitle":sub})
        log(f"SECTION {t}s {title}")
    def hold(t=2):
        page.wait_for_timeout(int(t*1000))
    def css():
        # Recording-only presentation framing. The application source is unchanged.
        page.add_style_tag(content="""
            html {scroll-behavior:auto!important}
            .page-shell{width:min(1530px,calc(100% - 72px))!important}
            .topbar{height:72px!important}
            main{padding-top:25px!important;padding-bottom:46px!important}
            .hero-grid{gap:32px!important;margin-bottom:25px!important}
            .section-block{margin-top:30px!important}
            .focus-layout{min-height:0!important;align-items:start!important;gap:42px!important}
            .focus-console{padding:20px 30px!important;border-radius:26px!important}
            .timer-ring{width:245px!important;max-width:245px!important;margin:3px auto 6px!important}
            .timer-numbers strong{font-size:62px!important}
            .visual-timer-meters{gap:7px!important;padding:0 0 12px!important}
            .timer-config{margin-top:12px!important;padding-top:14px!important;gap:9px!important}
            .focus-note{display:none!important}
            .focus-intro h1{font-size:54px!important}
            .focus-summary{margin:24px 0!important}
            .timer-controls{margin-top:6px!important}
        """)
    def goto(path):
        page.goto(BASE+path, wait_until="domcontentloaded", timeout=30000)
        page.locator("body").wait_for()
        css()
        page.evaluate("window.scrollTo(0,0)")
        hold(.65)
        assert page.locator("body").inner_text().strip()
    def top():
        page.evaluate("window.scrollTo(0,0)")
        hold(.45)
    def assert_timer():
        top()
        b=page.locator("#timerVisual").bounding_box()
        c=page.locator(".timer-controls").bounding_box()
        log("timer positions: "+json.dumps({"ring":b,"controls":c},ensure_ascii=False))
        assert b and b["y"] > 70 and b["y"] + b["height"] < 660, "Timer ring not fully visible"
        assert c and c["y"] + c["height"] < 900, "Timer buttons off-screen"

    mark("DaySpark · 실제 웹앱 시연","딥러닝 학습 계획부터 완료 기록과 통계까지")
    try:
        goto("/")
        hold(4)
        mark("목표 관리 · 여러 학습 계획","딥러닝 강의·모델 구현·논문 읽기 등 목표를 한곳에")
        goto("/challenges")
        hold(5)

        mark("새 목표 등록","딥러닝 공부와 반복 요일·수치 목표를 설정")
        goto("/challenges/new")
        page.locator('input[name="title"]').fill(NAME)
        page.locator('textarea[name="description"]').fill("신경망 역전파와 CNN 구조를 정리하고 연습문제 풀기")
        page.locator('select[name="category"]').select_option("STUDY")
        page.locator('input[name="minutes"]').fill("25")
        page.locator('input[name="targetValue"]').fill("20")
        page.locator('select[name="targetUnit"]').select_option("페이지")
        page.locator('input[name="weeklyTarget"]').fill("5")
        page.locator('input[name="startDate"]').fill(TODAY.isoformat())
        page.locator('input[name="repeatType"][value="DAILY"]').check()
        page.locator("#reviewEnabled").check()
        page.locator("#reviewBaseDate").fill((TODAY-timedelta(days=1)).isoformat())
        page.locator("#reviewIntervals").fill("1,3,7,14")
        # Keep relevant form fields in frame
        page.locator("#reviewIntervals").scroll_into_view_if_needed()
        hold(3.0)
        page.get_by_role("button",name="도전 만들기").click()
        page.wait_for_load_state("domcontentloaded")
        assert page.get_by_text(NAME).count() > 0

        mark("등록 결과 확인","새 학습 목표가 도전 목록에 저장됨")
        top()
        hold(4)
        card=page.locator(".manage-card").filter(has_text=NAME)
        assert card.count()==1
        card.get_by_role("link",name="상세·통계").click()
        page.wait_for_load_state("domcontentloaded")
        css()
        top()

        mark("수치 목표 · 진행량 기록","20페이지 목표에 8페이지를 실제로 입력·저장")
        page.locator('input[name="amount"]').fill("8")
        hold(1.4)
        page.get_by_role("button",name="직접 기록").click()
        assert "8 / 20" in page.locator(".today-quant").inner_text()
        top()
        hold(5)

        mark("일정 예외 처리","특정 날짜를 쉬는 날로 지정하고 저장")
        section=page.locator("section.panel").filter(has_text="일정 변경·휴식일").first
        section.locator('input[name="date"]').fill((TODAY+timedelta(days=3)).isoformat())
        section.locator('select[name="mode"]').select_option("SKIP")
        section.locator('input[name="note"]').fill("중간고사 준비")
        hold(1.5)
        section.get_by_role("button",name="일정 적용").click()
        assert "중간고사 준비" in page.locator("body").inner_text()
        top()
        hold(3.3)

        mark("복습 계획 · 완료 체크","1·3·7·14일 복습 중 첫 번째 복습 완료")
        reviews=page.locator("section.panel").filter(has_text="복습 플랜").first
        reviews.scroll_into_view_if_needed()
        hold(1.5)
        reviews.get_by_role("button",name="복습 완료").first.click()
        assert page.get_by_role("button",name="완료 취소").count() >= 1
        page.locator("section.panel").filter(has_text="복습 플랜").first.scroll_into_view_if_needed()
        hold(5.5)

        mark("오늘 목표 완료 · 메모 저장","목표를 완료하고 학습 메모를 기록")
        goto("/")
        card=page.locator(".task-card").filter(has_text=NAME)
        assert card.count()==1
        card.locator(".complete-trigger").click()
        page.locator("#note").fill("역전파 체인룰과 CNN 합성곱 계산 연습 완료")
        hold(3.0)
        page.get_by_role("button",name="완료 기록 저장").click()
        assert page.locator(".task-card").filter(has_text=NAME).get_by_text("완료됨").count()>=1
        top()
        hold(5.0)

        mark("복수 목표의 완료 상태","하루의 서로 다른 목표들도 완료 처리")
        # Demonstrate more representative recorded results.
        for title, note in [
            ("딥러닝 강의 복습","경사하강법 강의 정리"),
            ("PyTorch 모델 구현","CNN 모형 구현 실습"),
            ("물 6잔 마시기","오늘 물 섭취 목표 달성")
        ]:
            card=page.locator(".task-card").filter(has_text=title)
            if card.count() == 0:
                continue
            card.locator(".complete-trigger").click()
            page.locator("#note").fill(note)
            page.get_by_role("button",name="완료 기록 저장").click()
            assert page.locator(".task-card").filter(has_text=title).get_by_text("완료됨").count()>=1
            hold(1.0)
        top()
        hold(4.5)

        mark("포모도로 · 집중 시간","타이머 전체 화면에서 시작·일시정지·재개·종료")
        goto("/focus")
        assert_timer()
        page.locator("#focusDare").select_option(label=NAME)
        page.locator("#focusMinutes").fill("1")
        page.locator("#breakMinutes").fill("0")
        page.locator("#repeatCount").fill("1")
        assert_timer()
        hold(2.2)
        page.locator("#startTimer").click()
        page.wait_for_function("document.querySelector('#focusPhase').textContent.includes('FOCUS MODE')",timeout=18000)
        assert_timer()
        hold(8)
        page.locator("#pauseTimer").click()
        assert page.get_by_role("button",name="재개").is_visible()
        assert_timer()
        hold(3)
        page.locator("#pauseTimer").click()
        assert_timer()
        hold(5)
        page.locator("#resetTimer").click()
        assert_timer()
        hold(4)

        mark("달력 · 날짜별 기록","완료된 목표와 복습 예정일을 날짜별로 확인")
        goto("/calendar")
        hold(7)

        mark("통계 · 학습 성과","달성률·카테고리·집중 기록의 요약 확인")
        goto("/stats")
        hold(8)

        mark("완료 기록 · 메모","앞서 저장한 여러 완료 기록을 다시 조회")
        goto("/history")
        assert page.get_by_text(NAME).count() > 0
        hold(8)

        mark("DaySpark · 시연 종료","계획 → 실행 → 기록 → 통계로 이어지는 학습 관리")
        goto("/")
        hold(4)
        log("SUCCESS")
    except Exception as e:
        log("FAILURE "+repr(e))
        page.screenshot(path=str(OUT/"failure.png"),full_page=True)
        raise
    finally:
        context.close()
        browser.close()
        path=Path(video.path())
        if path.exists():
            new=OUT/"clean_raw.webm"
            path.replace(new)
            log("RECORDED "+str(new)+" bytes="+str(new.stat().st_size))
        (OUT/"sections.json").write_text(json.dumps(SEQ,ensure_ascii=False,indent=2),encoding="utf-8")
