# DaySpark

반복 목표(도전)를 오늘 할 일로 바꿔 주고, 완료·집중 시간·복습을 한곳에 기록하는 **단일 사용자용 Spring Boot 웹앱**입니다.
소프트웨어공학 수업 팀 프로젝트입니다.

| 화면 | 하는 일 |
|---|---|
| 오늘 | 오늘 계획된 도전, 진행률, Streak, 완료(사진·메모) / **완료 취소**, 오늘·지난 복습 |
| 도전 | 생성·수정·쉬기·삭제, 반복 일정(매일·평일·주말·요일), 수치 목표, 일정 예외(쉬기·추가), 복습 플랜 |
| 집중 타이머 | 포모도로(집중·휴식·반복). 중간에 끝내도 **실제 집중한 시간은 기록**, 끝까지 마친 회차는 `분` 목표 진행량에 더해지고 목표 첫 도달 시 **자동 완료**. 진행 중 도전 전환 |
| 달력 / 통계 / 기록 | 월간 달성률·집중 시간, 주간·월간 통계, 카테고리별 완료, 완료 기록 |

## 문서

| 문서 | 내용 |
|---|---|
| [docs/requirements.md](docs/requirements.md) | 기능·비기능 요구사항, 수용 기준, 대응 테스트 |
| [docs/domain-rules.md](docs/domain-rules.md) | 하루의 계획, 일정 예외, 완료/취소, Streak, 복습 플랜, 포모도로 연결 규칙 |
| [docs/architecture.md](docs/architecture.md) | 계층 구조, 클래스 다이어그램, ERD, 시퀀스·상태 다이어그램, 설계 결정 |
| [docs/testing.md](docs/testing.md) | 테스트 68개 구성, 버그별 회귀 테스트, 실행 결과 기록 |
| [docs/process.md](docs/process.md) | Git 이력, GitHub 올리기, Issue → PR → CI 작업 방식 |
| [docs/limitations.md](docs/limitations.md) | 하지 않은 것과 이유(N+1, 단일 사용자, Docker 미사용 등) |
| [docs/CHANGELOG.md](docs/CHANGELOG.md) | 버전별 변경 |

## 개발 환경

| 항목 | 기준 |
|---|---|
| Java | **17** (`pom.xml`의 `java.version`; JDK 17 이상에서 실행 가능, GitHub Actions는 JDK 17·25에서 `verify`하도록 설정) |
| 빌드 | Maven Wrapper (`mvnw`, `mvnw.cmd`) — Maven을 따로 설치하지 않아도 됨 |
| 프레임워크 | Spring Boot 3.5 (Web, Thymeleaf, Data JPA, Validation) |
| DB | H2 파일 DB (`./dayspark_v2.mv.db`) |
| 테스트 | JUnit 5, Spring Boot Test(MockMvc), JaCoCo 커버리지 |
| CI | GitHub Actions (`.github/workflows/ci.yml`) — push/PR마다 `./mvnw verify` |
| 코드 스타일 | `.editorconfig`, `.prettierrc.json` (Prettier + prettier-plugin-java) |

## 실행

**Windows:** `RUN_DAYSPARK.bat` 더블클릭.
서버가 그 창에서 실행되고, 준비되면 기본 브라우저로 `http://127.0.0.1:8080/`이 열립니다.
종료는 창에서 `Ctrl+C` 또는 창 닫기. 8080 포트가 이미 사용 중이면 실행하지 않고 안내합니다.

**WSL / Linux / macOS:**

```bash
./mvnw spring-boot:run
# 브라우저에서 http://localhost:8080/
```

서버는 `127.0.0.1`에만 바인딩됩니다(단일 사용자 로컬 앱). 데이터는 실행 폴더의
`dayspark_v2.mv.db`, 업로드 사진은 `uploads/`에 저장됩니다. 이전 버전의 기록을 이어 쓰려면
서버를 끈 상태에서 이전 폴더의 `dayspark_v2.mv.db`와 `uploads/`를 복사해 오세요.

## Maven Wrapper

저장소에 Maven Wrapper(3.3.4)가 들어 있어 **Maven을 설치할 필요가 없습니다.**
`./mvnw`(WSL·Linux) 또는 `mvnw.cmd`(Windows)를 처음 실행하면 Maven 3.9.11을 `~/.m2/wrapper/`에 내려받아 씁니다.
처음 한 번은 Maven과 프로젝트 의존성 다운로드로 몇 분 걸릴 수 있습니다(인터넷 필요).

Maven 버전을 바꾸려면 `.mvn/wrapper/maven-wrapper.properties`의 `distributionUrl`을 수정합니다.

## 테스트

```bash
./mvnw test      # 단위 + 통합 테스트
./mvnw verify    # 테스트 + JaCoCo 커버리지 리포트 (target/site/jacoco/index.html)
```

Windows CMD/PowerShell에서는 `mvnw.cmd test`, `mvnw.cmd verify`.

2026-10-04 Windows 10 / JDK 25.0.4.1에서 `mvnw.cmd clean test`를 실행해 **68개 전체 통과(Failures 0, Errors 0, Skipped 0)**를 확인했습니다.
GitHub Actions는 JDK 17·25에서 `verify`하도록 구성되어 있으며, 실제 CI 실행 결과는 Actions 기록으로 구분합니다.
결과를 기록하는 곳과 테스트 목록은 [docs/testing.md](docs/testing.md).

## 소스 구조

```text
src/main/java/com/dayspark/app
├── controller   HomeController, DareController, CalendarController, StatsController,
│                FocusController, FocusApi(JSON), GlobalExceptionHandler
├── service      DareService, PlanningService, CompletionService, ProgressService,
│                FocusService, ReviewService, StatsService, ImageStorage
│   ├── dto      DareRequest (도전 폼)
│   └── view     StatsService가 만드는 화면용 record (DayPlan, CalendarMonth, …)
├── domain       StreakCalculator, ReviewIntervals, enum(DareCategory, RepeatType, …)
├── model        JPA 엔티티 (Dare, Completion, ProgressLog, ScheduleException, FocusSession, ReviewRecord)
├── repository   Spring Data JPA
└── config       Clock, 업로드 경로, 예시 데이터
src/main/resources/templates   Thymeleaf (공통 내비게이션: fragments/layout.html)
src/test/java/com/dayspark/app 단위 테스트(domain, model) + 통합 테스트, support/(고정 시계)
```
