package com.dayspark.app.service;

import com.dayspark.app.model.Dare;
import com.dayspark.app.model.ProgressLog;
import com.dayspark.app.repository.ProgressLogRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProgressService {
    private final ProgressLogRepository logs;
    private final PlanningService planning;
    private final Clock clock;

    public ProgressService(ProgressLogRepository logs, PlanningService planning, Clock clock) {
        this.logs = logs;
        this.planning = planning;
        this.clock = clock;
    }

    public int progress(Dare dare, LocalDate day) {
        return logs.findByDareIdAndDay(dare.getId(), day).stream().mapToInt(ProgressLog::getAmount).sum();
    }

    public List<ProgressLog> forDare(Dare dare, LocalDate from, LocalDate to) {
        return logs.findByDareIdAndDayBetween(dare.getId(), from, to);
    }

    public void add(Dare dare, LocalDate day, int amount) {
        if (day.isAfter(LocalDate.now(clock)) || !planning.isScheduled(dare, day)) throw new IllegalArgumentException("예정된 날짜에만 진행량을 기록할 수 있습니다.");
        if (amount < 1 || amount > 10000) throw new IllegalArgumentException("1~10000 사이 수치를 입력해주세요.");
        logs.save(new ProgressLog(dare, day, amount, ProgressLog.SOURCE_MANUAL, LocalDateTime.now(clock)));
    }

    public void focusEarned(Dare dare, LocalDate day, int mins) {
        if ("분".equals(dare.getTargetUnit())) logs.save(new ProgressLog(dare, day, mins, ProgressLog.SOURCE_FOCUS, LocalDateTime.now(clock)));
    }
}
