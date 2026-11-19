# DaySpark 테스트 실행 결과 및 증빙

> **검증 대상:** `main` / `a9b925775f58f036bc7ce3cebd2df46779ae54b5`  
> **실행 날짜:** 2026-11-19 (GitHub Actions UTC 기록)  
> **검증 방식:** GitHub-hosted Ubuntu · JDK 17/25 · 실제 Spring Boot 서버 · Chrome/Playwright · 격리된 H2 데이터베이스  
> **상태:** 아래 모든 테스트 성공, 실패 0개

## 1. 최종 결과 요약

| 검증 유형 | 실제 검사 | 통과 | 실패·오류·건너뜀 | 실행 환경 |
| --- | ---: | ---: | ---: | --- |
| 단위 테스트 (JUnit) | 28 | 28 | 0 | JDK 17, 25 각각 |
| 통합 테스트 (Spring Boot + MockMvc + JPA/H2) | 6 | 6 | 0 | JDK 17, 25 각각 |
| 시스템 테스트 (Playwright 실제 Chrome) | 14 | 14 | 0 | JDK 17 / Ubuntu 24.04 / Chrome |
| 기록 영속성 (실제 JVM 종료·재실행) | 2 | 2 | 0 | JDK 17 / 파일 H2 DB |
| **서로 다른 검증 항목 합계** | **50** | **50** | **0** | |

*JUnit 34개를 JDK 17과 25에서 각각 다시 실행했으며, 동일한 34개를 68개 고유 테스트로 합산하지 않았다. 영속성 검사 2개는 별도 쉘 스크립트이며 JUnit/Playwright 개수와 혼동하지 않는다.*

## 2. GitHub Actions 실행 증빙

| 원본 실행 기록 | GitHub 작업 | 결과 | 실제 로그에서 확인한 문구 |
| --- | --- | --- | --- |
| [CI #44512438749](https://github.com/xmogux/DaySpark/actions/runs/44512438749) | JDK 17 (job `135519814905`) | **PASS** | `Tests run: 34, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS` |
| [CI #44512438749](https://github.com/xmogux/DaySpark/actions/runs/44512438749) | JDK 25 (job `135519815473`) | **PASS** | `Tests run: 34, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS` |
| [System #44512438745](https://github.com/xmogux/DaySpark/actions/runs/44512438745) | Chrome E2E (job `135519815129`) | **PASS** | `[PASS] SYS-01` ... `[PASS] SYS-14` |
| [System #44512438745](https://github.com/xmogux/DaySpark/actions/runs/44512438745) | JVM restart (job `135519814720`) | **PASS** | `[PASS] PERSIST-01` / `[PASS] PERSIST-02` |

원본 GitHub 실행 로그 일부의 **정확한 타임스탬프와 원문**은 [로그 발췌 파일](test-evidence/2026-11-19-actions-log-excerpts.txt)에 보존하였다. 각 Actions 실행의 Artifacts 탭에서 테스트 보고서와 브라우저 스크린샷·서버 로그·재시작 전후 기록을 확인할 수 있다.

## 3. 단위 테스트 28개

| 테스트 클래스 | 수 | 검증 내용 | 결과 |
| --- | ---: | --- | --- |
| `StreakCalculatorTest` | 6 | 연속 달성일, 오늘 미완료 처리, 휴무일 건너뛰기, 범위 제한 | PASS |
| `ReviewIntervalsTest` | 6 | 복습 간격 정렬·중복 제거, 잘못된 입력·최대 개수 | PASS |
| `DareScheduleTest` | 5 | 일별·요일별 반복, 시작·종료 기간, 쉬기·재개, 복습 설정 | PASS |
| `FocusSessionUnitTests` | 9 | 일시정지·재개, 중간 종료, 취소, 집중 시간, 자정 날짜 처리 | PASS |
| `FocusServiceUnitTests` | 2 | 잘못된 전환 시 상태 보존, 완료 시에만 진행량 적립 및 중복 차단 | PASS |

소스: `src/test/java/com/dayspark/app/` 아래 5개 단위 테스트 클래스.

## 4. 통합 테스트 6개

| ID | 실제 연동 시나리오 | 결과 |
| --- | --- | --- |
| IT-01 | 도전 생성 HTTP 요청 → DB 저장 → 목록 조회 | PASS |
| IT-02 | 집중 완료 → 진행량 적립 → 자동 완료 1회만 생성 | PASS |
| IT-03 | 날짜별 예외 SKIP·ADD·RESET → 오늘 화면 반영 | PASS |
| IT-04 | 완료 기록 취소 → DB·이력·월별 통계 갱신 | PASS |
| IT-05 | 복습 완료·취소 → DB 및 상세 화면 반영 | PASS |
| IT-06 | 도전 삭제 → 완료·진행량·예외·복습·집중 기록 삭제 | PASS |

소스: `src/test/java/com/dayspark/app/DaySparkIntegrationTest.java`, `DaySparkMoreIntegrationTest.java`.

## 5. 실제 브라우저 시스템 테스트 14개

| ID | 자동 수행한 사용자 동작 | 결과 |
| --- | --- | --- |
| SYS-01 | 도전 등록 → 오늘 화면 → 완료 → 기록 조회 | PASS |
| SYS-02 | 실제 1분 집중 → 목표 자동 완료 → 통계 | PASS |
| SYS-03 | 빈 제목 입력 → 브라우저·서버 입력 오류 | PASS |
| SYS-04 | 반복 요일 수정 → 달력·오늘 화면 | PASS |
| SYS-05 | 타이머 일시정지 → 재개 → 중간 종료 | PASS |
| SYS-06 | 복습 플랜 설정 → 복습 완료 → 달력 반영 | PASS |
| SYS-07 | 도전 삭제 → 목록·완료 이력 삭제 | PASS |
| SYS-08 | 도전 쉬기 → 오늘 일정 제외 → 재개 복원 | PASS |
| SYS-09 | 주간 목표 2회 설정 → 완료 1회 → 1/2 표시 | PASS |
| SYS-10 | PNG 첨부 → HTTP 조회 → 완료 취소와 함께 사진 삭제 | PASS |
| SYS-11 | 실행 중인 집중 도전 A에서 B로 정상 전환 | PASS |
| SYS-12 | 집중 도중 새로고침 → 동일 서버 세션 복원 | PASS |
| SYS-13 | **실제 시간 경과** 1분 × 2회 반복 → 누적 시간·횟수 +2 | PASS |
| SYS-14 | 허용되지 않은 파일 확장자 업로드 → 오류 및 완료 기록 미생성 | PASS |

소스: `scripts/system_browser_test.cjs`, `scripts/system_browser_more_test.cjs`, `scripts/system_browser_critical_test.cjs`. 스크린샷과 `results.json`은 [브라우저 테스트 실행](https://github.com/xmogux/DaySpark/actions/runs/44512438745)의 artifact `browser-system-evidence`에서 확인 가능.

## 6. 서버 재시작 및 H2 보존 검사 2개

| ID | 검증 절차 | 결과 |
| --- | --- | --- |
| PERSIST-01 | 분리된 H2 파일 DB에 새 도전 저장 → 목록에서 조회 | PASS |
| PERSIST-02 | JVM 완전 종료 → 다른 JVM 프로세스로 재기동 → 앞서 저장한 도전 재조회 | PASS |

소스: `scripts/system_restart_persistence_test.sh`. [동일 시스템 실행](https://github.com/xmogux/DaySpark/actions/runs/44512438745)의 artifact `persistence-restart-evidence`에 재시작 전후 출력 및 서버 로그가 보관된다.

## 7. 검증 범위

이 문서는 **해당 커밋, 해당 자동 실행 환경에서** 위 시나리오가 통과했다는 증거이다. 개인 Windows PC의 직접 실행, Firefox/Edge, 장시간·고부하, 사용성 평가는 위 50개 통과 결과에 포함되지 않는다.
