package com.dayspark.app;

import static org.assertj.core.api.Assertions.*;

import com.dayspark.app.model.Dare;
import com.dayspark.app.model.FocusSession;
import java.time.*;
import org.junit.jupiter.api.Test;

class FocusSessionUnitTests {

    @Test
    void pauseResumeAndCompletionRequireFullElapsedTime() {
        Dare d = new Dare("알고리즘", "", "STUDY", 25, "DAILY", "", LocalDate.of(2026, 1, 1), null);
        Instant t = Instant.parse("2026-09-01T00:00:00Z");
        FocusSession s = new FocusSession(d, 1, t);
        assertThat(s.remainingMillis(t.plusSeconds(20))).isEqualTo(40000);
        s.pause(t.plusSeconds(20));
        assertThat(s.remainingMillis(t.plusSeconds(60))).isEqualTo(40000);
        s.resume(t.plusSeconds(50));
        assertThat(s.remainingMillis(t.plusSeconds(80))).isEqualTo(10000);
        assertThatThrownBy(() -> s.complete(t.plusSeconds(80), ZoneOffset.UTC)).isInstanceOf(
            IllegalStateException.class
        );
        s.complete(t.plusSeconds(90), ZoneOffset.UTC);
        assertThat(s.getCompletedDay()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(s.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void cancellationNeverBecomesCompletion() {
        Dare d = new Dare("독서", "", "STUDY", 25, "DAILY", "", LocalDate.of(2026, 1, 1), null);
        Instant start = Instant.parse("2026-09-01T00:00:00Z");
        FocusSession s = new FocusSession(d, 1, start);
        s.cancel(start.plusSeconds(12));
        assertThat(s.getStatus()).isEqualTo("CANCELLED");
        assertThatThrownBy(() -> s.complete(start.plusSeconds(120), ZoneOffset.UTC)).isInstanceOf(
            IllegalStateException.class
        );
    }

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private static FocusSession session(int plannedMinutes, Instant start) {
        Dare d = new Dare("알고리즘", "", "STUDY", 25, "DAILY", "", LocalDate.of(2026, 1, 1), null);
        return new FocusSession(d, plannedMinutes, start);
    }

    @Test
    void stopKeepsTheTimeFocusedAndExcludesPauses() {
        Instant t = Instant.parse("2026-09-01T03:00:00Z"); // 12:00 Asia/Seoul
        FocusSession s = session(25, t);
        s.pause(t.plusSeconds(5 * 60));
        s.resume(t.plusSeconds(35 * 60));
        s.stop(t.plusSeconds(38 * 60), SEOUL);

        assertThat(s.getStatus()).isEqualTo("STOPPED");
        assertThat(s.isRecorded()).isTrue();
        assertThat(s.getFocusedMinutes()).isEqualTo(8);
        assertThat(s.getCompletedDay()).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    void stopWhilePausedIsRecordedOnTheDayTheFocusEnded() {
        Instant start = Instant.parse("2026-09-01T14:50:00Z"); // 23:50 Asia/Seoul
        FocusSession s = session(25, start);
        s.pause(start.plusSeconds(7 * 60)); // 23:57
        s.stop(start.plusSeconds(40 * 60), SEOUL); // 00:30 the next day, still paused

        assertThat(s.getFocusedMinutes()).isEqualTo(7);
        assertThat(s.getCompletedDay()).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    void focusedTimeNeverExceedsThePlannedTime() {
        Instant t = Instant.parse("2026-09-01T03:00:00Z");
        FocusSession s = session(1, t);
        assertThat(s.focusedMillis(t.plusSeconds(30))).isEqualTo(30_000);
        assertThat(s.focusedMillis(t.plusSeconds(300))).isEqualTo(60_000);
    }

    @Test
    void aStoppedSessionIsFinal() {
        Instant t = Instant.parse("2026-09-01T03:00:00Z");
        FocusSession s = session(25, t);
        s.stop(t.plusSeconds(10 * 60), SEOUL);

        assertThatThrownBy(() -> s.complete(t.plusSeconds(60 * 60), SEOUL)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> s.stop(t.plusSeconds(60 * 60), SEOUL)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> s.cancel(t.plusSeconds(60 * 60))).isInstanceOf(IllegalStateException.class);
        assertThat(s.getFocusedMinutes()).isEqualTo(10);
    }

    @Test
    void aCancelledSessionRecordsNothing() {
        Instant t = Instant.parse("2026-09-01T03:00:00Z");
        FocusSession s = session(25, t);
        s.cancel(t.plusSeconds(10 * 60));

        assertThat(s.isRecorded()).isFalse();
        assertThat(s.getRecordedMillis()).isZero();
        assertThat(s.getFocusedMinutes()).isZero();
    }

    @Test
    void completedDayUsesActualExpectedFinishTimeAcrossMidnight() {
        Dare d = new Dare("심야 공부", "", "STUDY", 25, "DAILY", "", LocalDate.of(2026, 1, 1), null);
        Instant start = Instant.parse("2026-09-01T14:50:00Z"); // 23:50 Asia/Seoul
        FocusSession s = new FocusSession(d, 25, start);
        assertThat(s.expectedCompletedDay(ZoneId.of("Asia/Seoul"))).isEqualTo(LocalDate.of(2026, 9, 2));
    }
}
