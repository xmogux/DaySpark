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

    private LocalDate completedDay;

    protected FocusSession() {}

    public FocusSession(Dare dare, int plannedMinutes, Instant now) {
        this.dare = dare;
        this.plannedMinutes = plannedMinutes;
        this.startedAt = now;
    }

    public Long getId() { return id; }
    public Dare getDare() { return dare; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getPausedAt() { return pausedAt; }
    public Instant getEndedAt() { return endedAt; }
    public long getPausedMillis() { return pausedMillis; }
    public int getPlannedMinutes() { return plannedMinutes; }
    public String getStatus() { return status; }
    public LocalDate getCompletedDay() { return completedDay; }

    public long elapsedMillis(Instant now) {
        Instant stop = FocusStatus.PAUSED.name().equals(status) ? pausedAt : (endedAt != null ? endedAt : now);
        return Math.max(0, Duration.between(startedAt, stop).toMillis() - pausedMillis);
    }

    public long remainingMillis(Instant now) {
        return Math.max(0, plannedMinutes * 60_000L - elapsedMillis(now));
    }

    public Instant expectedFinishedAt() {
        return startedAt.plusMillis(pausedMillis).plus(Duration.ofMinutes(plannedMinutes));
    }

    public LocalDate expectedCompletedDay(ZoneId zoneId) {
        return expectedFinishedAt().atZone(zoneId).toLocalDate();
    }

    public void pause(Instant now) {
        if (!FocusStatus.RUNNING.name().equals(status)) throw new IllegalStateException("진행 중인 타이머만 일시정지할 수 있습니다.");
        pausedAt = now;
        status = FocusStatus.PAUSED.name();
    }

    public void resume(Instant now) {
        if (!FocusStatus.PAUSED.name().equals(status)) throw new IllegalStateException("정지된 타이머만 재개할 수 있습니다.");
        pausedMillis += Math.max(0, Duration.between(pausedAt, now).toMillis());
        pausedAt = null;
        status = FocusStatus.RUNNING.name();
    }

    public void complete(Instant now, LocalDate day) {
        if (FocusStatus.PAUSED.name().equals(status)) resume(now);
        if (!FocusStatus.RUNNING.name().equals(status)) throw new IllegalStateException("이미 끝난 타이머입니다.");
        if (remainingMillis(now) > 0) throw new IllegalStateException("집중 시간이 아직 끝나지 않았습니다.");
        endedAt = now;
        completedDay = day;
        status = FocusStatus.COMPLETED.name();
    }

    public void cancel(Instant now) {
        if (FocusStatus.COMPLETED.name().equals(status)) throw new IllegalStateException("완료된 기록은 취소할 수 없습니다.");
        status = FocusStatus.CANCELLED.name();
        endedAt = now;
    }
}
