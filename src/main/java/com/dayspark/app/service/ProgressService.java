package com.dayspark.app.service;

import com.dayspark.app.model.Dare;
import com.dayspark.app.model.ProgressLog;
import com.dayspark.app.repository.ProgressLogRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Numeric goal progress. Reaching the daily target for the first time completes the challenge
 * (see docs/domain-rules.md, "포모도로 연결").
 */
@Service
public class ProgressService {

    /** Unit whose progress can be earned by focus sessions. */
    public static final String MINUTES_UNIT = "분";

    private final ProgressLogRepository logs;
    private final PlanningService planning;
    private final CompletionService completions;
    private final Clock clock;

    public ProgressService(
        ProgressLogRepository logs,
        PlanningService planning,
        CompletionService completions,
        Clock clock
    ) {
        this.logs = logs;
        this.planning = planning;
        this.completions = completions;
        this.clock = clock;
    }

    public int progress(Dare dare, LocalDate day) {
        return logs.findByDareIdAndDay(dare.getId(), day).stream().mapToInt(ProgressLog::getAmount).sum();
    }

    public List<ProgressLog> forDare(Dare dare, LocalDate from, LocalDate to) {
        return logs.findByDareIdAndDayBetween(dare.getId(), from, to);
    }

    /** Progress typed in by the user. Only for planned days up to today. */
    @Transactional
    public void add(Dare dare, LocalDate day, int amount) {
        if (day.isAfter(LocalDate.now(clock)) || !planning.isScheduled(dare, day)) {
            throw new IllegalArgumentException("예정된 날짜에만 진행량을 기록할 수 있습니다.");
        }
        if (amount < 1 || amount > 10000) throw new IllegalArgumentException("1~10000 사이 수치를 입력해주세요.");
        record(dare, day, amount, ProgressLog.SOURCE_MANUAL);
    }

    /**
     * Minutes earned by a finished focus session. Counted only for '분' goals on a day the challenge
     * is planned; otherwise the session is kept as focus history only. Never throws, so finishing a
     * session cannot fail because of the goal.
     */
    @Transactional
    public void focusEarned(Dare dare, LocalDate day, int minutes) {
        if (!MINUTES_UNIT.equals(dare.getTargetUnit()) || !planning.isScheduled(dare, day)) return;
        record(dare, day, minutes, ProgressLog.SOURCE_FOCUS);
    }

    private void record(Dare dare, LocalDate day, int amount, String source) {
        int before = progress(dare, day);
        logs.save(new ProgressLog(dare, day, amount, source, LocalDateTime.now(clock)));
        int target = dare.getTargetValue();
        boolean firstReach = before < target && before + amount >= target;
        if (firstReach) completions.autoComplete(dare, day);
    }
}
