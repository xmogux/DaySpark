# DaySpark 통합·브라우저 시스템 테스트 (총 20개)

이 문서는 **48개 공식 테스트 중 통합 테스트 6개와 브라우저 테스트 14개**를 설명합니다. 전체 검증표 및 실행 근거는 [공식 테스트 48개 보고서](TEST_VERIFICATION_2026-11-19.md)를 참조하세요.

## 실행 환경

- 통합: Spring Boot · MockMvc · JPA · 별도 H2 메모리 DB
- 브라우저: Ubuntu 24.04 · JDK 17 · 실제 Spring Boot 서버 · Chrome/Playwright
- 브라우저 전용 DB: `jdbc:h2:file:./target/system-test-db` (개인 H2 DB와 분리)
- 시간대: `Asia/Seoul`
- 증빙: GitHub Actions의 `browser-system-evidence` artifact에 스크린샷·JSON·서버 로그

## 통합 테스트 6개

| ID | 검증 흐름 | 클래스 |
| --- | --- | --- |
| IT-01 | 도전 HTTP 등록 → H2 저장 → 목록 | `DaySparkIntegrationTest` |
| IT-02 | 집중 완료 → 진행량 적립 → 자동 완료/중복 차단 | `DaySparkIntegrationTest` |
| IT-03 | 일정 예외 SKIP/ADD/RESET → 오늘 화면 | `DaySparkMoreIntegrationTest` |
| IT-04 | 완료 취소 → DB·기록·통계 복원 | `DaySparkMoreIntegrationTest` |
| IT-05 | 복습 토글 → DB·화면 반영 | `DaySparkMoreIntegrationTest` |
| IT-06 | 도전 삭제 → 연관 기록 정리 | `DaySparkMoreIntegrationTest` |

[JDK 17·25 Java CI](https://github.com/xmogux/DaySpark/actions/runs/44512438749)에서 각 환경당 단위 28개와 통합 6개, 총 34개가 통과했습니다.

## Chrome 브라우저 시스템 테스트 14개

| ID | 시나리오 | 스크립트 |
| --- | --- | --- |
| SYS-01 | 도전 생성 → 오늘 → 완료 → 기록 | `system_browser_test.cjs` |
| SYS-02 | 1분 타이머 → 자동 완료 → 통계 | `system_browser_test.cjs` |
| SYS-03 | 빈 제목 입력 거부 | `system_browser_test.cjs` |
| SYS-04 | 반복 요일 변경 → 달력 | `system_browser_more_test.cjs` |
| SYS-05 | 타이머 일시정지·재개·중간 종료 | `system_browser_more_test.cjs` |
| SYS-06 | 복습 예정·완료·달력 | `system_browser_more_test.cjs` |
| SYS-07 | 도전 삭제·기록 제거 | `system_browser_more_test.cjs` |
| SYS-08 | 도전 쉬기·재개 | `system_browser_critical_test.cjs` |
| SYS-09 | 주간 목표 달성 횟수 | `system_browser_critical_test.cjs` |
| SYS-10 | PNG 첨부·조회·삭제 | `system_browser_critical_test.cjs` |
| SYS-11 | 집중 도전 전환 | `system_browser_critical_test.cjs` |
| SYS-12 | 새로고침 후 세션 복원 | `system_browser_critical_test.cjs` |
| SYS-13 | 1분×2회 집중 누적 | `system_browser_critical_test.cjs` |
| SYS-14 | 금지 확장자 거부 | `system_browser_critical_test.cjs` |

[실제 Chrome 브라우저 실행 결과](https://github.com/xmogux/DaySpark/actions/runs/44512438745)에서 SYS-01~SYS-14가 통과했습니다.

## 재실행

Java 테스트 (34개):

```bash
./mvnw clean verify
```

Chrome 시스템 테스트 (14개)는 GitHub Actions `.github/workflows/system-browser.yml`에서 서버·격리 DB·Playwright를 준비한 뒤 아래 코드를 실행합니다.

```bash
node scripts/system_browser_test.cjs
node scripts/system_browser_more_test.cjs
node scripts/system_browser_critical_test.cjs
```

운영 데이터베이스와 사용자 업로드 파일은 테스트 대상으로 사용하지 않습니다. 이 문서에 없는 수동 검사나 과거 실행 기록은 **48개 공식 테스트 합계에 포함하지 않습니다.**
