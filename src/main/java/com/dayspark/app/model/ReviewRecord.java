package com.dayspark.app.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_records", uniqueConstraints = @UniqueConstraint(columnNames = {"dare_id", "offset_days"}))
public class ReviewRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dare_id", nullable = false)
    private Long dareId;

    @Column(name = "offset_days", nullable = false)
    private int offsetDays;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    protected ReviewRecord() {}

    public ReviewRecord(Long dareId, int offsetDays, LocalDateTime completedAt) {
        this.dareId = dareId;
        this.offsetDays = offsetDays;
        this.completedAt = completedAt;
    }

    public Long getId() { return id; }
    public Long getDareId() { return dareId; }
    public int getOffsetDays() { return offsetDays; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
