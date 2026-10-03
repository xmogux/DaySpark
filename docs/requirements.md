# DaySpark 요구사항과 수용 기준

이 문서는 **현재 분석·설계 보고서의 요구사항 체계**에 맞춘 저장소 기준 문서다.
보고서와 동일하게 기능 요구사항은 **R01~R16(16개)**, 비기능 요구사항은 **N01~N05(5개)**로 사용한다.

- **대상 사용자:** 개인 PC에서 공부·운동·생활·업무 목표를 관리하는 단일 사용자
- **범위:** 단일 사용자 로컬 웹앱. 회원가입·다중 사용자·외부 캘린더 연동·알림·AI 추천·자동 백업/복원 화면은 현재 범위에서 제외 → [limitations.md](limitations.md)
- **규칙의 상세 정의:** [domain-rules.md](domain-rules.md)
- 아래의 **테스트/확인** 항목은 현재 구현을 확인할 수 있는 대표 근거다. 전체 테스트 목록과 실행 방법은 [testing.md](testing.md)를 참고한다.

## 기능 요구사항 (R01~R16)

### R01 도전 생성·수정·삭제

이름·분류·기간을 설정한다. 삭제 시 관련 기록과 사진을 정리한다.

| # | 수용 기준 | 테스트/확인 |
|---|---|---|
| 1 | 이름(1~80자)과 시작일은 필수이며 종료일은 시작일 이후여야 한다. | `DaySparkApplicationTests.createAndEditChallenge`, `DareService.validate` |
| 2 | 분류는 지원되는 카테고리만 선택할 수 있다. | `RegressionTests.challengeFormOffersExactlyTheSupportedCategories` |
| 3 | 수정 화면은 저장된 값으로 채워진다. | `DaySparkApplicationTests.newAndEditFormsRenderWithTheSavedValues` |
| 4 | 입력 오류 시 오류를 표시하고 사용자가 입력한 값을 유지한다. | `DaySparkApplicationTests.invalidInputIsSentBackToTheFormInsteadOfBeingLost` |
| 5 | 도전 삭제 시 완료·진행량·집중·일정 예외·복습 기록과 연결된 사진 파일을 함께 정리한다. | `DareService.delete` 수동/코드 확인 |

### R02 반복 일정 설정

매일·평일·선택 요일 및 시작일·종료일을 설정한다.

| # | 수용 기준 | 테스트/확인 |
|---|---|---|
| 1 | 매일·평일·주말·선택 요일 반복을 설정할 수 있다. | `DareScheduleTest.repeatTypesPlanTheExpectedWeekdays` |
| 2 | 시작일 이전과 종료일 이후에는 계획되지 않는다. | `DareScheduleTest.nothingIsPlannedOutsideTheStartAndEndDates` |
| 3 | 잘못된 반복 요일 값은 저장 단계에서 거부한다. | `DaySparkApplicationTests.invalidRepeatDayIsRejectedInsteadOfCrashingLater` |

### R03 도전 쉬기와 재개

도전을 일시적으로 쉬게 하거나 다시 활성화한다.

| # | 수용 기준 | 테스트/확인 |
|---|---|---|
| 1 | 쉬기 시작일 이후의 계획은 비활성화되고 이전 날짜의 계획은 유지된다. | `DomainRuleTests.pausingRemovesTodayAndLaterButKeepsThePast`, `DareScheduleTest.pauseAffectsOnlyTheDayItStartsAndLater` |
| 2 | 쉬고 있는 도전을 다시 활성 상태로 돌릴 수 있다. | `DareService`의 pause/resume 처리 확인 |

### R04 날짜별 일정 변경

도전 기간 안의 오늘·미래 날짜에 추가·쉬기·예외 해제를 적용한다.

| # | 수용 기준 | 테스트/확인 |
|---|---|---|
| 1 | `SKIP`한 날짜는 계획에서 빠지고 완료할 수 없다. | `EnhancementApiTests.skipOverrideRemovesTodayAndBlocksCompletion` |
| 2 | 날짜별로 `ADD`, `SKIP`, `RESET`을 적용할 수 있다. | `PlanningService.setOverride` |
| 3 | 이미 완료한 날짜에는 허용되지 않는 일정 변경을 막는다. | `RegressionTests.resetIsRejectedOnACompletedDate` |
| 4 | 과거 날짜와 도전 기간 밖의 날짜 변경은 거부한다. | `PlanningService.setOverride` |

### R05 수치 목표와 주간 목표

하루 1~10000의 수치 목표와 단위를 설정한다. 주간 목표는 0~7회이며 0은 미설정이다.

| # | 수용 기준 | 테스트/확인 |
|---|---|---|
| 1 | 하루 목표, 단위, 주간 목표를 저장할 수 있다. | `EnhancementApiTests.goalsProgressAndWeekAreSaved` |
| 2 | 하루 목표는 1~10000 범위로 검증한다. | `DareService.validate`, `DareRequest` |
| 3 | 주간 목표 0은 미설정으로 취급하고 1~7 범위를 사용할 수 있다. | `DareService.validate`, `DareRequest` |

### R06 오늘 계획 조회

예정 도전, 완료, 진행량, 집중 시간, 오늘·지난 복습을 보여준다.

| # | 수용 기준 | 테스트/확인 |
|---|---|---|
| 1 | 오늘 계획된 도전과 완료/진행 상태를 조회할 수 있다. | `DaySparkApplicationTests.mainPagesLoad`, `StatsService`/`TodayBoard` |
| 2 | 오늘 복습과 기한이 지난 미완료 복습을 함께 보여준다. | `RegressionTests.overdueReviewsStayOnTheHomeScreenUntilDone` |
| 3 | 오늘의 집중 시간 정보가 화면 조회 결과에 반영된다. | `StatsService`, `TodayBoard` |

### R07 오늘 완료와 취소

오늘 예정된 도전을 중복 없이 완료하고 오늘 완료를 취소한다.

| # | 수용 기준 | 테스트/확인 |
|---|---|---|
| 1 | 오늘 예정된 도전만 완료할 수 있다. | `CompletionService.complete`, `PlanningService.isScheduled` |
| 2 | 같은 도전·날짜의 완료는 중복 생성하지 않는다. | DB 고유 제약 `completions(dare_id, challenge_date)`, `CompletionService` |
| 3 | 오늘 완료를 취소하면 완료 기록과 연결된 사진을 삭제하고 계획은 유지한다. | `RegressionTests.todaysCompletionCanBeUndoneAndItsPhotoIsDeleted` |

### R08 사진과 메모 기록

완료 시 사진과 메모를 선택 입력한다. 사진 확장자와 업로드 크기를 제한한다.

| # | 수용 기준 | 테스트/확인 |
|---|---|---|
| 1 | 완료 시 최대 300자의 메모를 저장할 수 있다. | `DaySparkApplicationTests.completingScheduledChallengeStoresNoteAndImage` |
| 2 | JPG·PNG·WEBP·GIF 사진을 선택 첨부할 수 있고 업로드 크기를 제한한다. | `ImageStorage`, `DaySparkApplicationTests.completingScheduledChallengeStoresNoteAndImage` |
| 3 | 허용되지 않는 이미지 형식이나 저장 실패를 성공으로 처리하지 않는다. | `DaySparkApplicationTests.invalidImageIsRejected`, 오류 처리 코드 |

### R09 진행량 기록과 자동 완료

오늘 예정된 도전에 양수 진행량을 누적하고 목표에 처음 도달할 때 자동 완료한다.

| # | 수용 기준 | 테스트/확인 |
|---|---|---|
| 1 | 오늘 예정된 도전의 진행량만 기록할 수 있다. | `ProgressService.add` |
| 2 | 양수 진행량을 누적하며 입력 범위를 검증한다. | `ProgressService.add` |
| 3 | 목표에 처음 도달하는 순간 자동 완료를 생성한다. | `FocusAutoCompleteTests.manualProgressReachingTheTargetCompletesTheChallenge` |
| 4 | 목표 미달 상태에서도 사용자가 수동 완료하는 것은 허용한다. | `DomainRuleTests.manualCompletionIsAllowedBelowTheNumericTarget` |

### R10 집중 타이머 사용

활성 도전에 1~120분을 설정하고 시작·일시정지·재개·완료한다.

| # | 수용 기준 | 테스트/확인 |
|---|---|---|
| 1 | 활성 도전을 선택해 1~120분 집중 세션을 시작할 수 있다. | `FocusService.start`, `FocusApi` |
| 2 | 실행 중 세션을 일시정지하고 재개할 수 있다. | `EnhancementApiTests.focusApiCanStartPauseResumeCancel`, `FocusSessionUnitTests.pauseResumeAndCompletionRequireFullElapsedTime` |
| 3 | 계획 시간이 모두 지난 경우에만 정상 완료(`COMPLETED`)가 된다. | `FocusSessionUnitTests.pauseResumeAndCompletionRequireFullElapsedTime` |
| 4 | 일시정지 시간은 실제 집중 시간에서 제외한다. | `FocusStopAndSwitchTests.pausedTimeIsExcludedFromFocusTime`, `FocusSessionUnitTests.stopKeepsTheTimeFocusedAndExcludesPauses` |

### R11 집중 중간 종료와 도전 전환

중간 종료 시간을 기록한다. 도전 전환 시 기존 세션을 종료한다.

| # | 수용 기준 | 테스트/확인 |
|---|---|---|
| 1 | 계획 시간 전에 종료하면 실제 집중한 시간만 `STOPPED` 세션으로 기록한다. | `FocusStopAndSwitchTests.stoppingEarlyKeepsTheMinutesActuallyFocused` |
| 2 | 중간 종료 세션은 완료 회차와 목표 진행량에 더하지 않는다. | `FocusStopAndSwitchTests.stoppedSessionIsNotCountedAsCompletedAndDoesNotAutoComplete` |
| 3 | 다른 도전으로 전환하면 기존 세션을 종료해 시간을 보존한 뒤 새 세션을 시작한다. | `FocusStopAndSwitchTests.switchingToAnotherChallengeRecordsTheOldSessionAndStartsANewOne` |
| 4 | 이미 계획 시간이 지난 세션을 종료하면 정상 완료로 처리한다. | `FocusStopAndSwitchTests.stoppingAfterTheTimeRanOutCompletesTheSession` |

### R12 집중 회차와 화면 복원

휴식·반복 회차를 관리하고 서버의 진행 중인 세션을 다시 불러온다.

| # | 수용 기준 | 테스트/확인 |
|---|---|---|
| 1 | 브라우저에서 집중/휴식 시간과 반복 회차를 설정하고 다음 회차를 진행할 수 있다. | `focus.js`, 집중 화면 수동 확인 |
| 2 | 새로고침 후 서버에 남아 있는 진행 중 세션의 상태와 남은 시간을 다시 불러온다. | `FocusApi`, `focus.js`, `EnhancementApiTests.focusApiCanStartPauseResumeCancel` |
| 3 | 완료 요청 실패 시 서버 상태를 다시 확인하며 과도한 재요청을 피한다. | `focus.js`, [testing.md](testing.md)의 브라우저 확인 기록 |

### R13 복습 계획과 완료 관리

공부 도전의 기준일·간격을 정하고 복습을 조회·완료·취소한다.

| # | 수용 기준 | 테스트/확인 |
|---|---|---|
| 1 | STUDY 도전에 기준일과 복습 간격을 직접 설정할 수 있다. | `EnhancementApiTests.studyReviewPlanCanBeConfiguredAndCompleted`, `ReviewIntervalsTest` |
| 2 | 입력한 간격은 정렬하고 중복을 제거한다. | `ReviewIntervalsTest` |
| 3 | 하나의 기준일에 간격을 더해 예정일을 만든다. 예: 10/2 + 1·3·7일 → 10/3·10/5·10/9. | `DomainRuleTests.reviewPlanCreatesOneScheduleForTheStudyUnit` |
| 4 | 복습 완료를 다시 누르면 완료 표시를 취소할 수 있다. | `ReviewService` |
| 5 | 지난 미완료 복습은 완료할 때까지 조회 대상에 남는다. | `RegressionTests.overdueReviewsStayOnTheHomeScreenUntilDone` |

### R14 달력과 날짜 상세 조회

월별 정보를 조회하고 날짜별 계획·실행 기록을 확인한다.

| # | 수용 기준 | 테스트/확인 |
|---|---|---|
| 1 | 월간 계획·완료·집중·복습 정보와 요약을 조회할 수 있다. | `DaySparkApplicationTests.redesignedCalendarExposesMonthSummaryAndDayCards` |
| 2 | 선택한 날짜의 계획·완료·복습·집중 기록을 조회할 수 있다. | `EnhancementApiTests.newPagesRender` |
| 3 | 조회 허용 범위를 벗어난 월은 사용자용 오류로 안내한다. | `DaySparkApplicationTests.invalidCalendarMonthShowsFriendlyErrorPage` |

### R15 통계와 기록 조회

주간·월간·분류별·도전별 지표, 연속 기록과 완료 이력을 본다.

| # | 수용 기준 | 테스트/확인 |
|---|---|---|
| 1 | 주간·월간 달성률과 분류별·도전별 결과를 조회한다. | `StatsService`, `StatsController` |
| 2 | 달성률은 0~100% 범위를 유지한다. | `RegressionTests.ratesStayAtOrBelow100PercentAfterRepeatRuleChange` |
| 3 | Streak는 계획일 기준으로 계산하며 진행 중인 오늘은 미완료라는 이유만으로 끊지 않는다. | `DomainRuleTests.streakCountsDaysWithAtLeastOneCompletion`, `DomainRuleTests.streakIgnoresUnfinishedTodayButBreaksOnAMissedDay`, `StreakCalculatorTest` |
| 4 | 완료 기록을 최신순으로 메모·사진과 함께 조회할 수 있다. | `DaySparkApplicationTests.mainPagesLoad`, `HomeController` |

### R16 로컬 실행과 오류 안내

로컬 서버로 실행하고 기록을 보존하며 입력 오류·실패를 안내한다.

| # | 수용 기준 | 테스트/확인 |
|---|---|---|
| 1 | 애플리케이션은 개인 PC의 로컬 서버에서 실행한다. | `application.properties`, `RUN_DAYSPARK.bat` |
| 2 | 업무 기록은 H2 파일 DB, 사진은 로컬 파일에 저장하여 재시작 후 사용할 수 있게 구성한다. | `application.properties`, `ImageStorage` |
| 3 | 잘못된 입력과 요청 실패를 사용자에게 안내하고, JSON API 오류는 JSON으로 반환한다. | `ErrorHandlingTests`, `GlobalExceptionHandler` |
| 4 | Windows 환경에서 제공 실행 스크립트로 앱을 시작할 수 있다. | `RUN_DAYSPARK.bat` 수동 확인 |

## 비기능 요구사항 (N01~N05)

| ID | 요구사항 | 현재 반영 방식 / 확인 |
|---|---|---|
| N01 | **사용 편의성** | 주요 화면에 공통 메뉴와 한글 안내를 제공한다. 도전 입력 검증 실패 시 입력값을 유지해 다시 수정할 수 있게 한다. (`layout.html`, `DaySparkApplicationTests.invalidInputIsSentBackToTheFormInsteadOfBeingLost`) |
| N02 | **기록의 일관성** | 동일 날짜 완료 중복을 방지하고 집중 시간·완료 횟수·목표 진행량을 서로 구분한다. (DB 고유 제약, `CompletionService`, `FocusService`, `ProgressService`) |
| N03 | **데이터 보존** | 업무 기록은 H2 파일 DB, 사진은 로컬 파일로 저장하여 재시작 후 사용할 수 있게 구성한다. (`application.properties`, `ImageStorage`) |
| N04 | **유지보수성** | 요청 처리(Controller/API), 업무 규칙(Service/Domain), 데이터 접근(Repository)을 분리하고 주요 규칙을 기존 테스트로 검사한다. ([architecture.md](architecture.md), [testing.md](testing.md)) |
| N05 | **로컬 접근 범위** | 기본 서버 주소는 `127.0.0.1`이며 사용자 인증이 있는 공개 서비스로 간주하지 않는다. (`application.properties`) |

## 요구사항의 확인 범위

이 요구사항은 현재 구현을 정리한 것이다. 별도의 인터뷰·설문 결과, 개발 비용 산정이나 성능 목표를 사후에 만들어 넣지 않는다. 기능이 코드에 존재한다는 사실과 실행 검증 결과는 구분해서 기록한다.

고정 Clock, JDK 버전별 빌드, 브라우저 재요청 간격처럼 이전 문서에서 별도 NFR로 분류했던 항목은 삭제한 것이 아니라 **테스트 환경·구현 세부 조건**으로 [testing.md](testing.md)와 관련 코드에 남긴다. 보고서와 저장소의 공식 요구사항 수를 맞추기 위해 요구사항 ID 체계에서는 제외한다.
