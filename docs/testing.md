# DaySpark Windows 자동 테스트 (총 48개)

프로젝트 최상위 폴더의 **`RUN_ALL_TESTS.bat`를 더블클릭**하면 단위·통합·시스템 테스트 48개를 순서대로 실행하고, 종료 후 `test-results/summary.html`을 엽니다. 검증 결과는 각 테스트의 실제 출력에서 읽어 집계하며, 누락된 항목은 성공 처리하지 않습니다.

## 테스트 구성

| 구분 | 개수 | 구현 위치 | 검증 방식 |
| --- | ---: | --- | --- |
| 단위 테스트 | 28 | `src/test/java/com/dayspark/app/` | Maven · JUnit 5 · Mockito |
| 통합 테스트 | 6 | `DaySparkIntegrationTest`, `DaySparkMoreIntegrationTest` | Spring Boot · MockMvc · 격리 H2 |
| 시스템 테스트 | 14 | `scripts/system_browser_*.cjs`, `scripts/system_browser_test.cjs` | Windows · 실제 Chrome · Playwright |
| **총계** | **48** | | |

단위 테스트 상세 구성은 `StreakCalculatorTest` 6개, `ReviewIntervalsTest` 6개, `DareScheduleTest` 5개, `FocusSessionUnitTests` 9개, `FocusServiceUnitTests` 2개다.

## 실행 방법

1. Windows 10/11, **JDK 17 이상**, **Google Chrome**을 준비한다.
2. `RUN_ALL_TESTS.bat`를 더블클릭한다.
3. 테스트가 종료되면 자동으로 열리는 `test-results/summary.html`에서 48개 항목의 결과를 확인한다.

처음 실행할 때 Maven Wrapper의 의존성과 Playwright 라이브러리가 내려받아질 수 있다. Node.js가 없다면 공식 SHA-256 체크섬으로 검증한 Node.js 22 휴대용 배포본을 `.tools/`에 설치한다. 관리자 권한은 필요하지 않다.

테스트 실행기는 아래 단계를 자동으로 수행한다.

1. `mvnw.cmd -B -ntp clean verify`로 JUnit 34개를 실행한다.
2. 결과 XML과 JaCoCo 커버리지 자료를 저장한다.
3. 임의의 사용 가능한 로컬 포트로 Spring Boot를 실행한다.
4. 개인 데이터와 분리된 `target/system-test-db` 및 `target/system-test-uploads`를 사용한다.
5. 기존 Playwright 스크립트 3개를 순서대로 실행해 SYS-01부터 SYS-14까지 확인한다.
6. JUnit XML·브라우저 JSON을 집계해 HTML·텍스트 보고서를 저장하고 테스트용 서버를 종료한다.

## 결과 파일

| 파일·폴더 | 내용 |
| --- | --- |
| `test-results/summary.html` | 성공/실패/미실행 구분, 전체 48개 목록 |
| `test-results/summary.txt` | 구분별 개수와 오류 요약 |
| `test-results/junit/` | 단위·통합 JUnit XML |
| `test-results/browser/` | Chrome 시스템 테스트 JSON·스크린샷 |
| `test-results/coverage/` | JaCoCo HTML·CSV·XML 커버리지 |
| `test-results/logs/` | 빌드, 브라우저, 테스트 서버 로그 |

**주의:** 같은 경로에 다시 실행하면 이전 `test-results/`의 최신 결과가 갱신된다. 제출용 증빙이 필요하면 실행 직후 해당 폴더를 별도로 복사한다. 실제 데이터인 `dayspark_v2.mv.db`와 `uploads/`는 테스트에 사용하지 않는다.

## 통합 테스트 6개

| ID | 검증 내용 |
| --- | --- |
| IT-01 | HTTP 도전 생성 → 저장·조회 |
| IT-02 | 중복 집중 시작 거부, 정상 완료와 자동 완료 |
| IT-03 | 일정 예외 및 재개 후 과거 휴식일 보존 |
| IT-04 | 사진 포함 완료 취소와 파일·통계 갱신 |
| IT-05 | 복습 완료/취소 |
| IT-06 | 도전 삭제 후 연관 기록 정리 |

## Chrome 시스템 테스트 14개

| ID | 검증 내용 |
| --- | --- |
| SYS-01 | 도전 생성 → 완료 → 기록 조회 |
| SYS-02 | 1분 집중 → 자동 완료 및 통계 |
| SYS-03 | 빈 제목 입력 거부 |
| SYS-04 | 반복 요일 수정과 달력 반영 |
| SYS-05 | 타이머 일시정지·재개·중간 종료 |
| SYS-06 | 복습 계획·완료·달력 반영 |
| SYS-07 | 도전 삭제 및 완료 기록 정리 |
| SYS-08 | 도전 쉬기·재개 |
| SYS-09 | 주간 목표 달성 횟수 |
| SYS-10 | 사진 첨부·조회·완료 취소 |
| SYS-11 | 집중 도전 전환 |
| SYS-12 | 세션 새로고침 복원 |
| SYS-13 | 실제 1분 집중 2회 누적 |
| SYS-14 | 허용하지 않은 파일 및 위장 이미지 거부 |

단위·통합 테스트는 `mvnw.cmd clean verify`만으로 별도 실행할 수도 있지만, **48개 전체 결과**는 `RUN_ALL_TESTS.bat`로 확인한다.
