# DaySpark 테스트

## 1. 실행 방법

```bash
./mvnw test      # 단위 + 통합 테스트
./mvnw verify    # 위 + JaCoCo 커버리지 리포트
```

| 결과물 | 위치 |
|---|---|
| 테스트별 결과 | `target/surefire-reports/` (`*.txt`, `TEST-*.xml`) |
| 커버리지 리포트 | `target/site/jacoco/index.html` (패키지 → 클래스 → 소스 줄 단위) |
| CI 실행 기록 | GitHub 저장소 → **Actions** 탭 (JDK 17 job 요약에 패키지별 커버리지 표) |

## 2. 테스트 구성 — 68개

| 종류 | 클래스 | 개수 | 확인하는 것 |
|---|---|---:|---|
| 단위 | `StreakCalculatorTest` | 6 | Streak 규칙 (건너뛰기, 진행 중인 오늘, 끊김, 범위) |
| 단위 | `ReviewIntervalsTest` | 6 | 복습 간격 입력 정규화·검증 메시지 |
| 단위 | `DareScheduleTest` | 5 | 반복 규칙, 기간, 쉬기 경계, 요일 라벨, 복습 저장 |
| 단위 | `FocusSessionUnitTests` | 8 | 타이머 일시정지·재개·완료 조건, 자정 넘김, 종료 및 기록(일시정지 제외·계획 시간 상한·종료 후 변경 불가) |
| 통합 | `DaySparkApplicationTests` | 10 | 주요 페이지, 도전 생성·수정 폼, 완료+사진, 입력 오류 |
| 통합 | `EnhancementApiTests` | 5 | 수치 목표, 일정 예외, 집중 API, 복습 플랜 |
| 통합 | `RegressionTests` | 6 | 리뷰·리팩터링 중 찾은 버그 6건 (아래 3절) |
| 통합 | `DomainRuleTests` | 5 | 복습·Streak·수치 목표·쉬기 규칙 (`domain-rules.md`) |
| 통합 | `FocusAutoCompleteTests` | 5 | 포모도로 → 진행량 → 자동 완료, 계획 없는 날, 자정 |
| 통합 | `FocusStopAndSwitchTests` | 8 | 중간 종료 시간 기록, 완료 횟수·자동 완료 제외, 일시정지 제외, 도전 전환 (`domain-rules.md` 7.1) |
| 통합 | `ErrorHandlingTests` | 4 | 404 / 405 / 400 메시지 / JSON API 오류 |

- **단위 테스트**는 Spring 없이 순수 Java 객체만 검사합니다(빠름).
- **통합 테스트**는 Spring Boot 전체 + MockMvc + H2 메모리 DB로 HTTP 요청부터 DB까지 검사합니다.
- 요구사항 ↔ 테스트 매핑: [requirements.md](requirements.md)

### 테스트 인프라

| 구성 | 역할 |
|---|---|
| `support/TestDates` | 모든 테스트의 "오늘" = **2026-10-07(수) 12:00 Asia/Seoul** |
| `support/MutableClock` + `TestClockConfig` | 운영 `Clock`을 대체하는 `@Primary` 시계. 집중 테스트는 `clock.advance(25분)`으로 시간을 넘김(대기 없음) |
| `support/@DaySparkIntegrationTest` | `@SpringBootTest` + `@AutoConfigureMockMvc` + 테스트 시계를 한 번에 |
| `support/IntegrationTestSupport` | 매 테스트 전 DB 비우기 + 시계 초기화 |
| `src/test/resources/application.properties` | H2 메모리 DB, 예시 데이터 비활성화, 업로드는 `target/test-uploads` |

실행하는 날짜·요일·시각에 따라 결과가 달라지던 문제(`LocalDate.now()` 사용)를 없앴습니다.

## 3. 버그 → 회귀 테스트 (테스트 먼저 커밋)

각 버그는 **실패하는 테스트 커밋 → 수정 커밋** 순서로 남겼습니다. `git log --graph`에서 브랜치·병합으로 보입니다.

| 버그 | 테스트 커밋 | 수정 커밋 | 테스트 |
|---|---|---|---|
| 반복 요일을 바꾸면 주간 달성률 200% | `6852c9f` | `6198162` | `ratesStayAtOrBelow100PercentAfterRepeatRuleChange` |
| ADD → 완료 → RESET 하면 완료가 계획 없는 날에 남음 | `8a54827` | `c462807` | `resetIsRejectedOnACompletedDate` |
| 지난 복습이 오늘 화면에서 사라짐 | `d29a8da` | `37d3c1c` | `overdueReviewsStayOnTheHomeScreenUntilDone` |
| 완료를 취소할 수 없음 | `72628a0` | `534bded` | `todaysCompletionCanBeUndoneAndItsPhotoIsDeleted` |
| 폼이 서버가 거부하는 카테고리(MIND)를 보여줌 | `51754b8` | `fbad7eb` | `challengeFormOffersExactlyTheSupportedCategories` |
| 포모도로·진행량·완료가 따로 놂 | `1aaa383` | `f939437` | `FocusAutoCompleteTests` (5개) |
| 404·405가 500으로, API가 HTML로 응답 | `3f761f1` | `d38800f` | `ErrorHandlingTests` (4개) |
| (리팩터링이 만든 회귀) v5 이전 집중 기록이 있으면 도전 상세 500 | `6d6ce8d` | `2860dbc` | `detailPageToleratesFocusSessionsFromBeforeV5` |
| (v5.7) 타이머를 중간에 멈추면 집중한 시간이 사라짐, 도전을 바꿀 수 없음 | `fc92169` | `21bc4d4`, `948cee6`(화면) | `FocusStopAndSwitchTests` (8개) |

기존 테스트 자체의 문제도 고쳤습니다.

| 문제 | 커밋 |
|---|---|
| 달력 테스트가 화면에 없는 문자열 `달력 UI v5.4`를 기대(항상 실패) | `98187cc` |
| SKIP 테스트의 `not(hasItem(d))`가 엔티티 인스턴스 비교라 **항상 통과** | `b19812b` |

## 4. 실행 결과

### 4.1 로컬 / CI 결과

| 날짜 | 환경 | 명령 | Tests run | Failures | Errors | Skipped | 서비스 계층 Line 커버리지 |
|---|---|---|---:|---:|---:|---:|---:|
| 2026-10-04 | Windows 10, JDK 25.0.4.1 | `mvnw.cmd clean test` | **68** | **0** | **0** | **0** | `test` 실행이라 미측정 |
| v5.6 시점 (과거 사용자 실행) | Windows, JDK 25.0.4.1 | `mvnw.cmd test` | 55 | 0 | 0 | - | (verify 미실행) |
| (CI 실행 후 기입) | GitHub Actions JDK 17 | `./mvnw -B verify` | | | | | |
| (CI 실행 후 기입) | GitHub Actions JDK 25 | `./mvnw -B verify` | | | | | |

2026-10-04 로컬 실행에서는 Maven Surefire 최종 요약이 `Tests run: 68, Failures: 0, Errors: 0, Skipped: 0`이고 `BUILD SUCCESS`임을 확인했습니다.
커버리지는 `test`가 아니라 `verify`를 실행한 뒤 `target/site/jacoco/index.html`에서 확인합니다.
**CI와 커버리지는 실제 실행한 결과만 기록합니다.**

### 4.2 문서 정리 환경에서 한 별도 검증 (과거 기록)

정리 작업을 한 환경은 Maven Central 접속이 차단되어 Spring Boot 의존성을 받을 수 없었습니다.
그래서 그 환경에서는 Maven 빌드를 돌리지 못했고 아래 검증을 대신 했습니다. 실제 Maven 실행 결과는 4.1 표에만 적습니다
(v5.6은 Windows PC에서 `mvnw.cmd test` 55개 통과).

**v5.6 정리 작업**

| 검증 | 방법 | 결과 |
|---|---|---|
| 컴파일 | main 47개 + test 15개 파일을 Spring/JPA API 스텁과 함께 `javac --release 17` | 성공 |
| Maven Wrapper | `./mvnw -v`가 Maven 3.9.11 배포본 경로를 찾아 실행되는지 (다운로드 단계는 차단 환경이라 미확인) | 성공 |
| 단위 테스트 | 위 4개 단위 테스트 클래스를 실제 JUnit 5.10 + AssertJ로 실행 | **20 / 20 통과** |
| 버그 재현·수정 확인 | 실제 서비스·컨트롤러 코드를 메모리 저장소로 연결해 시나리오 실행 | 수정 전 실패 → 수정 후 통과 (기대값 34개 전부 통과) |
| 리팩터링 동일성 | 같은 데이터(도전 9개, 약 70일)를 리팩터링 전·후 코드에 넣고 30개 화면의 모델 값 비교 | 동일 (사용하지 않던 속성 2개 제거만 차이) |
| 브라우저 동작 | headless Chrome에서 `focus.js`, `app.js`를 가짜 서버 응답으로 실행 | finish 실패 시 6초간 요청 **1,067회 → 2회**, 정상 타이머 흐름·복습 미리보기 수정 전후 동일 |
| 템플릿 | 모든 템플릿 태그 균형, `th:replace` 대상 존재, 포맷 전후 `th:*` 속성 동일 | 이상 없음 |
| 설계 문서 | `architecture.md`의 Mermaid 다이어그램 6개 렌더링 | 전부 렌더링 |

**v5.7 집중 타이머 변경** (`feat/focus-stop-and-switch`)

| 검증 | 방법 | 결과 |
|---|---|---|
| 컴파일 | main 47개 + test 16개 파일, 스텁과 함께 `javac --release 17` | 성공 |
| 단위 테스트 | 단위 테스트 4개 클래스를 실제 JUnit 5.10 + AssertJ로 실행 | **25 / 25 통과** |
| 통합 테스트와 같은 시나리오 | `FocusStopAndSwitchTests` 8개를 실제 `FocusApi`·컨트롤러·서비스 + 메모리 저장소로 재현 (모델 값의 타입까지 비교) | 기대값 43개 전부 통과 |
| 기존 동작 유지 | v5.6 시나리오(버그 6건·규칙·포모도로 자동 완료) 재실행 | 전부 통과 (12 + 7 + 14 + 1) |
| 기존 데이터 동일성 | 완료·취소된 집중 기록이 섞인 같은 데이터를 v5.6(`ba36d9e`)과 v5.7 코드에 넣고 30개 화면 모델 비교 | 동일 (날짜 상세에 `focusRounds` 속성 추가만 차이, 값은 기존 표시와 같음) |
| 브라우저 동작 | headless Chrome에서 `focus.js`를 상태를 가진 가짜 서버로 실행: 종료 및 기록(진행 중·일시정지 중·1분 미만·시간 초과), 확인창 취소, 도전 전환(확인·취소·일시정지 중), 전환 후 새로고침 복원, 휴식 중 도전 변경·반복 종료, `?dareId`가 다른 경우 안내 | 12개 시나리오 기대대로, 페이지 오류 0 |
| 기존 브라우저 검사 | 정상 1회 흐름, finish 실패 시 재요청 제한 | 이전과 동일 (start 1·finish 1 / 6초간 finish 2회) |
| 설계 문서 | Mermaid 다이어그램 7개 렌더링 | 전부 렌더링 |

위 항목은 **당시 문서 정리 환경**에서 확인하지 못했던 범위다. 이후 2026-10-04 Windows에서 `mvnw.cmd clean test`를 실행하여
단위 25개와 통합 43개, 총 68개 테스트가 모두 통과했음을 확인했다. 다만 JaCoCo 커버리지와 GitHub Actions JDK 17·25 결과는
`verify` 또는 실제 CI를 실행한 뒤 별도로 기록한다.

## 5. 수동 확인 (자동화하지 않은 것)

| 항목 | 방법 |
|---|---|
| Windows 실행기 | Windows 10/11에서 `RUN_DAYSPARK.bat` 실행 → 기본 브라우저가 열리는지, 8080 사용 중이면 안내하는지 |
| 화면 배치 | PC·모바일 폭에서 오늘·달력·통계·도전 상세 화면 |
| 사진 8MB 초과 | 8MB 넘는 사진 업로드 시 오류 화면 문구 |
| 도전 삭제 | 사진이 있는 도전을 삭제한 뒤 `uploads/`에서 파일이 지워졌는지 |
