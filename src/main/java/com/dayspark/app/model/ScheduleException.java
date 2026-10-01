package com.dayspark.app.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "schedule_exceptions",
    uniqueConstraints = @UniqueConstraint(columnNames = { "dare_id", "exception_date" })
)
public class ScheduleException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "dare_id", nullable = false)
    private Dare dare;

    @Column(nullable = false, name = "exception_date")
    private LocalDate day;

    @Column(nullable = false, length = 8)
    private String mode; // SKIP or ADD

    @Column(length = 120)
    private String note;

    protected ScheduleException() {}

    public ScheduleException(Dare dare, LocalDate day, String mode, String note) {
        this.dare = dare;
        this.day = day;
        this.mode = mode;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public Dare getDare() {
        return dare;
    }

    public LocalDate getDay() {
        return day;
    }

    public String getMode() {
        return mode;
    }

    public String getNote() {
        return note;
    }

    public void update(String mode, String note) {
        this.mode = mode;
        this.note = note;
    }
}
