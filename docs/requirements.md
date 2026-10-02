# DaySpark 요구사항과 수용 기준

- **대상 사용자:** 반복 목표(공부·운동·생활 습관)를 작게 쪼개 매일 실행하려는 대학생 1인
- **범위:** 단일 사용자 로컬 웹앱. 로그인·알림·SNS 공유·AI 추천은 제외 → [limitations.md](limitations.md)
- **규칙의 상세 정의:** [domain-rules.md](domain-rules.md)
- 각 요구사항의 "테스트" 칸은 그 수용 기준을 자동으로 확인하는 테스트입니다. 테스트 목록과 실행 방법은 [testing.md](testing.md).

## 기능 요구사항 (FR)

### FR-01 도전 관리 (생성·조회·수정·삭제)

| # | 수용 기준 | 테스트 |
|---|---|---|
| 1 | 이름(1~80자)과 시작일은 필수, 종료일은 시작일 이후 | `DaySparkApplicationTests.createAndEditChallenge`(정상 생성), 검증 규칙은 `DareService.validate` |
| 2 | 카테고리는 STUDY·HEALTH·MOVE·LIFE·WORK 중 하나이고, 폼은 이 값만 보여준다 | `RegressionTests.challengeFormOffersExactlyTheSupportedCategories` |
| 3 | 수정 화면은 저장된 값으로 채워진다 | `DaySparkApplicationTests.newAndEditFormsRenderWithTheSavedValues` |
| 4 | 입력 오류 시 오류 메시지를 보여주고 **입력값을 잃지 않는다** | `DaySparkApplicationTests.invalidInputIsSentBackToTheFormInsteadOfBeingLost` |
| 5 | 삭제하면 완료·진행량·집중·예외·복습 기록과 사진 파일을 함께 지운다 | (수동 확인, `DareService.delete`) |

### FR-02 반복 일정

| # | 수용 기준 | 테스트 |
|---|---|---|
| 1 | 매일 / 평일 / 주말 / 요일 선택(1개 이상) | `DareScheduleTest.repeatTypesPlanTheExpectedWeekdays` |
| 2 | 시작일 이전·종료일 이후에는 계획되지 않는다 | `DareScheduleTest.nothingIsPlannedOutsideTheStartAndEndDates` |
| 3 | 잘못된 요일 값은 저장 단계에서 거부한다 | `DaySparkApplicationTests.invalidRepeatDayIsRejectedInsteadOfCrashingLater` |

### FR-03 일정 예외 (쉬기 SKIP / 추가 ADD / 해제 RESET)

| # | 수용 기준 | 테스트 |
|---|---|---|
| 1 | SKIP한 날은 오늘 목록에서 빠지고 완료할 수 없다 | `EnhancementApiTests.skipOverrideRemovesTodayAndBlocksCompletion` |
| 2 | 이미 완료한 날짜는 SKIP·RESET 할 수 없다 | `RegressionTests.resetIsRejectedOnACompletedDate` |
| 3 | 과거 날짜와 도전 기간 밖은 변경할 수 없다 | (`PlanningService.setOverride`) |

### FR-04 쉬기·재개

| # | 수용 기준 | 테스트 |
|---|---|---|
| 1 | 쉬기 시작한 날부터 계획에서 빠지고, 그 전 날짜의 계획은 그대로 남는다 | `DomainRuleTests.pausingRemovesTodayAndLaterButKeepsThePast`, `DareScheduleTest.pauseAffectsOnlyTheDayItStartsAndLater` |

### FR-05 오늘 화면과 완료 기록

| # | 수용 기준 | 테스트 |
|---|---|---|
| 1 | 오늘 계획된 도전과 진행률(완료/계획)을 보여준다 | `DaySparkApplicationTests.mainPagesLoad` |
| 2 | 완료 시 메모(300자)와 사진(JPG·PNG·WEBP·GIF, 8MB)을 남길 수 있다 | `DaySparkApplicationTests.completingScheduledChallengeStoresNoteAndImage` |
| 3 | 허용되지 않은 파일은 거부한다 | `DaySparkApplicationTests.invalidImageIsRejected` |
| 4 | 같은 도전·날짜의 완료는 한 번만 기록된다 | DB 고유 제약 `completions(dare_id, challenge_date)` |

### FR-06 완료 취소

| # | 수용 기준 | 테스트 |
|---|---|---|
| 1 | 오늘의 완료를 취소하면 기록과 사진이 삭제되고 "미완료"로 돌아간다 | `RegressionTests.todaysCompletionCanBeUndoneAndItsPhotoIsDeleted` |

### FR-07 수치 목표와 진행량

| # | 수용 기준 | 테스트 |
|---|---|---|
| 1 | 하루 목표(1~10000)·단위·주간 목표(0~7)를 저장한다 | `EnhancementApiTests.goalsProgressAndWeekAreSaved` |
| 2 | 계획된 날에만 진행량을 기록할 수 있다 | (`ProgressService.add`) |
| 3 | 목표 미달이어도 수동 완료를 허용한다 | `DomainRuleTests.manualCompletionIsAllowedBelowTheNumericTarget` |
| 4 | 진행량이 목표에 **처음 도달하는 순간** 자동 완료한다 | `FocusAutoCompleteTests.manualProgressReachingTheTargetCompletesTheChallenge` |

### FR-08 포모도로 집중 타이머

| # | 수용 기준 | 테스트 |
|---|---|---|
| 1 | 시작·일시정지·재개·종료, 서버가 남은 시간을 계산(새로고침해도 유지) | `EnhancementApiTests.focusApiCanStartPauseResumeCancel` |
| 2 | 계획 시간이 다 지나야 완료되고, 중간에 끝낸 세션은 완료가 되지 않는다 | `FocusSessionUnitTests.pauseResumeAndCompletionRequireFullElapsedTime`, `aStoppedSessionIsFinal` |
| 3 | 자정을 넘긴 세션은 시간이 끝난 날짜로 기록된다 | `FocusSessionUnitTests.completedDayUsesActualExpectedFinishTimeAcrossMidnight` |
| 4 | **종료 및 기록**: 25분 타이머를 10분 만에 끝내면 오늘·주·월 집중 시간에 10분이 남는다 | `FocusStopAndSwitchTests.stoppingEarlyKeepsTheMinutesActuallyFocused` |
| 5 | 중간에 끝낸 세션은 완료 횟수에 들어가지 않고 진행량·자동 완료도 없다 | `FocusStopAndSwitchTests.stoppedSessionIsNotCountedAsCompletedAndDoesNotAutoComplete` |
| 6 | 끝까지 완료한 세션은 이전과 같이 시간 + 1회로 기록된다 | `FocusStopAndSwitchTests.completedSessionCountsMinutesAndOneRound` |
| 7 | 일시정지한 시간은 집중 시간에 포함되지 않는다(일시정지 중 종료 포함) | `FocusStopAndSwitchTests.pausedTimeIsExcludedFromFocusTime`, `stoppingWhilePausedRecordsOnlyTheTimeBeforeThePause`, `FocusSessionUnitTests.stopKeepsTheTimeFocusedAndExcludesPauses` |
| 8 | 세션 중 다른 도전을 고르면 확인 후 이전 세션 시간을 기록하고 새 세션을 시작한다 | `FocusStopAndSwitchTests.switchingToAnotherChallengeRecordsTheOldSessionAndStartsANewOne`, `switchingWithoutAnActiveSessionJustStarts` |
| 9 | 계획 시간이 이미 지난 세션을 종료하면 정상 완료로 처리한다 | `FocusStopAndSwitchTests.stoppingAfterTheTimeRanOutCompletesTheSession` |

### FR-09 포모도로 ↔ 목표 연결

| # | 수용 기준 | 테스트 |
|---|---|---|
| 1 | `분` 목표면 **끝까지 완료한** 집중 시간이 진행량에 더해지고, 목표 도달 시 자동 완료 | `FocusAutoCompleteTests.focusMinutesReachingTheTargetCompleteTheChallenge` |
| 2 | 자동 완료는 목표에 처음 도달할 때 한 번만 | `FocusAutoCompleteTests.autoCompletionHappensOnlyWhenTheTargetIsFirstReached` |
| 3 | 계획 없는 날의 집중은 정상 종료, 진행량·완료 반영 없음 | `FocusAutoCompleteTests.focusOnAnUnplannedDayFinishesNormallyWithoutProgressOrCompletion` |
| 4 | 자정 경계에서도 세션이 RUNNING으로 남지 않는다 | `FocusAutoCompleteTests.sessionCrossingMidnightFinishesOnTheNextDayAndIsNotLeftRunning` |

### FR-10 달력

| # | 수용 기준 | 테스트 |
|---|---|---|
| 1 | 월간 완료 수·달성률·집중 시간 요약과 날짜별 상태 | `DaySparkApplicationTests.redesignedCalendarExposesMonthSummaryAndDayCards` |
| 2 | 날짜 상세: 계획·완료·복습·집중 기록 | `EnhancementApiTests.newPagesRender` |
| 3 | 조회 범위(4년 전~1년 후) 밖은 친절한 오류 | `DaySparkApplicationTests.invalidCalendarMonthShowsFriendlyErrorPage` |

### FR-11 통계와 Streak

| # | 수용 기준 | 테스트 |
|---|---|---|
| 1 | 주간·월간 달성률은 0~100%이고, 반복 규칙을 바꿔도 100%를 넘지 않는다 | `RegressionTests.ratesStayAtOrBelow100PercentAfterRepeatRuleChange` |
| 2 | Streak: 계획일 기준, 하루에 1개 이상 완료하면 이어진다 | `DomainRuleTests.streakCountsDaysWithAtLeastOneCompletion`, `StreakCalculatorTest` |
| 3 | 진행 중인 오늘은 끊김이 아니고, 놓친 계획일에서 끊긴다 | `DomainRuleTests.streakIgnoresUnfinishedTodayButBreaksOnAMissedDay` |

### FR-12 공부 복습 플랜

| # | 수용 기준 | 테스트 |
|---|---|---|
| 1 | STUDY 도전에 기준일·간격을 직접 설정, 입력은 정렬·중복 제거 | `EnhancementApiTests.studyReviewPlanCanBeConfiguredAndCompleted`, `ReviewIntervalsTest` |
| 2 | 하나의 학습 단위 기준으로 일정 생성(기준일 10/2 + 1,3,7 → 3개) | `DomainRuleTests.reviewPlanCreatesOneScheduleForTheStudyUnit` |
| 3 | 지난 복습은 완료할 때까지 오늘 화면에 남는다 | `RegressionTests.overdueReviewsStayOnTheHomeScreenUntilDone` |

### FR-13 완료 기록(히스토리)

| # | 수용 기준 | 테스트 |
|---|---|---|
| 1 | 완료 기록을 최신순으로 사진·메모와 함께 보여준다 | `DaySparkApplicationTests.mainPagesLoad` |

## 비기능 요구사항 (NFR)

| ID | 요구사항 | 확인 방법 |
|---|---|---|
| NFR-01 | 오류는 사용자용 메시지와 올바른 HTTP 상태(400/404/405/500)로 응답하고 서버 로그에 남긴다. JSON API는 항상 JSON으로 응답 | `ErrorHandlingTests` (4개) |
| NFR-02 | 테스트는 실행 날짜와 무관하게 같은 결과를 낸다 (고정 Clock 2026-10-07) | `TestClockConfig`, `MutableClock` |
| NFR-03 | 단일 사용자 로컬 앱: 서버는 127.0.0.1에만 바인딩 | `application.properties` |
| NFR-04 | Windows 10/11에서 `RUN_DAYSPARK.bat` 한 번으로 실행, 기본 브라우저 사용 | 수동 확인 |
| NFR-05 | JDK 17(기준)과 25에서 빌드·테스트 통과 | GitHub Actions 매트릭스 |
| NFR-06 | 브라우저 타이머는 서버 오류 시 요청을 반복 폭주시키지 않는다 (최대 5초에 1회) | 브라우저 확인 기록 → [testing.md](testing.md) |
