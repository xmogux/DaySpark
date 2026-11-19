# DaySpark 공식 테스트 실행 결과 · 48개

> **검증 대상:** `main` / `a9b925775f58f036bc7ce3cebd2df46779ae54b5` (아래 원본 증빙 로그의 커밋)  
> **확인 날짜:** 2026-11-19  
> **검증 환경:** GitHub-hosted Ubuntu · JDK 17/25 · Spring Boot · 격리 H2 · Chrome/Playwright  
> **현행 공식 범위:** 단위 28 + 통합 6 + 브라우저 시스템 14 = **48개**

## 전체 결과

| 분류 | 실행 항목 | 통과 | 실패/오류/건너뜀 | 실행 환경 |
| --- | ---: | ---: | ---: | --- |
| 단위 테스트 (JUnit) | 28 | 28 | 0 | JDK 17 / 25 |
| 통합 테스트 (MockMvc + Spring Boot + H2) | 6 | 6 | 0 | JDK 17 / 25 |
| 브라우저 시스템 테스트 (Chrome/Playwright) | 14 | 14 | 0 | JDK 17, Ubuntu 24.04 |
| **합계** | **48** | **48** | **0** | |

JUnit 34개는 JDK 17과 25 각각에서 재실행했으며 동일한 테스트를 중복 계산하지 않았습니다. 여러 계층에서 같은 기능을 확인하는 테스트는 있을 수 있으므로 48가지의 서로 다른 사용자 기능을 뜻하지 않습니다.

## 실제 GitHub Actions 증빙

**수정 코드 검증:** 커밋 `75247fc`에서 [Java CI JDK 17·25](https://github.com/xmogux/DaySpark/actions/runs/44536227536)와 [Chrome 시스템 테스트 14개](https://github.com/xmogux/DaySpark/actions/runs/44536226641)가 모두 성공했습니다. 아래 발췌 로그는 이전 커밋을 대상으로 한 독립적인 원문 증빙입니다.

| 실행 | 검증 | 결과 | 확인된 로그 |
| --- | --- | --- | --- |
| [Java CI 실행 #44512438749](https://github.com/xmogux/DaySpark/actions/runs/44512438749) | JDK 17 | PASS | `Tests run: 34, Failures: 0, Errors: 0, Skipped: 0` |
| [Java CI 실행 #44512438749](https://github.com/xmogux/DaySpark/actions/runs/44512438749) | JDK 25 | PASS | `Tests run: 34, Failures: 0, Errors: 0, Skipped: 0` |
| [브라우저 실행 #44512438745](https://github.com/xmogux/DaySpark/actions/runs/44512438745) | Chrome | PASS | `SYS-01`~`SYS-14` 전체 PASS |

- 원문 발췌 로그: [2026-11-19 Actions 로그](test-evidence/2026-11-19-actions-log-excerpts.txt)
- Java 상세 보고서: CI artifact `surefire-reports-jdk17`, `surefire-reports-jdk25`
- 브라우저 화면 및 결과: 시스템 실행 artifact `browser-system-evidence`

## 단위 테스트 28개

| 클래스 | 수 | 검사 |
| --- | ---: | --- |
| `StreakCalculatorTest` | 6 | 연속 달성일·계획일·조회 경계 |
| `ReviewIntervalsTest` | 6 | 복습 간격 정규화·오류·범위 |
| `DareScheduleTest` | 5 | 반복 일정·기간·쉬기·복습 설정 |
| `FocusSessionUnitTests` | 9 | 집중 상태와 시간·날짜 계산 |
| `FocusServiceUnitTests` | 2 | 도전 전환과 집중 적립 규칙 |
| **합계** | **28** | |

## 통합 테스트 6개

| ID | 검사 항목 | 결과 |
| --- | --- | --- |
| IT-01 | 도전 생성 요청 → DB 저장 → 목록 조회 | PASS |
| IT-02 | 집중 완료 → 진행량 → 자동 완료(중복 방지) | PASS |
| IT-03 | 일정 예외 SKIP/ADD/RESET 및 재개 후 휴식일 보존 | PASS |
| IT-04 | 사진 포함 완료 취소 → DB·이력·통계 및 사진 파일 정리 | PASS |
| IT-05 | 복습 완료·취소 → DB·상세 화면 반영 | PASS |
| IT-06 | 도전 삭제 → 연관 기록 정리 | PASS |

## 브라우저 시스템 테스트 14개

| ID | 사용자 흐름 | 결과 |
| --- | --- | --- |
| SYS-01 | 도전 등록 → 오늘 계획 → 완료 → 기록 | PASS |
| SYS-02 | 1분 집중 타이머 → 자동 완료 → 통계 | PASS |
| SYS-03 | 빈 제목 입력·서버 유효성 검사 | PASS |
| SYS-04 | 반복 요일 수정 → 오늘·달력 반영 | PASS |
| SYS-05 | 집중 일시정지·재개·중간 종료 | PASS |
| SYS-06 | 복습 계획 설정·완료·달력 반영 | PASS |
| SYS-07 | 도전 삭제 → 목록·완료 기록 삭제 | PASS |
| SYS-08 | 도전 쉬기·재개 → 오늘 일정 반영 | PASS |
| SYS-09 | 주간 목표 2회 → 1회 완료 반영 | PASS |
| SYS-10 | PNG 첨부·조회·완료 취소와 파일 삭제 | PASS |
| SYS-11 | 진행 중 도전 A → B 전환 | PASS |
| SYS-12 | 집중 도중 새로고침 → 서버 세션 복원 | PASS |
| SYS-13 | 1분 × 2회 실제 경과 → 시간·횟수 누적 | PASS |
| SYS-14 | 금지 확장자·실제 내용이 이미지가 아닌 위장 PNG 거부 | PASS |

## 검증의 한계

위 PASS는 **표시된 실행 커밋과 환경**에 대한 결과입니다. 개인 Windows PC, 다른 브라우저, 장시간·고부하, 접근성, 사용성 평가를 검증했다는 뜻은 아닙니다.
