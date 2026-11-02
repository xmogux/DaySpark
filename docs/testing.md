# DaySpark 단위테스트

이 프로젝트에 포함된 자동화 테스트는 **JUnit 5 단위테스트 25개(4개 클래스)**입니다.
Spring 컨텍스트나 데이터베이스를 띄우지 않고 주요 도메인 객체의 규칙을 검사합니다.

## 실행 방법

```bash
./mvnw test       # 단위테스트 실행
./mvnw verify     # 단위테스트 실행 + JaCoCo 리포트 생성
```

Windows CMD/PowerShell: `mvnw.cmd test` 또는 `mvnw.cmd verify`

| 확인할 결과 | 위치 |
|---|---|
| 개별 테스트 통과·실패 | `target/surefire-reports/` (`*.txt`, `TEST-*.xml`) |
| 라인·분기 커버리지 | `target/site/jacoco/index.html` (`verify` 실행 후) |
| CI 빌드 기록 | GitHub 저장소 → **Actions** 탭 (JDK 17 및 25) |

## 단위테스트 구성

| 파일 경로 (`src/test/java/com/dayspark/app/` 기준) | 개수 | 확인 내용 |
|---|---:|---|
| `domain/StreakCalculatorTest.java` | 6 | 연속 달성일, 오늘 미완료, 실패한 계획일, 쉬는 날, 시작일 및 최대 탐색 기간 |
| `domain/ReviewIntervalsTest.java` | 6 | 입력 정렬, 중복 제거, 전각 쉼표, 빈 값·숫자 오류, 간격 범위, 최대 12개 |
| `model/DareScheduleTest.java` | 5 | 반복 요일, 계획 기간, 쉬기 시작일 경계, 요일 표시, 복습 설정 저장 |
| `FocusSessionUnitTests.java` | 8 | 일시정지·재개·완료 조건, 취소 및 종료 상태, 실제 집중 시간, 날짜 경계 |
| **총계** | **25** | **JUnit 5 `@Test` 메서드 수** |

## 테스트 방식과 범위

- `StreakCalculator`와 `ReviewIntervals`: 규칙·정규화·예외 입력을 확인합니다.
- `Dare`: 반복 일정과 쉬기 시작일, 복습 설정의 상태를 확인합니다.
- `FocusSession`: 고정된 `Instant`를 직접 입력해 경과 시간, 일시정지 구간, 상태 전환, 자정 경계를 확인합니다.
- 모든 테스트는 외부 시간 대기, 실제 파일 DB, HTTP 요청 없이 실행되도록 작성했습니다.

현재 자동화 검증은 위 도메인 객체를 중심으로 합니다. Controller·Service·Repository를 연결한
전체 사용 흐름과 실제 데이터 저장·조회는 이 테스트 목록만으로 확인되지 않습니다.
필요한 기능은 실행 후 수동으로 확인해야 합니다.

## 실행 결과 기록

테스트 파일의 개수와 실제 실행 성공은 다릅니다. 결과가 필요할 때는 `./mvnw clean verify`를 실행하고
Surefire 요약의 `Tests run`, `Failures`, `Errors`, `Skipped`를 확인하세요.
JaCoCo는 측정 도구이며 현재 프로젝트에는 커버리지 최소 통과 기준이 지정되어 있지 않습니다.
이 배포본에 포함된 테스트의 **최신 Maven 실행 결과 및 커버리지 수치는 확인되지 않았습니다.**

## 수동 확인 권장 항목

| 항목 | 방법 |
|---|---|
| 앱 시작 | Windows에서 `RUN_DAYSPARK.bat` 실행 후 `http://127.0.0.1:8080/` 접속 |
| 도전 등록·수정·삭제 | 폼 정상 입력 및 오류 입력을 각각 시도하고 목록 반영 확인 |
| 일정 및 완료 | 쉬기/추가/해제, 오늘 완료·취소, 기록 반영 확인 |
| 사진 업로드 | 지원 형식·비지원 형식·용량 초과 후 결과와 파일 저장 확인 |
| 타이머 | 시작·일시정지·재개·중간 종료·도전 전환·새로고침 복원 확인 |
| 통계·달력·복습 | 날짜 변경과 복습 완료 상태가 화면에 일관되게 표시되는지 확인 |
| 데이터 유지 | 서버 재시작 후 기록·사진이 남아 있는지 확인 |
