package com.dayspark.app.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class StreakCalculatorTest {

    private static final LocalDate WED = LocalDate.of(2026, 10, 7);
    private static final LocalDate START = WED.minusDays(60);
    private static final Predicate<LocalDate> EVERY_DAY = d -> true;

    private static Predicate<LocalDate> doneOn(LocalDate... days) {
        Set<LocalDate> set = Set.of(days);
        return set::contains;
    }

    @Test
    void consecutiveAchievedDaysAreCounted() {
        assertThat(
            StreakCalculator.count(WED, START, EVERY_DAY, doneOn(WED, WED.minusDays(1), WED.minusDays(2)))
        ).isEqualTo(3);
    }

    @Test
    void unfinishedTodayDoesNotBreakTheStreak() {
        assertThat(StreakCalculator.count(WED, START, EVERY_DAY, doneOn(WED.minusDays(1), WED.minusDays(2)))).isEqualTo(
            2
        );
    }

    @Test
    void aMissedPlannedDayEndsTheStreak() {
        // Tuesday missed: only Wednesday counts even though Monday was done.
        assertThat(StreakCalculator.count(WED, START, EVERY_DAY, doneOn(WED, WED.minusDays(2)))).isEqualTo(1);
    }

    @Test
    void unplannedDaysAreSkipped() {
        Predicate<LocalDate> weekdays = d -> d.getDayOfWeek().getValue() <= 5;
        LocalDate mon = WED.with(DayOfWeek.MONDAY);
        LocalDate lastFri = mon.minusDays(3);
        // Wed, Tue, Mon, (Sun, Sat skipped), Fri
        assertThat(StreakCalculator.count(WED, START, weekdays, doneOn(WED, WED.minusDays(1), mon, lastFri))).isEqualTo(
            4
        );
    }

    @Test
    void neverLooksBeforeTheEarliestStartDate() {
        assertThat(StreakCalculator.count(WED, WED.minusDays(1), EVERY_DAY, EVERY_DAY)).isEqualTo(2);
        assertThat(StreakCalculator.count(WED, WED.plusDays(1), EVERY_DAY, EVERY_DAY)).isZero();
        assertThat(StreakCalculator.count(WED, null, EVERY_DAY, EVERY_DAY)).isZero();
    }

    @Test
    void lookbackIsCappedAtOneYear() {
        assertThat(StreakCalculator.count(WED, WED.minusYears(5), EVERY_DAY, EVERY_DAY)).isEqualTo(
            StreakCalculator.MAX_LOOKBACK_DAYS
        );
    }
}
