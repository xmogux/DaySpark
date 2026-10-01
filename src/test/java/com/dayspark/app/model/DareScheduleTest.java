package com.dayspark.app.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Repeat rules of a challenge (FR-02) and the pause rule (FR-04), without Spring. */
class DareScheduleTest {

    private static final LocalDate MON = LocalDate.of(2026, 10, 5);
    private static final LocalDate SAT = MON.plusDays(5);

    private static Dare dare(String repeatType, String days) {
        return new Dare("t", "", "LIFE", 10, repeatType, days, MON.minusDays(7), null);
    }

    @Test
    void repeatTypesPlanTheExpectedWeekdays() {
        assertThat(dare("DAILY", "").isScheduledFor(SAT)).isTrue();
        assertThat(dare("WEEKDAYS", "").isScheduledFor(MON)).isTrue();
        assertThat(dare("WEEKDAYS", "").isScheduledFor(SAT)).isFalse();
        assertThat(dare("WEEKENDS", "").isScheduledFor(SAT)).isTrue();
        assertThat(dare("WEEKENDS", "").isScheduledFor(MON)).isFalse();
        assertThat(dare("CUSTOM", "MONDAY,FRIDAY").isScheduledFor(MON)).isTrue();
        assertThat(dare("CUSTOM", "MONDAY,FRIDAY").isScheduledFor(MON.plusDays(1))).isFalse();
    }

    @Test
    void nothingIsPlannedOutsideTheStartAndEndDates() {
        Dare d = new Dare("t", "", "LIFE", 10, "DAILY", "", MON, MON.plusDays(2));
        assertThat(d.isScheduledFor(MON.minusDays(1))).isFalse();
        assertThat(d.isScheduledFor(MON)).isTrue();
        assertThat(d.isScheduledFor(MON.plusDays(2))).isTrue();
        assertThat(d.isScheduledFor(MON.plusDays(3))).isFalse();
    }

    @Test
    void pauseAffectsOnlyTheDayItStartsAndLater() {
        Dare d = dare("DAILY", "");
        d.pauseFrom(MON);
        assertThat(d.isPausedOn(MON.minusDays(1))).isFalse();
        assertThat(d.isPausedOn(MON)).isTrue();
        d.resume();
        assertThat(d.isPausedOn(MON.plusDays(3))).isFalse();
    }

    @Test
    void customDaysAreShownInKoreanWeekOrder() {
        assertThat(dare("CUSTOM", "FRIDAY,MONDAY,WEDNESDAY").getRepeatLabel()).isEqualTo("월·수·금");
        assertThat(dare("WEEKDAYS", "").getRepeatLabel()).isEqualTo("평일");
    }

    @Test
    void reviewPlanIsStoredOnlyWhenEnabled() {
        Dare d = dare("DAILY", "");
        d.configureReview(true, null, "1,3,7");
        assertThat(d.getReviewBaseDate()).isEqualTo(d.getStartDate());
        assertThat(d.getReviewCount()).isEqualTo(3);
        d.configureReview(false, MON, "1,3,7");
        assertThat(d.getReviewBaseDate()).isNull();
        assertThat(d.getReviewIntervals()).isEmpty();
        assertThat(d.getReviewCount()).isZero();
    }
}
