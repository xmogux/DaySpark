# DaySpark Changelog

## v5 — bug-fix and presentation-template release

- Fixed focus session completion date calculation.
- Removed write side effect from GET `/api/focus/current`.
- Added enum-based input validation helpers for repeat type, category, schedule mode, and focus status.
- Added `pausedFrom` so pausing a challenge no longer changes historical calendar/stat denominators.
- Updated overall Streak to count scheduled days only.
- Added global error handling and an error page.
- Removed obsolete Thymeleaf Java 8 time dependency.
- Cleaned top-level README and moved older version-specific notes to `docs/archive/` (removed in v5.6).
- Added new software-engineering presentation template based on Calvin CS262 and Rutgers SE demo guidance.

## v5.1 — compilation hotfix / quiet Chrome launch

- Fixed Java 17 lambda capture compilation error at `CompletionService.java` in streak calculation. Historical schedule query is also performed once instead of each loop iteration.
- Run the server in the background without the extra blue download/server console. Show genuine startup errors in the launcher, with full `.tools/server_startup.log` retained.
- Reuse Maven from PATH or `.tools`, copy local installation to shared `%LOCALAPPDATA%/DaySpark/tools` once, and share it between extracted releases. Download only if absent.
- Added `03_STOP_DAYSPARK.bat` for stopping the app server after closing Chrome.
- Kept all original reports and the Calvin/Rutgers-based PPTX template.


## v5.4 — monthly calendar visual redesign

- New 7-column rounded calendar day cards with clear completed / partial / scheduled / unfinished / no-plan states, replacing neon strips on every date.
- Added monthly summary cards for completed challenges, completion ratio through today, and actual completed focus minutes & rounds.
- Date links keep the existing drill-down functionality; restyled the day details screen.
- Added v5.4 page marker + updated launcher version check and CSS cache-busting across pages, to avoid silently opening a stale v5.3 process.
- Added a static local Chrome preview with illustrative example data and a MockMvc test to check the calendar view model and markup. Full Maven tests require an installed Maven and access to cached dependencies.

## v5.5 — Study Review Plan
- STUDY 도전에 사용자 지정 복습 기준일/간격 설정 추가
- 오늘 복습, 도전 상세 복습 타임라인, 월간 달력 복습 배지, 날짜 상세 복습 완료 처리 추가
- 복습 완료 기록을 일반 도전 완료 통계와 분리 저장

## v5.6 — 제출 준비: 일관성 · 테스트 · Git/CI (첫 Git 이력)

이 버전부터 Git으로 관리합니다. 커밋 단위의 상세 이력은 `git log --graph`, 요약은 `docs/process.md`.

**실행 환경**
- Java 17 기준으로 통일(pom · README · CI), Maven Wrapper 사용 안내
- 실행기를 `RUN_DAYSPARK.bat` 하나로 정리(기본 브라우저, 8080 포트 확인). v5.4 표식을 기다리다 v5.5에서 브라우저를 열지 못하던 문제 제거
- 서버를 127.0.0.1에만 바인딩. 브라우저 전용 데모(`demo.html`)와 미리보기 파일 제거

**버그 수정 (모두 실패 테스트를 먼저 커밋)**
- 반복 요일 변경 후 주간 달성률 200% → "하루의 계획 = 예정 + 그날 완료" 규칙으로 해결
- ADD → 완료 → RESET 시 완료가 계획 없는 날에 남던 문제 → 완료한 날짜는 SKIP·RESET 불가
- 지난 복습이 오늘 화면에서 사라지던 문제 → 완료할 때까지 "N일 지남"으로 표시
- 폼이 서버가 거부하는 MIND 카테고리를 보여주던 문제(WORK 누락)
- 404·405가 500으로 바뀌고 오류가 로그에 남지 않던 문제
- 타이머 finish 실패 시 브라우저가 초당 수백 번 재요청하던 문제
- (리팩터링 중 발견) v5 이전 집중 기록이 있으면 도전 상세가 500 오류

**기능 연결**
- 오늘 완료 취소(사진도 삭제)
- 포모도로 → 수치 목표 진행량 → 목표 첫 도달 시 자동 완료(계획된 날만, 자정 경계 안전)

**규칙 고정 (`docs/domain-rules.md`)**
- 복습 플랜 = 하나의 학습 단위, Streak = 계획일에 1개 이상 완료, 목표 미달 수동 완료 허용, 쉬기는 과거에 영향 없음

**구조**
- `DareRequest` DTO로 생성/수정 흐름 하나로(파라미터 16개 메서드 제거), `ReviewIntervals` 값 객체, `StreakCalculator`
- 화면 계산을 `StatsService`로 모으고 컨트롤러를 영역별로 분리, 내비게이션 fragment, 코드 포맷 통일

**테스트·문서**
- 고정 시계(2026-10-07) 기반 테스트 55개(단위 20 + 통합 35), JaCoCo, GitHub Actions(JDK 17·25)
- 요구사항·수용 기준, 설계(ERD·클래스·시퀀스·상태), 테스트, 개발 과정, 한계 문서

## v5.7 — 집중 타이머: 실제 집중 시간 기록과 도전 전환

**문제:** 25분 타이머를 12분 만에 멈추면 12분이 통째로 사라졌습니다(중단 = `CANCELLED`, 기록 없음).
다른 도전으로 바꾸려면 중단(기록 삭제) → 다시 시작밖에 없었습니다.

**바뀐 점** (규칙: `docs/domain-rules.md` 7.1)
- "실제 집중한 시간"과 "끝까지 완료한 세션"을 분리. 새 상태 `STOPPED`
  - **종료 및 기록**(이전 "중단"): 그때까지 집중한 시간(일시정지 제외)을 오늘·주·월·달력·도전 상세 집중 시간에 기록.
    완료 횟수·진행량·자동 완료에는 넣지 않음
  - 끝까지 마친 세션은 이전과 같음(시간 + 1회, 조건 충족 시 자동 완료)
  - 계획 시간이 이미 지난 세션을 종료하면 정상 완료로 처리
- 세션 중 다른 도전을 고르면 확인창 → 이전 세션을 기록·종료하고 새 세션 시작(`POST /api/focus/switch`, 한 트랜잭션)
- 날짜 상세에 중간 종료 세션을 실제 분과 "중간 종료" 표시로 보여 줌. "N회 집중 완료"는 완료 세션만
- 기록된 세션은 취소 불가

**바꾸지 않은 것**
- DB 스키마(상태 값 추가, `completed_day`를 기록 날짜로 재사용), 일시정지·새로고침 복원·서버 기준 시간 계산,
  자동 완료 규칙, 기존 `CANCELLED` 기록(계속 집계 제외 → 기존 데이터의 통계는 그대로)

**테스트:** `FocusStopAndSwitchTests` 8개 + `FocusSessionUnitTests` 5개 추가 → 68개(단위 25 + 통합 43)
