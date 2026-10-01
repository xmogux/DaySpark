package com.dayspark.app.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "completions", uniqueConstraints = @UniqueConstraint(columnNames = {"dare_id", "challenge_date"}))
public class Completion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "dare_id", nullable = false)
    private Dare dare;

    @Column(name = "challenge_date", nullable = false)
    private LocalDate challengeDate;

    @Column(length = 300)
    private String note;

    @Column(length = 255)
    private String imagePath;

    @Column(nullable = false)
    private LocalDateTime completedAt;

    protected Completion() {}

    public Completion(Dare dare, LocalDate challengeDate, String note, String imagePath, LocalDateTime completedAt) {
        this.dare = dare;
        this.challengeDate = challengeDate;
        this.note = note;
        this.imagePath = imagePath;
        this.completedAt = completedAt;
    }

    public Long getId() { return id; }
    public Dare getDare() { return dare; }
    public LocalDate getChallengeDate() { return challengeDate; }
    public String getNote() { return note; }
    public String getImagePath() { return imagePath; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
