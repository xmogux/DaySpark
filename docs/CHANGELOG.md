# DaySpark Changelog

## v5 — bug-fix and presentation-template release

- Fixed focus session completion date calculation.
- Removed write side effect from GET `/api/focus/current`.
- Added enum-based input validation helpers for repeat type, category, schedule mode, and focus status.
- Added `pausedFrom` so pausing a challenge no longer changes historical calendar/stat denominators.
- Updated overall Streak to count scheduled days only.
- Added global error handling and an error page.
- Removed obsolete Thymeleaf Java 8 time dependency.
- Cleaned top-level README and moved older version-specific notes to `docs/archive/`.
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
