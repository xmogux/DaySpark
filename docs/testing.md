# DaySpark 공식 자동 테스트 (48개)

현재 `main` 브랜치의 공식 자동 테스트는 **단위 28 + 통합 6 + 브라우저 시스템 14 = 총 48개**입니다. 예전 별도 로컬 테스트 목록이나 과거 실행 횟수를 더하지 않습니다.

| 유형 | 개수 | 위치 | 실행 |
| --- | ---: | --- | --- |
| 단위 테스트 | 28 | `src/test/java/com/dayspark/app/` | Maven JUnit |
| 통합 테스트 | 6 | `DaySparkIntegrationTest.java`, `DaySparkMoreIntegrationTest.java` | Maven JUnit + MockMvc + 격리 H2 |
| 브라우저 시스템 테스트 | 14 | `scripts/system_browser_*.cjs`, `scripts/system_browser_test.cjs` | GitHub Actions Chrome/Playwright |
| **총계** | **48** | | |

## 단위 테스트 28개

| 테스트 클래스 | 개수 | 검사 내용 |
| --- | ---: | --- |
| `domain/StreakCalculatorTest` | 6 | 연속 달성일, 계획이 없는 날, 시작일, 탐색 범위 |
| `domain/ReviewIntervalsTest` | 6 | 복습 입력 정렬·중복 제거·오류·범위·개수 |
| `model/DareScheduleTest` | 5 | 반복 방식, 날짜 범위, 쉬기, 요일 표시, 복습 설정 |
| `FocusSessionUnitTests` | 9 | 일시정지·재개·취소·중간 종료·완료와 시간/날짜 |
| `FocusServiceUnitTests` | 2 | 도전 전환 오류의 상태 보존, 집중 완료 적립·중복 방지 |
| **합계** | **28** | |

단위 테스트는 고정 시간과 Mockito 등을 이용해 규칙을 검사하며 운영용 DB를 사용하지 않습니다.

## 통합 테스트 6개

| ID | 검사 |
| --- | --- |
| IT-01 | 도전 등록 HTTP → H2 저장 → 목록 조회 |
| IT-02 | 집중 완료 → 진행량 → 자동 완료(중복 차단) |
| IT-03 | 일정 예외 및 재개 후 과거 휴식일 보존 → 화면 표시 |
| IT-04 | 사진을 포함한 완료 취소 → 기록/통계 반영·사진 파일 삭제 |
| IT-05 | 복습 완료/취소 → DB 및 화면 |
| IT-06 | 도전 삭제 → 관련 기록 정리 |

통합 테스트는 Spring 컨텍스트, MockMvc와 별도 H2 메모리 DB를 사용합니다.

## 실제 Chrome 시스템 테스트 14개

SYS-01~SYS-14는 실제 Spring Boot 앱을 구동하고 Playwright로 브라우저 UI를 검사합니다. SYS-14에서는 금지 확장자 및 위장 PNG(확장자만 이미지)를 모두 거부하는지 확인합니다. 개별 시나리오 및 통과 증빙은 [공식 테스트 검증 보고서](TEST_VERIFICATION_2026-11-19.md)에 정리되어 있으며, 실행 스크립트는 `scripts/system_browser_test.cjs`, `scripts/system_browser_more_test.cjs`, `scripts/system_browser_critical_test.cjs`입니다.

## 실행 방법

**Windows:**

```powershell
.\mvnw.cmd clean verify
```

**WSL/Linux/macOS:**

```bash
./mvnw clean verify
```

위 Maven 명령으로 실행되는 것은 **JUnit 34개(단위 28 + 통합 6)**입니다. 실제 Chrome 시스템 테스트 14개는 GitHub Actions의 [Browser system tests](https://github.com/xmogux/DaySpark/actions/workflows/system-browser.yml)에서 별도로 실행합니다.

| 증빙 | 경로 |
| --- | --- |
| 단위/통합 테스트 결과 | `target/surefire-reports/TEST-*.xml` |
| 코드 커버리지 | `target/site/jacoco/index.html` |
| 브라우저 스크린샷·결과 JSON | GitHub Actions `browser-system-evidence` artifact |
| 공식 PASS 로그 | [테스트 검증 보고서](TEST_VERIFICATION_2026-11-19.md) |

JDK 17과 25에서 똑같은 JUnit 34개를 다시 실행하더라도 **34개 테스트로 집계**합니다. 같은 기능을 단위·통합·시스템 테스트에서 중복 검증하는 것은 서로 다른 검증 수준입니다.

검증 결과는 로그가 남아 있는 정확한 커밋과 환경에서만 보장하며, 미실행 환경에서의 통과를 단정하지 않습니다.
