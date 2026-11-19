# DaySpark

반복 목표를 오늘의 할 일로 정리하고 **완료·진행량·집중 시간·복습·달력·통계**를 하나의 화면 흐름으로 관리하는 단일 사용자용 로컬 웹 애플리케이션입니다. 소프트웨어공학·객체지향 설계 프로젝트입니다.

> **공식 기준:** GitHub `main` 브랜치의 소스와 테스트. 테스트 구성은 **총 48개(단위 28개 + 통합 6개 + 실제 Chrome 브라우저 시스템 14개)**이며, GitHub Actions에서 실행합니다.

## 주요 기능

| 화면 / 기능 | 주요 동작 |
| --- | --- |
| 오늘 | 반복 규칙에서 계산한 오늘의 도전 확인, 수동 완료·취소, 수치 진행률, 사진·메모, 오늘/지난 복습 |
| 도전 | 생성·수정·삭제, 쉬기·재개, 매일·평일·주말·선택 요일, 시작·종료일, 날짜별 예외(추가·쉬기·해제), 수치·주간 목표 |
| 집중 | 서버에 저장되는 집중 세션, 시작·일시정지·재개·중간 종료·도전 전환·새로고침 복원, 브라우저의 휴식·반복 회차 |
| 자동 완료 | 정상 완료된 집중 시간을 `분` 단위 목표에 적립하고, 해당 날짜 목표에 처음 도달하면 완료 기록 생성. 중간 종료는 집중 시간만 기록 |
| 복습 | 공부 도전의 기준일·복습 간격 설정, 예정일 표시, 완료·취소 |
| 달력·통계·기록 | 월간 달력과 날짜별 내역, 주간·월간 달성/집중 통계, 분류별 결과와 완료 기록 |

## 개발 환경 및 구조

| 항목 | 현재 사용 |
| --- | --- |
| 프로젝트 | DaySpark **5.6.0** |
| Java | **17** 기준(`pom.xml`); CI는 JDK 17·25에서 확인 |
| 서버 | Spring Boot **3.5.16** / Spring MVC / Thymeleaf |
| 데이터 | Spring Data JPA + H2 **파일 DB** |
| 웹 화면 | HTML·CSS·JavaScript, 서버 렌더링 Thymeleaf |
| 빌드 및 테스트 | Maven Wrapper / JUnit 5 / Mockito / MockMvc / JaCoCo / Playwright |
| 자동 검증 | GitHub Actions: Java CI + Chrome 브라우저 시스템 테스트 |
| 사용 환경 | 개인 PC, 기본 `127.0.0.1:8080`, 로그인/다중 사용자 미지원 |

요청은 `Controller/FocusApi → Service → Repository → H2` 순서로 처리합니다. 집중·완료·복습 데이터는 H2에 저장하고, 업로드한 이미지는 `uploads/`에 저장하며 DB에는 그 경로를 보관합니다. 브라우저 `localStorage`는 집중 UI 설정을 보조적으로 저장합니다.

## 실행

**Windows:** `RUN_DAYSPARK.bat`를 더블클릭합니다. 서버가 시작되면 브라우저에서 `http://127.0.0.1:8080/`을 엽니다. 종료는 실행 창의 `Ctrl+C`입니다.

**WSL / Linux / macOS:**

```bash
./mvnw spring-boot:run
```

JDK 17 이상이 필요합니다. Maven은 별도 설치하지 않아도 되지만 최초 Wrapper 및 의존성 다운로드에는 인터넷이 필요합니다. 기본 데이터 파일 `dayspark_v2.mv.db` 및 `uploads/`는 실행 폴더에 남으므로 **폴더를 교체할 때 데이터 백업**이 필요합니다.

## 테스트: 공식 48개

| 유형 | 개수 | 실행 도구 | 확인 대상 |
| --- | ---: | --- | --- |
| 단위 테스트 | **28** | JUnit 5 / Mockito | 연속 달성일, 복습 간격, 반복 일정, 집중 상태와 서비스 규칙 |
| 통합 테스트 | **6** | Spring Boot / MockMvc / JPA / 격리 H2 | 도전 저장, 집중·자동 완료, 일정 예외, 완료 취소, 복습, 연관 기록 삭제 |
| 시스템 테스트 | **14** | Playwright / 실제 Chrome / Spring Boot | 사용자가 브라우저에서 수행하는 기능별 흐름 |
| **총계** | **48** | **GitHub Actions** | **서로 다른 테스트 실행 항목 48개** |

단위·통합 테스트는 동일한 Java 테스트 34개를 JDK 17과 25에서 각각 실행합니다. **반복 실행을 새로운 테스트 개수로 중복 집계하지 않습니다.** 비슷한 기능을 여러 테스트 수준에서 확인하는 경우가 있으며 48가지의 서로 다른 제품 기능을 뜻하지는 않습니다.

**로컬에서 Java 테스트 34개 실행** (Windows PowerShell):

```powershell
.\mvnw.cmd clean verify
```

**WSL/Linux:**

```bash
./mvnw clean verify
```

로컬 명령은 **Java 테스트 34개만** 실행합니다. 브라우저 시스템 테스트 14개는 `.github/workflows/system-browser.yml`에서 실제 Chrome과 격리 DB를 사용해 별도로 자동 실행합니다. JaCoCo 리포트는 `target/site/jacoco/index.html`, JUnit 결과는 `target/surefire-reports/`에서 확인합니다. 테스트 결과는 실행된 커밋과 CI 로그를 기준으로 판단합니다.

- [48개 테스트 상세·실행 증빙](docs/TEST_VERIFICATION_2026-11-19.md)
- [실행 방법 및 종류별 설명](docs/testing.md)
- [Java CI](https://github.com/xmogux/DaySpark/actions/workflows/ci.yml)
- [브라우저 시스템 테스트 CI](https://github.com/xmogux/DaySpark/actions/workflows/system-browser.yml)

## 프로젝트 문서

| 문서 | 내용 |
| --- | --- |
| [요구사항](docs/requirements.md) | 기능·비기능 요구사항 및 수용 기준 |
| [업무 규칙](docs/domain-rules.md) | 완료·반복·Streak·복습·타이머 규칙 |
| [시스템 설계](docs/architecture.md) | 계층·UML·ERD·시퀀스·상태 모델 |
| [검증 결과](docs/TEST_VERIFICATION_2026-11-19.md) | 공식 48개 테스트의 범위와 실행 근거 |
| [한계](docs/limitations.md) | 범위 밖 기능과 알려진 제약 |
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
scripts/
  system_browser_test.cjs
  system_browser_more_test.cjs
  system_browser_critical_test.cjs
.github/workflows/
  ci.yml
  system-browser.yml
```

## 검증 범위의 한계

48개 통과는 **해당 코드와 CI 실행 환경**에서의 결과입니다. 개인 Windows PC에서의 모든 사용자 동작, 타 브라우저 호환성, 대규모 부하, 장기 실행, 접근성 및 사용자 만족도까지 검증했다고 해석하면 안 됩니다.
