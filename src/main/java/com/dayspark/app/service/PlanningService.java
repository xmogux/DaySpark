package com.dayspark.app.service;

import com.dayspark.app.domain.ScheduleMode;
import com.dayspark.app.model.Dare;
import com.dayspark.app.model.ScheduleException;
import com.dayspark.app.repository.CompletionRepository;
import com.dayspark.app.repository.ScheduleExceptionRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanningService {

    private final ScheduleExceptionRepository exceptions;
    private final CompletionRepository completions;
    private final Clock clock;

    public PlanningService(ScheduleExceptionRepository exceptions, CompletionRepository completions, Clock clock) {
        this.exceptions = exceptions;
        this.completions = completions;
        this.clock = clock;
    }

    public boolean isScheduled(Dare dare, LocalDate date) {
        if (!dare.isWithinDateRange(date) || dare.isPausedOn(date)) return false;
        Optional<ScheduleException> exception = exceptions.findByDareIdAndDay(dare.getId(), date);
        return exception.map(e -> ScheduleMode.ADD.name().equals(e.getMode())).orElse(dare.isScheduledFor(date));
    }

    public java.util.List<ScheduleException> forDare(Long id) {
        return exceptions.findByDareIdOrderByDayDesc(id);
    }

    @Transactional
    public void setOverride(Dare dare, LocalDate date, String mode, String note) {
        ScheduleMode parsedMode = ScheduleMode.parse(mode);
        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today)) throw new IllegalArgumentException("과거 일정은 수정할 수 없습니다.");
        if (!dare.isWithinDateRange(date)) throw new IllegalArgumentException(
            "도전 시작일과 종료일 사이만 변경할 수 있습니다."
        );
        // A completed date keeps its plan: SKIP or RESET would leave a completion on an unplanned day.
        if (
            parsedMode != ScheduleMode.ADD && completions.findByDareIdAndChallengeDate(dare.getId(), date).isPresent()
        ) {
            throw new IllegalArgumentException(
                "이미 완료한 날짜는 쉬기·예외 해제를 할 수 없습니다. 먼저 오늘 화면에서 완료를 취소해주세요."
            );
        }
        Optional<ScheduleException> current = exceptions.findByDareIdAndDay(dare.getId(), date);
        if (parsedMode == ScheduleMode.RESET) {
            current.ifPresent(exceptions::delete);
            return;
        }
        String trimmed = note == null ? "" : note.trim();
        if (trimmed.length() > 120) trimmed = trimmed.substring(0, 120);
        if (current.isPresent()) {
            current.get().update(parsedMode.name(), trimmed);
            exceptions.save(current.get());
        } else {
            exceptions.save(new ScheduleException(dare, date, parsedMode.name(), trimmed));
        }
    }
}
