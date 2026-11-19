# DaySpark v5.7.0

반복 목표를 오늘의 할 일로 정리하고 **완료·진행량·집중 시간·복습·달력·통계**를 하나의 화면 흐름으로 관리하는 단일 사용자용 로컬 웹 애플리케이션입니다. 소프트웨어공학·객체지향 설계 프로젝트입니다.

> **테스트 실행:** Windows에서 `RUN_ALL_TESTS.bat`를 더블클릭하면 **단위 28개 + 통합 6개 + Chrome 시스템 14개 = 총 48개**가 순서대로 실행되고, 결과는 `test-results/summary.html`에 저장됩니다.

## 주요 기능

| 화면 / 기능 | 주요 동작 |
| --- | --- |
| 오늘 | 반복 규칙에서 계산한 오늘의 도전 확인, 수동 완료·취소, 수치 진행률, 사진·메모, 오늘/지난 복습 |
| 도전 | 생성·수정·삭제, 쉬기·재개, 매일·평일·주말·선택 요일, 시작·종료일, 날짜별 예외(추가·쉬기·해제), 수치·주간 목표 |
| 집중 | 서버에 저장되는 집중 세션, 중복 시작 방지, 일시정지·재개·중간 종료·도전 전환·새로고침 복원, 브라우저의 휴식·반복 회차 |
| 자동 완료 | 정상 완료된 집중 시간을 `분` 단위 목표에 적립하고, 해당 날짜 목표에 처음 도달하면 완료 기록 생성. 중간 종료는 집중 시간만 기록 |
| 복습 | 공부 도전의 기준일·복습 간격 설정, 예정일 표시, 완료·취소 |
| 달력·통계·기록 | 월간 달력과 날짜별 내역, 주간·월간 달성/집중 통계, 분류별 결과와 완료 기록 |

## 개발 환경 및 구조

| 항목 | 현재 사용 |
| --- | --- |
| 프로젝트 | DaySpark **5.7.0** |
| Java | **17 이상** (`pom.xml` 기준 17) |
| 서버 | Spring Boot **3.5.16** / Spring MVC / Thymeleaf |
| 데이터 | Spring Data JPA + H2 **파일 DB** |
| 웹 화면 | HTML·CSS·JavaScript, 서버 렌더링 Thymeleaf |
| 빌드 및 테스트 | Maven Wrapper / JUnit 5 / Mockito / MockMvc / JaCoCo / Playwright |
| 자동 검증 | `RUN_ALL_TESTS.bat` → JUnit 34개 + Windows Chrome 14개 |
| 사용 환경 | 개인 PC, 기본 `127.0.0.1:8080`, 로그인/다중 사용자 미지원 |

요청은 `Controller/FocusApi → Service → Repository → H2` 순서로 처리합니다. 집중·완료·복습 데이터는 H2에 저장하고, 업로드한 이미지는 `uploads/`에 저장하며 DB에는 그 경로를 보관합니다. 브라우저 `localStorage`는 집중 UI 설정을 보조적으로 저장합니다.

## 실행

**Windows:** `RUN_DAYSPARK.bat`를 더블클릭합니다. 서버가 시작되면 브라우저에서 `http://127.0.0.1:8080/`을 엽니다. 종료는 실행 창의 `Ctrl+C`입니다.

**WSL / Linux / macOS:**

```bash
./mvnw spring-boot:run
```

JDK 17 이상이 필요합니다. Maven은 별도 설치하지 않아도 되지만 최초 Wrapper 및 의존성 다운로드에는 인터넷이 필요합니다. 기본 데이터 파일 `dayspark_v2.mv.db` 및 `uploads/`는 실행 폴더에 남으므로 **폴더를 교체할 때 데이터 백업**이 필요합니다.

빈 DB를 처음 실행하면 예시 도전 4개가 자동 생성됩니다. 비활성화하려면 `app.seed-demo-data=false`를 설정하세요. 날짜·시간 계산은 `Asia/Seoul` 기준입니다.

## Windows에서 48개 테스트 한 번에 실행

DaySpark 폴더에서 **`RUN_ALL_TESTS.bat`를 더블클릭**합니다. Java 테스트와 Chrome 테스트를 차례로 실행한 뒤 결과 페이지를 자동으로 엽니다.

| 구분 | 개수 | 검증 내용 |
| --- | ---: | --- |
| 단위 테스트 | 28 | 연속 달성일, 복습 간격, 반복 일정, 집중 상태와 계산 규칙 |
| 통합 테스트 | 6 | HTTP·Service·JPA·H2를 연결한 저장·완료·취소·복습·삭제 |
| Chrome 시스템 테스트 | 14 | 실제 브라우저에서 수행하는 SYS-01부터 SYS-14까지의 사용자 동작 |
| **총계** | **48** | **모든 결과를 하나의 보고서로 집계** |

**준비물:** Windows 10/11, JDK 17 이상, Google Chrome. Node.js가 없으면 실행기가 검증된 공식 배포본을 `.tools/` 아래에 자동으로 준비하고, Playwright도 처음 실행할 때 설치합니다. 최초 실행에는 인터넷 연결이 필요할 수 있습니다.

**실행 결과는 실제 테스트 출력에서 생성**되며, 실패·미실행 항목을 성공으로 계산하지 않습니다. 프로그램 실행용 `RUN_DAYSPARK.bat`와 테스트용 `RUN_ALL_TESTS.bat`는 별개입니다.

```text
test-results/
  summary.html       전체 결과 및 개별 테스트 목록
  summary.txt        PASS/FAIL 요약
  junit/             단위·통합 테스트 JUnit XML
  browser/           시스템 테스트 결과 JSON·화면 캡처
  coverage/          JaCoCo 코드 커버리지
  logs/              테스트·서버 실행 로그
```

시스템 테스트는 **개인 기록용 `dayspark_v2.mv.db`와 `uploads/`를 사용하지 않습니다.** 별도의 H2 DB와 업로드 폴더를 `target/`에 만들어 사용하며, 결과물은 Git에 포함되지 않도록 설정되어 있습니다.

**확인된 결과(2026-11-20):** Windows 전체 실행 48/48 PASS(단위 28/28, 통합 6/6, Chrome 14/14). JUnit 결과 기준 JaCoCo **라인 커버리지 72.3%, 분기 커버리지 52.8%**를 기록했습니다. 이 수치는 브라우저 14개를 포함한 결합 커버리지가 아닙니다. 새 실행 시에는 `test-results/summary.html`에서 해당 회차 결과를 다시 확인하세요.

상세 내용: [테스트 실행 방법과 시나리오](docs/testing.md)

## 프로젝트 문서

| 문서 | 내용 |
| --- | --- |
| [요구사항](docs/requirements.md) | 기능·비기능 요구사항 및 수용 기준 |
| [업무 규칙](docs/domain-rules.md) | 완료·반복·Streak·복습·타이머 규칙 |
| [시스템 설계](docs/architecture.md) | 계층·UML·ERD·시퀀스·상태 모델 |
| [변경 이력](docs/CHANGELOG.md) | 버전별 기능 변경 |

## 소스 구성

```text
src/main/java/com/dayspark/app/
  controller/          화면 요청·JSON API·예외 처리
  service/             업무 흐름, 집계, 파일 저장
  domain/              반복·복습·연속 달성 규칙
  model/               Dare, Completion, ProgressLog, ScheduleException,
                       FocusSession, ReviewRecord
  repository/          JPA 데이터 접근
  config/              시간대·설정·예시 데이터
src/main/resources/
  templates/           Thymeleaf 화면
  static/              CSS 및 JavaScript
src/test/java/com/dayspark/app/
  *UnitTests.java      단위 테스트
  domain/              규칙 테스트
  model/               일정 테스트
  *IntegrationTest.java 통합 테스트
RUN_ALL_TESTS.bat
scripts/
  run_all_tests.ps1
  write_test_report.ps1
  system_browser_test.cjs
  system_browser_more_test.cjs
  system_browser_critical_test.cjs
```

## 검증 범위의 한계

48개 통과는 검증된 시나리오 범위에서의 결과입니다. **컨트롤러 분기 커버리지 15.8%** 등 아직 테스트하지 않은 오류·경계 조건이 남아 있습니다. 다른 브라우저, 동시 사용자·대규모 부하, 장기 실행, 접근성 및 사용자 만족도는 별도 검증이 필요합니다.
