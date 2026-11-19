# DaySpark 통합·시스템 테스트 실행 및 증빙

> 이 문서는 테스트 코드를 작성한 것과 **실제로 성공한 것**을 구분한다. 
> GitHub Actions 실제 실행 근거: [통합 테스트 CI (JDK 17/25)](https://github.com/xmogux/DaySpark/actions/runs/44288366368),
> [브라우저 시스템 테스트 7개](https://github.com/xmogux/DaySpark/actions/runs/44288308846).
> 실행 기록은 해당 커밋을 기준으로 하며 이후 코드 변경 시 다시 검증한다.

## 검증 환경

- 자동 통합 테스트: Spring Boot, MockMvc, JPA, 격리된 H2 메모리 DB (개인 DB 사용 안 함)
- 시스템 테스트: Ubuntu 24.04, JDK 17, Google Chrome + Playwright, 실제 DaySpark 서버 실행
- 시스템 테스트 DB: `jdbc:h2:file:./target/system-test-db` (운영/개인용 `dayspark_v2`와 분리)
- 서버/브라우저 시간대: Asia/Seoul
- 시스템 테스트 증빙: GitHub Actions의 `browser-system-evidence` artifact에 화면 캡처, `results.json`, 서버 로그 보관

## 통합 테스트: 6개 PASS

| ID | 검증 흐름 | 코드 |
| --- | --- | --- |
| IT-01 | 도전 생성 HTTP 요청 → DB 저장 → 목록 표시 | `DaySparkIntegrationTest` |
| IT-02 | 집중 세션 완료 → 진행량 → 자동 완료, 중복 기록 방지 | `DaySparkIntegrationTest` |
| IT-03 | 일정 예외 SKIP / ADD / RESET → 실제 오늘 화면 표시 | `DaySparkMoreIntegrationTest` |
| IT-04 | 완료 기록 취소 → DB / 완료 기록 / 월간 통계 원상복구 | `DaySparkMoreIntegrationTest` |
| IT-05 | 복습 완료/취소 → DB 상태와 복습 상세 UI 반영 | `DaySparkMoreIntegrationTest` |
| IT-06 | 도전 삭제 → 완료·수치 진행·일정 예외·복습·집중 연관 기록 삭제 | `DaySparkMoreIntegrationTest` |

실행 로그: JDK 17/25 `Tests run: 34, Failures: 0, Errors: 0` (단위 28 + 통합 6).
 
## 브라우저 시스템 테스트: 14개 (CI 결과 기준)

| ID | 브라우저에서 실제 수행한 시나리오 | 코드 |
| --- | --- | --- |
| SYS-01 | 도전 생성 → 오늘 화면 → 완료 → 완료 기록 | `system_browser_test.cjs` |
| SYS-02 | 실제 1분 타이머 → 자동 완료 → 통계 | `system_browser_test.cjs` |
| SYS-03 | 빈 제목 입력 차단 → 서버 유효성 검증 | `system_browser_test.cjs` |
| SYS-04 | 반복 요일 변경 → 오늘·달력 반영 | `system_browser_more_test.cjs` |
| SYS-05 | 타이머 일시정지 → 재개 → 중간 종료 | `system_browser_more_test.cjs` |
| SYS-06 | 복습 일정 설정 → 복습 완료 → 달력 표시 | `system_browser_more_test.cjs` |
| SYS-07 | 도전 삭제 → 목록·완료 기록 제거 | `system_browser_more_test.cjs` |
| SYS-08 | 도전 쉬기·재개 → 오늘 일정 제거·복구 | `system_browser_critical_test.cjs` |
| SYS-09 | 주간 목표 2회 설정 → 완료 1회 → 1/2 표시 | `system_browser_critical_test.cjs` |
| SYS-10 | PNG 업로드 → 기록에서 파일 조회 → 완료 취소 시 파일 삭제 | `system_browser_critical_test.cjs` |
| SYS-11 | 실행 중 집중 A → 도전 B 정상 전환, 이전 세션 종료 | `system_browser_critical_test.cjs` |
| SYS-12 | 진행 중인 타이머 새로고침 → 동일 서버 세션 복원 | `system_browser_critical_test.cjs` |
| SYS-13 | 1분 집중 × 2회 → 분·완료 회차 각각 +2 | `system_browser_critical_test.cjs` |
| SYS-14 | 금지 이미지 확장자 거부 → 완료 기록 미생성 | `system_browser_critical_test.cjs` |

SYS-01~07의 과거 검증 결과는 GitHub Actions의 [기존 통과 기록](https://github.com/xmogux/DaySpark/actions/runs/44289301641)을 참조한다. SYS-08~14는 최신 PR/메인 CI 실행 결과를 기준으로 판정하며 결과를 사전 확정하지 않는다.
단, GitHub 환경에서의 성공은 모든 기기·브라우저에서의 동작을 보장하지 않는다.

## 재실행

```bash
./mvnw clean verify
```

브라우저 시스템 테스트는 `.github/workflows/system-browser.yml`에서
PR과 `main` 푸시마다 실제 서버를 실행한 뒤 다음 스크립트를 실행한다.

```bash
node scripts/system_browser_test.cjs
node scripts/system_browser_more_test.cjs
node scripts/system_browser_critical_test.cjs
```

운영 DB는 건드리지 않아야 한다. 버그가 발생하면 오류 스택, 스크린샷, 서버 로그를 증빙에 남긴다.

## 실제 JVM 재시작을 통한 기록 보존 검사

`scripts/system_restart_persistence_test.sh`는 개인 데이터베이스와 분리된 H2 파일 DB에서 다음 두 항목을 검증한다.

| ID | 검증 내용 |
| --- | --- |
| PERSIST-01 | 실 서버에서 도전 생성 → 파일 DB 저장 → 목록 조회 |
| PERSIST-02 | JVM 종료 → 새 JVM 프로세스 기동 → 동일 DB의 기존 도전 조회 |

GitHub Actions의 `restart-persistence` 작업은 브라우저 테스트와 별도의 실제 서버 재시작 검증을 수행한다. 이 결과는 Maven JUnit/Playwright 사례 개수에 중복 합산하지 않는다.
