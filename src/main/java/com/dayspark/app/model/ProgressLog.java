package com.dayspark.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** One amount added towards a challenge's numeric daily goal, either typed in or earned by a focus session. */
@Entity
@Table(name = "progress_logs")
public class ProgressLog {
    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_FOCUS = "FOCUS";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "dare_id", nullable = false)
    private Dare dare;

    @Column(nullable = false, name = "progress_date")
    private LocalDate day;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false, length = 12)
    private String source;

    @Column(nullable = false)
    private LocalDateTime loggedAt;

    protected ProgressLog() {}

    public ProgressLog(Dare dare, LocalDate day, int amount, String source, LocalDateTime loggedAt) {
        this.dare = dare;
        this.day = day;
        this.amount = amount;
        this.source = source;
        this.loggedAt = loggedAt;
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

    public int getAmount() {
        return amount;
    }

    public String getSource() {
        return source;
    }

    public LocalDateTime getLoggedAt() {
        return loggedAt;
    }
}
