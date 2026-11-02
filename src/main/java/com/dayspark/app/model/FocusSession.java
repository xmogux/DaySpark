package com.dayspark.app.model;

import com.dayspark.app.domain.FocusStatus;
import jakarta.persistence.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Entity
@Table(name = "focus_sessions")
public class FocusSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "dare_id", nullable = false)
    private Dare dare;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant pausedAt;
    private Instant endedAt;

    @Column(nullable = false)
    private long pausedMillis;

    @Column(nullable = false)
    private int plannedMinutes;

    @Column(nullable = false, length = 12)
    private String status = FocusStatus.RUNNING.name();

    /** Day the session is recorded on: when the planned time ran out (COMPLETED) or the focus ended (STOPPED). */
    private LocalDate completedDay;

    protected FocusSession() {}

    public FocusSession(Dare dare, int plannedMinutes, Instant now) {
        this.dare = dare;
        this.plannedMinutes = plannedMinutes;
        this.startedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Dare getDare() {
        return dare;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getPausedAt() {
        return pausedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public long getPausedMillis() {
        return pausedMillis;
    }

    public int getPlannedMinutes() {
        return plannedMinutes;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getCompletedDay() {
        return completedDay;
    }

    public long elapsedMillis(Instant now) {
        Instant stop = FocusStatus.PAUSED.name().equals(status) ? pausedAt : endedAt != null ? endedAt : now;
        return Math.max(0, Duration.between(startedAt, stop).toMillis() - pausedMillis);
    }

    public long remainingMillis(Instant now) {
        return Math.max(0, plannedMillis() - elapsedMillis(now));
    }

    private long plannedMillis() {
        return plannedMinutes * 60_000L;
    }

    /**
     * Time actually focused: wall-clock time minus pauses, never more than the planned time.
     * A completed session counts its full planned time; a cancelled one counts nothing.
     */
    public long focusedMillis(Instant now) {
        return switch (FocusStatus.parse(status)) {
            case COMPLETED -> plannedMillis();
            case CANCELLED -> 0;
            case STOPPED, RUNNING, PAUSED -> Math.min(plannedMillis(), elapsedMillis(now));
        };
    }

    /** Focus time that counts in the statistics: only for COMPLETED and STOPPED sessions. */
    public long getRecordedMillis() {
        return isRecorded() ? focusedMillis(endedAt) : 0;
    }

    /** Recorded focus time in whole minutes (for display). */
    public int getFocusedMinutes() {
        return (int) (getRecordedMillis() / 60_000);
    }

    public boolean isRecorded() {
        return FocusStatus.parse(status).isRecorded();
    }

    public Instant expectedFinishedAt() {
        return startedAt.plusMillis(pausedMillis).plus(Duration.ofMinutes(plannedMinutes));
    }

    public LocalDate expectedCompletedDay(ZoneId zoneId) {
        return expectedFinishedAt().atZone(zoneId).toLocalDate();
    }

    public void pause(Instant now) {
        if (!FocusStatus.RUNNING.name().equals(status)) throw new IllegalStateException(
            "진행 중인 타이머만 일시정지할 수 있습니다."
        );
        pausedAt = now;
        status = FocusStatus.PAUSED.name();
    }

    public void resume(Instant now) {
        if (!FocusStatus.PAUSED.name().equals(status)) throw new IllegalStateException(
            "정지된 타이머만 재개할 수 있습니다."
        );
        pausedMillis += Math.max(0, Duration.between(pausedAt, now).toMillis());
        pausedAt = null;
        status = FocusStatus.RUNNING.name();
    }

    /**
     * Finishes the session. The completed day is the local date on which the planned time ran out
     * (not the moment the browser called finish), computed after any pause is closed so a session
     * started at 23:50 for 25 minutes belongs to the next day.
     */
    public void complete(Instant now, ZoneId zone) {
        if (!FocusStatus.RUNNING.name().equals(status) && !FocusStatus.PAUSED.name().equals(status)) {
            throw new IllegalStateException("이미 끝난 타이머입니다.");
        }
        // Validate before resuming: a rejected completion must not change a paused session.
        if (remainingMillis(now) > 0) throw new IllegalStateException("집중 시간이 아직 끝나지 않았습니다.");
        if (FocusStatus.PAUSED.name().equals(status)) resume(now);
        endedAt = now;
        completedDay = expectedCompletedDay(zone);
        status = FocusStatus.COMPLETED.name();
    }

    /**
     * Ends the session before the planned time ran out ("종료 및 기록"). The time focused so far is
     * kept and time spent paused is not. The session is recorded on the day the focus actually
     * ended: the start of the pause if it was paused, otherwise now. It is not a completed round.
     */
    public void stop(Instant now, ZoneId zone) {
        boolean paused = FocusStatus.PAUSED.name().equals(status);
        if (!paused && !FocusStatus.RUNNING.name().equals(status)) {
            throw new IllegalStateException("이미 끝난 타이머입니다.");
        }
        Instant focusEnd = paused ? pausedAt : now;
        if (paused) resume(now);
        endedAt = now;
        completedDay = focusEnd.atZone(zone).toLocalDate();
        status = FocusStatus.STOPPED.name();
    }

    public void cancel(Instant now) {
        if (isRecorded()) throw new IllegalStateException("이미 기록된 집중은 취소할 수 없습니다.");
        status = FocusStatus.CANCELLED.name();
        endedAt = now;
    }
}
