package com.dayspark.app.domain;

import java.time.LocalDate;
import java.util.function.Predicate;

/**
 * Counts a streak of consecutive planned days. Pure function: no database, no clock.
 *
 * <p>Rules (see docs/domain-rules.md, "Streak"):
 *
 * <ol>
 *   <li>Count backwards from {@code today}. If today is not achieved yet, start from yesterday:
 *       an unfinished today does not break the streak.
 *   <li>Days that are not planned are skipped: they neither extend nor break the streak.
 *   <li>A planned day that is achieved adds one to the streak.
 *   <li>The first planned day that is not achieved ends the streak.
 *   <li>Never look before {@code earliest} and never more than {@link #MAX_LOOKBACK_DAYS} days.
 * </ol>
 */
public final class StreakCalculator {

    public static final int MAX_LOOKBACK_DAYS = 366;

    private StreakCalculator() {}

    public static int count(
        LocalDate today,
        LocalDate earliest,
        Predicate<LocalDate> planned,
        Predicate<LocalDate> achieved
    ) {
        if (earliest == null || today.isBefore(earliest)) return 0;
        LocalDate day = achieved.test(today) ? today : today.minusDays(1);
        int streak = 0;
        for (int checked = 0; checked < MAX_LOOKBACK_DAYS && !day.isBefore(earliest); checked++) {
            if (planned.test(day)) {
                if (!achieved.test(day)) break;
                streak++;
            }
            day = day.minusDays(1);
        }
        return streak;
    }
}
