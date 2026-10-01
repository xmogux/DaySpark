# DaySpark 설계

GitHub에서 이 파일을 열면 Mermaid 다이어그램이 그림으로 보입니다.
보고서(Word)에 넣을 때는 [mermaid.live](https://mermaid.live)에 코드 블록을 붙여 PNG/SVG로 내보내세요.

## 1. 계층 구조

```mermaid
flowchart TB
    Browser["브라우저<br/>Thymeleaf 페이지 + app.js / focus.js"]

    subgraph Web["controller — HTTP만 담당 (얇게)"]
        HC[HomeController<br/>오늘·완료·취소·기록]
        DC[DareController<br/>/challenges/**]
        CC[CalendarController]
        SC[StatsController]
        FC[FocusController + FocusApi<br/>타이머 페이지·JSON API]
        GE[GlobalExceptionHandler]
    end

    subgraph Service["service — 규칙과 계산"]
        DS[DareService]
        PS[PlanningService]
        CS[CompletionService]
        PR[ProgressService]
        FS[FocusService]
        RS[ReviewService]
        ST[StatsService]
        IS[ImageStorage]
    end

    subgraph Domain["domain / model — 순수 규칙 + 엔티티"]
        SK[StreakCalculator]
        RI[ReviewIntervals]
        EN[Dare · Completion · ProgressLog<br/>ScheduleException · FocusSession · ReviewRecord]
    end

    Repo["repository — Spring Data JPA"]
    DB[("H2 파일 DB")]
    Files[("uploads/ 사진")]

    Browser --> Web
    Web --> Service
    Service --> Domain
    Service --> Repo --> DB
    IS --> Files
```

- **Controller**는 요청 파라미터를 받고 서비스 결과를 모델에 담기만 합니다. 계산 로직이 없습니다.
- **StatsService**가 오늘 화면·달력·통계·도전 상세의 숫자를 모두 만들기 때문에 화면끼리 숫자가 어긋나지 않습니다.
- **domain** 패키지(`StreakCalculator`, `ReviewIntervals`, enum들)는 Spring·DB 없이 단위 테스트합니다.
- 모든 날짜·시간은 주입된 `Clock`에서 가져옵니다(테스트에서 고정 가능).

## 2. 주요 클래스

```mermaid
classDiagram
    direction LR

    class DareController {
        +save(id, DareRequest)
        +detail(id)
        +schedule(id, date, mode)
        +toggleReview(id, offset)
    }
    class HomeController {
        +home()
        +complete(dareId, note, image)
        +undoComplete(dareId)
    }
    class DareRequest {
        <<DTO>>
        title, category, minutes
        repeatType, repeatDays, startDate, endDate
        targetValue, targetUnit, weeklyTarget
        reviewEnabled, reviewBaseDate, reviewIntervals
        +from(Dare)$
        +startingOn(LocalDate)$
    }
    class DareService {
        +create(DareRequest) Dare
        +update(id, DareRequest) Dare
        +plannedFor(date) List~Dare~
        +toggle(id)
        +delete(id)
    }
    class PlanningService {
        +isScheduled(dare, date) boolean
        +setOverride(dare, date, mode, note)
    }
    class CompletionService {
        +complete(dare, date, note, image)
        +autoComplete(dare, date) Optional
        +undo(dareId, date)
    }
    class ProgressService {
        +add(dare, day, amount)
        +focusEarned(dare, day, minutes)
    }
    class FocusService {
        +start(dare, minutes)
        +pause(id) / resume(id)
        +finish(id)
    }
    class ReviewService {
        +planFor(dare) List~ReviewItem~
        +dueOn(today) List~ReviewItem~
        +toggle(dare, offset)
    }
    class StatsService {
        +today(date) TodayBoard
        +calendarMonth(month, today) CalendarMonth
        +overview(today) StatsOverview
        +dareInsights(dare, today) DareInsights
        +overallStreak(today) int
    }
    class Dare {
        <<Entity>>
        +describe(title, description, category, minutes)
        +reschedule(repeatType, days, start, end)
        +setGoals(target, unit, weekly)
        +configureReview(enabled, base, intervals)
        +pauseFrom(day) / resume()
        +isScheduledFor(date) boolean
    }
    class FocusSession {
        <<Entity>>
        +pause(now) / resume(now)
        +complete(now, zone)
        +remainingMillis(now) long
    }
    class StreakCalculator {
        <<domain>>
        +count(today, earliest, planned, achieved)$ int
    }
    class ReviewIntervals {
        <<value object>>
        +parse(raw)$
        +ofStored(stored)$
        +offsets() List~Integer~
        +normalized() String
    }

    DareController --> DareService
    DareController --> StatsService
    DareController ..> DareRequest
    HomeController --> StatsService
    HomeController --> CompletionService
    DareService ..> DareRequest
    DareService --> PlanningService
    DareService ..> ReviewIntervals
    CompletionService --> PlanningService
    ProgressService --> CompletionService : 목표 첫 도달 시 autoComplete
    FocusService --> ProgressService : 세션 종료 시 focusEarned
    StatsService --> DareService
    StatsService --> CompletionService
    StatsService --> ReviewService
    StatsService ..> StreakCalculator
    ReviewService ..> ReviewIntervals
    FocusService --> FocusSession
    DareService --> Dare
```

### 설계 결정

| 결정 | 이유 |
|---|---|
| 폼 전체를 `DareRequest` DTO 하나로 받고 `create(request)` / `update(id, request)` 두 메서드만 둠 | 기능이 늘 때마다 `createWithGoals…` 같은 메서드와 파라미터(최대 16개)가 늘던 구조를 끊기 위해 |
| `Dare`의 변경은 `describe / reschedule / setGoals / configureReview` 네 메서드로 | 의미 단위로 묶어 파라미터를 4개 이하로 유지 |
| 하루의 계획 = 예정된 도전 + 그날 완료한 도전 (`DareService.plannedFor`) | 규칙 변경 후 달성률 200% 같은 모순을 원천 차단. 모든 화면이 같은 메서드를 씀 |
| Streak 규칙을 `StreakCalculator` 순수 함수로 | 전체/도전별 두 곳의 루프가 서로 다른 조건을 쓰던 문제 제거, Spring 없이 테스트 |
| 복습 간격 파싱을 `ReviewIntervals` 값 객체로 | 두 서비스에 복붙돼 있던 파싱 코드를 하나로 |
| 자동 완료(`autoComplete`)는 예외를 던지지 않음 | 집중 세션 종료(finish)가 목표 처리 때문에 실패하면 세션이 RUNNING으로 남고 브라우저가 재시도 폭주 |
| 사진 저장·삭제를 `ImageStorage` 하나로 | 완료·완료 취소·도전 삭제가 같은 경로 검증 로직을 씀 |
| 내비게이션 바를 Thymeleaf fragment(`fragments/layout.html`)로 | 9개 템플릿에 복붙돼 서로 달라지던 문제 제거 |

## 3. 데이터 모델 (ERD)

JPA 엔티티에서 생성되는 실제 테이블 기준입니다(`spring.jpa.hibernate.ddl-auto=update`).

```mermaid
erDiagram
    DARES ||--o{ COMPLETIONS : "완료"
    DARES ||--o{ PROGRESS_LOGS : "진행량"
    DARES ||--o{ SCHEDULE_EXCEPTIONS : "일정 예외"
    DARES ||--o{ FOCUS_SESSIONS : "집중"
    DARES ||--o{ REVIEW_RECORDS : "복습 완료"

    DARES {
        bigint id PK
        varchar title "1~80자"
        varchar description
        varchar category "STUDY|HEALTH|MOVE|LIFE|WORK"
        int minutes
        varchar repeat_type "DAILY|WEEKDAYS|WEEKENDS|CUSTOM"
        varchar repeat_days "MONDAY,FRIDAY"
        date start_date
        date end_date "nullable"
        boolean active
        date paused_from "nullable"
        int target_value
        varchar target_unit "분|페이지|회|개|잔"
        int weekly_target "0~7"
        boolean review_enabled
        date review_base_date "nullable"
        varchar review_intervals "1,3,7"
    }
    COMPLETIONS {
        bigint id PK
        bigint dare_id FK
        date challenge_date "UNIQUE(dare_id, challenge_date)"
        varchar note "300자"
        varchar image_path "/uploads/uuid.png"
        timestamp completed_at
    }
    PROGRESS_LOGS {
        bigint id PK
        bigint dare_id FK
        date progress_date
        int amount
        varchar source "MANUAL|FOCUS"
        timestamp logged_at
    }
    SCHEDULE_EXCEPTIONS {
        bigint id PK
        bigint dare_id FK
        date exception_date "UNIQUE(dare_id, exception_date)"
        varchar mode "SKIP|ADD"
        varchar note
    }
    FOCUS_SESSIONS {
        bigint id PK
        bigint dare_id FK
        timestamp started_at
        timestamp paused_at
        timestamp ended_at
        bigint paused_millis
        int planned_minutes
        varchar status "RUNNING|PAUSED|COMPLETED|CANCELLED"
        date completed_day
    }
    REVIEW_RECORDS {
        bigint id PK
        bigint dare_id "FK 제약 없음 (한계 참고)"
        int offset_days "UNIQUE(dare_id, offset_days)"
        timestamp completed_at
    }
```

- 복습 **예정일은 저장하지 않습니다.** `review_base_date + offset_days`로 계산하고, 완료한 회차만 `review_records`에 남깁니다.
- 하루의 계획도 저장하지 않고 반복 규칙 + 예외 + 완료 기록으로 계산합니다.

## 4. 주요 흐름

### 4.1 오늘의 도전 완료

```mermaid
sequenceDiagram
    actor U as 사용자
    participant HC as HomeController
    participant CS as CompletionService
    participant PS as PlanningService
    participant IS as ImageStorage
    participant DB as CompletionRepository

    U->>HC: POST /complete (dareId, note, image)
    HC->>CS: complete(dare, today, note, image)
    CS->>PS: isScheduled(dare, today)
    alt 오늘 계획 아님
        CS-->>HC: IllegalArgumentException
        HC-->>U: redirect / + 오류 메시지
    else 이미 완료
        CS-->>HC: 기존 기록 반환 (중복 없음)
    else 정상
        CS->>IS: save(image) — 확장자 검사, UUID 파일명
        CS->>DB: save(Completion)
        HC-->>U: redirect / + "완료했어요"
    end
```

### 4.2 집중 세션 종료 → 진행량 → 자동 완료

```mermaid
sequenceDiagram
    participant JS as focus.js
    participant API as FocusApi
    participant FS as FocusService
    participant S as FocusSession
    participant PR as ProgressService
    participant CS as CompletionService

    JS->>API: POST /api/focus/{id}/finish (남은 시간 0)
    API->>FS: finish(id)
    FS->>S: complete(now, zone)
    Note over S: 남은 시간 > 0 이면 예외(400)<br/>completedDay = 계획 시간이 끝난 날짜
    FS->>PR: focusEarned(dare, completedDay, minutes)
    alt 목표 단위가 '분'이고 그날 계획됨
        PR->>PR: 진행량 기록
        opt 목표에 처음 도달
            PR->>CS: autoComplete(dare, day)
            Note over CS: 계획 없음·이미 완료면 아무것도 안 함<br/>(예외를 던지지 않음)
        end
    end
    API-->>JS: 200 {status: COMPLETED}
    Note over JS: 실패 시 /current로 상태 동기화,<br/>재시도는 5초에 1번
```

### 4.3 집중 세션 상태

```mermaid
stateDiagram-v2
    [*] --> RUNNING: start
    RUNNING --> PAUSED: pause
    PAUSED --> RUNNING: resume
    RUNNING --> COMPLETED: finish (남은 시간 0)
    PAUSED --> COMPLETED: finish (남은 시간 0, 재개 후 완료)
    RUNNING --> CANCELLED: cancel
    PAUSED --> CANCELLED: cancel
    COMPLETED --> [*]
    CANCELLED --> [*]
```

- 동시에 진행 중인 세션은 1개뿐입니다(`FocusService.start`).
- 서버가 시작 시각·누적 일시정지 시간을 저장하므로 새로고침이나 탭을 닫아도 세션이 유지됩니다.
