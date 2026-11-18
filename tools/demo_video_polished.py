from datetime import datetime, timedelta
from zoneinfo import ZoneInfo
from pathlib import Path
from playwright.sync_api import sync_playwright
import time, json

ROOT="http://127.0.0.1:8080"
OUT=Path("demo-output")
OUT.mkdir(exist_ok=True)
TODAY=datetime.now(ZoneInfo("Asia/Seoul")).date()
TITLE="딥러닝 공부 · CNN과 역전파"
EVENTS=[]

with sync_playwright() as p:
    browser=p.chromium.launch(headless=True,args=["--no-sandbox"])
    context=browser.new_context(
        viewport={"width":1920,"height":930},device_scale_factor=1,
        record_video_dir=str(OUT), record_video_size={"width":1920,"height":930},
        locale="ko-KR",timezone_id="Asia/Seoul",reduced_motion="reduce",
    )
    page=context.new_page()
    vid=page.video
    clock=time.monotonic()

    def stamp(name, desc):
        t=round(time.monotonic()-clock,2)
        EVENTS.append({"t":t,"title":name,"subtitle":desc})
        print(f"SHOT {t:06.2f}s : {name}",flush=True)
    def hold(secs):
        page.wait_for_timeout(int(secs*1000))
    def styling():
        page.add_style_tag(content="""
            html{scroll-behavior:auto!important}
            .page-shell{width:min(1530px,calc(100% - 72px))!important}
            .topbar{height:72px!important}
            main{padding-top:25px!important;padding-bottom:46px!important}
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
            .detail-columns{gap:25px!important}
        """)
    def go(path):
        page.goto(ROOT+path,wait_until="domcontentloaded",timeout=30000)
        styling()
        page.evaluate("window.scrollTo(0,0)")
        hold(.6)
    def centered(locator):
        locator.scroll_into_view_if_needed(timeout=15000)
        hold(.7)
    def top():
        page.evaluate("window.scrollTo(0,0)")
        hold(.7)
    def timer_visible():
        b=page.locator("#timerVisual").bounding_box()
        c=page.locator(".timer-controls").bounding_box()
        assert b and c and b["y"]>=0 and b["y"]+b["height"]<620 and c["y"]+c["height"]<750, (b,c)
        print("TIMER_FULLY_VISIBLE",b,c,flush=True)
    def check_response():
        info=context.request.get(ROOT+"/api/focus/summary").json()
        print("FOCUS_SUMMARY",json.dumps(info,ensure_ascii=False),flush=True)
        return info

    try:
        seed=context.request.post(ROOT+"/demo-video/seed")
        assert seed.ok, (seed.status,seed.text())
        print("SAMPLE_DATA_CREATED",seed.json(),flush=True)
        stamp("DaySpark","학습 목표와 달성 기록을 관리하는 웹 애플리케이션")
        go("/")
        hold(4.5)

        stamp("여러 목표를 한곳에","딥러닝·PyTorch·논문 읽기 등 시연용 목표 확인")
        go("/challenges")
        hold(5)

        stamp("목표 추가","학습 일정·목표량·복습 계획 등록")
        go("/challenges/new")
        page.locator('input[name="title"]').fill(TITLE);hold(.45)
        page.locator('textarea[name="description"]').fill("매일 CNN 합성곱과 역전파를 공부하고 핵심 정리");hold(.4)
        page.locator('select[name="category"]').select_option("STUDY")
        page.locator('input[name="minutes"]').fill("25")
        page.locator('input[name="targetValue"]').fill("20")
        page.locator('select[name="targetUnit"]').select_option("페이지")
        page.locator('input[name="weeklyTarget"]').fill("5")
        page.locator('input[name="repeatType"][value="DAILY"]').check()
        page.locator("#reviewEnabled").check()
        page.locator("#reviewBaseDate").fill((TODAY-timedelta(days=1)).isoformat())
        page.locator("#reviewIntervals").fill("1,3,7,14")
        centered(page.locator("#reviewIntervals"))
        hold(2)
        centered(page.get_by_role("button",name="도전 만들기"))
        hold(1)
        page.get_by_role("button",name="도전 만들기").click()
        page.wait_for_load_state("domcontentloaded")
        assert page.get_by_text(TITLE).count()>=1

        stamp("등록 완료","새 목표가 실제 데이터베이스에 저장됨")
        top()
        hold(4.5)

        card=page.locator(".manage-card").filter(has_text=TITLE)
        assert card.count()==1
        card.get_by_role("link",name="상세·통계").click()
        page.wait_for_load_state("domcontentloaded")
        styling();top()

        stamp("진행량 기록","하루 목표 20페이지 중 8페이지를 입력하고 저장")
        inp=page.locator('input[name="amount"]')
        centered(inp)
        inp.fill("8")
        hold(1.7)
        page.get_by_role("button",name="직접 기록").click()
        page.wait_for_load_state("domcontentloaded")
        styling()
        assert "8 / 20" in page.locator(".today-quant").inner_text()
        centered(page.locator(".today-quant"))
        hold(4.0)

        stamp("일정 예외","다른 일정이 있는 날은 일회성 휴식일로 설정")
        box=page.locator("section.panel").filter(has_text="일정 변경·휴식일").first
        centered(box.locator('input[name="date"]'))
        box.locator('input[name="date"]').fill((TODAY+timedelta(days=3)).isoformat())
        box.locator('select[name="mode"]').select_option("SKIP")
        box.locator('input[name="note"]').fill("시험 준비일")
        hold(1.6)
        box.get_by_role("button",name="일정 적용").click()
        page.wait_for_load_state("domcontentloaded")
        styling()
        assert "시험 준비일" in page.locator("body").inner_text()
        box=page.locator("section.panel").filter(has_text="일정 변경·휴식일").first
        centered(box)
        hold(3.0)

        stamp("복습 관리","1·3·7·14일 간격으로 복습 완료 체크")
        review=page.locator("section.panel").filter(has_text="복습 플랜").first
        centered(review)
        review.get_by_role("button",name="복습 완료").first.click()
        page.wait_for_load_state("domcontentloaded")
        styling()
        assert page.get_by_role("button",name="완료 취소").count()>=1
        centered(page.locator("section.panel").filter(has_text="복습 플랜").first)
        hold(4)

        stamp("오늘의 목표 완료","메모를 입력하고 실제 완료 기록 저장")
        go("/")
        task=page.locator(".task-card").filter(has_text=TITLE)
        centered(task)
        task.locator(".complete-trigger").click()
        page.locator("#note").fill("CNN 합성곱과 역전파 연습문제 완료")
        hold(2.7)
        page.get_by_role("button",name="완료 기록 저장").click()
        assert page.locator(".task-card").filter(has_text=TITLE).get_by_text("완료됨").count()>=1
        centered(page.locator(".task-card").filter(has_text=TITLE))
        hold(4.4)

        stamp("집중 타이머","1분 세션을 실제로 완주해 기록으로 남기는 과정")
        go("/focus")
        timer_visible()
        page.locator("#focusDare").select_option(label=TITLE)
        page.locator("#focusMinutes").fill("1")
        page.locator("#breakMinutes").fill("0")
        page.locator("#repeatCount").fill("1")
        timer_visible()
        hold(2.5)

        before=check_response()
        assert before["weekCount"]==0 and before["weekMinutes"]==0, before
        stamp("집중 시작","실제 타이머를 시작합니다")
        page.locator("#startTimer").click()
        page.wait_for_function("document.querySelector('#focusPhase').textContent.includes('FOCUS MODE')",timeout=15000)
        timer_visible()
        hold(6)

        stamp("일시정지 후 재개","일시정지 동안 집중 시간이 증가하지 않는지 확인")
        page.locator("#pauseTimer").click()
        page.wait_for_function("document.querySelector('#pauseTimer').textContent.includes('재개')",timeout=12000)
        hold(2.5)
        page.locator("#pauseTimer").click()
        page.wait_for_function("document.querySelector('#pauseTimer').textContent.includes('일시정지')",timeout=12000)
        hold(3.0)

        stamp("실제 60초 경과","녹화 후 대기 구간만 시간 압축하고 결과는 실제 서버 값으로 검증")
        # Important: allow a full uninterrupted 60+ seconds, never fabricate elapsed time.
        end=time.monotonic()+64
        while time.monotonic()<end:
            hold(2)
            info=check_response()
            if info["weekCount"]>=1:
                break
        page.wait_for_function("document.querySelector('#focusPhase').textContent.includes('GET READY')",timeout=30000)
        after=check_response()
        assert after["weekCount"]==1 and after["weekMinutes"]>=1 and after["todayMinutes"]>=1,after
        stamp("1회 집중 완료","1분 집중 완료가 서버에 1회·1분으로 저장됨")
        timer_visible()
        hold(5)

        stamp("달력에서 확인","시연용 과거 기록과 방금 완료한 집중 세션을 날짜별로 조회")
        go("/calendar")
        hold(7.0)

        stamp("통계 자동 반영","오늘 집중 1분·완료 1회, 누적 목표 달성률 확인")
        go("/stats")
        maintext=page.locator("body").inner_text()
        assert "1분" in maintext and "1회 완료" in maintext, maintext[:3000]
        hold(8)

        stamp("완료 기록 조회","날짜별 학습 메모와 완료 기록 확인")
        go("/history")
        assert page.get_by_text(TITLE).count()>=1
        hold(6.5)

        stamp("DaySpark","계획 · 실행 · 복습 · 성과 분석까지 한 번에")
        go("/")
        hold(4)
        print("VIDEO_RECORDING_SUCCESS",flush=True)
    except Exception as e:
        print("VIDEO_RECORDING_ERROR",repr(e),flush=True)
        page.screenshot(path=str(OUT/"error.png"),full_page=True)
        raise
    finally:
        context.close()
        browser.close()
        path=Path(vid.path())
        if path.exists():
            path.replace(OUT/"polished_raw.webm")
        (OUT/"sections.json").write_text(json.dumps(EVENTS,ensure_ascii=False,indent=2),encoding="utf-8")
        print("TIMELINE",json.dumps(EVENTS,ensure_ascii=False),flush=True)
