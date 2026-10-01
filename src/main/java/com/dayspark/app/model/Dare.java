package com.dayspark.app.model;

import com.dayspark.app.domain.DareCategory;
import com.dayspark.app.domain.RepeatType;
import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "dares")
public class Dare {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false, length = 220)
    private String description;

    @Column(nullable = false, length = 32)
    private String category;

    @Column(nullable = false)
    private int minutes;

    @Column(nullable = false, length = 20)
    private String repeatType = RepeatType.DAILY.name();

    @Column(length = 100)
    private String repeatDays = "";

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false)
    private boolean active = true;

    // If a challenge is paused, dates before this value still count for historical stats.
    private LocalDate pausedFrom;

    @Column(nullable = false, columnDefinition = "integer default 1")
    private int targetValue = 1;

    @Column(nullable = false, length = 12, columnDefinition = "varchar(12) default '회'")
    private String targetUnit = "회";

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int weeklyTarget = 0;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean reviewEnabled = false;

    private LocalDate reviewBaseDate;

    @Column(nullable = false, length = 120, columnDefinition = "varchar(120) default ''")
    private String reviewIntervals = "";

    protected Dare() {}

    public Dare(String title, String description, String category, int minutes,
                String repeatType, String repeatDays, LocalDate startDate, LocalDate endDate) {
        update(title, description, category, minutes, repeatType, repeatDays, startDate, endDate);
        this.active = true;
        this.pausedFrom = null;
    }

    public void update(String title, String description, String category, int minutes,
                       String repeatType, String repeatDays, LocalDate startDate, LocalDate endDate) {
        this.title = title.trim();
        this.description = description == null ? "" : description.trim();
        this.category = DareCategory.parse(category).name();
        this.minutes = Math.max(1, minutes);
        this.repeatType = RepeatType.parse(repeatType).name();
        this.repeatDays = repeatDays == null ? "" : repeatDays.trim();
        this.startDate = java.util.Objects.requireNonNull(startDate, "startDate");
        this.endDate = endDate;
    }

    public boolean isWithinDateRange(LocalDate date) {
        return !date.isBefore(startDate) && (endDate == null || !date.isAfter(endDate));
    }

    public boolean isPausedOn(LocalDate date) {
        return !active && pausedFrom != null && !date.isBefore(pausedFrom);
    }

    public boolean isScheduledFor(LocalDate date) {
        if (!isWithinDateRange(date)) return false;
        DayOfWeek day = date.getDayOfWeek();
        return switch (RepeatType.parse(repeatType)) {
            case WEEKDAYS -> day.getValue() <= 5;
            case WEEKENDS -> day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
            case CUSTOM -> getRepeatDaySet().contains(day);
            case DAILY -> true;
        };
    }

    public Set<DayOfWeek> getRepeatDaySet() {
        if (repeatDays == null || repeatDays.isBlank()) return Set.of();
        return Arrays.stream(repeatDays.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toUnmodifiableSet());
    }

    public String getRepeatLabel() {
        return switch (RepeatType.parse(repeatType)) {
            case WEEKDAYS -> "평일";
            case WEEKENDS -> "주말";
            case CUSTOM -> customDaysKorean();
            case DAILY -> "매일";
        };
    }

    private String customDaysKorean() {
        Set<DayOfWeek> days = getRepeatDaySet();
        if (days.isEmpty()) return "요일 미지정";
        String[] ko = {"월", "화", "수", "목", "금", "토", "일"};
        return days.stream().sorted().map(d -> ko[d.getValue() - 1]).collect(Collectors.joining("·"));
    }

    public void setGoals(int targetValue, String targetUnit, int weeklyTarget) {
        this.targetValue = targetValue;
        this.targetUnit = targetUnit;
        this.weeklyTarget = weeklyTarget;
    }


    public void configureReview(boolean enabled, LocalDate baseDate, String intervals) {
        this.reviewEnabled = enabled;
        this.reviewBaseDate = enabled ? (baseDate == null ? this.startDate : baseDate) : null;
        this.reviewIntervals = enabled && intervals != null ? intervals : "";
    }

    public void pauseFrom(LocalDate day) {
        this.active = false;
        this.pausedFrom = day;
    }

    public void resume() {
        this.active = true;
        this.pausedFrom = null;
    }

    public int getTargetValue() { return targetValue; }
    public String getTargetUnit() { return targetUnit; }
    public int getWeeklyTarget() { return weeklyTarget; }

    public boolean isReviewEnabled() { return reviewEnabled; }
    public LocalDate getReviewBaseDate() { return reviewBaseDate; }
    public String getReviewIntervals() { return reviewIntervals; }
    public int getReviewCount() {
        if (!reviewEnabled || reviewIntervals == null || reviewIntervals.isBlank()) return 0;
        return (int) Arrays.stream(reviewIntervals.split(",")).map(String::trim).filter(v -> !v.isBlank()).count();
    }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public int getMinutes() { return minutes; }
    public String getRepeatType() { return repeatType; }
    public String getRepeatDays() { return repeatDays; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public boolean isActive() { return active; }
    public LocalDate getPausedFrom() { return pausedFrom; }

    /**
     * Kept for JPA/tests. Prefer pauseFrom(day) or resume() so historical stats stay stable.
     */
    public void setActive(boolean active) {
        this.active = active;
        if (active) this.pausedFrom = null;
    }
}
