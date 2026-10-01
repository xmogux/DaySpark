package com.dayspark.app.service.dto;

import com.dayspark.app.model.Dare;
import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

/**
 * Everything the challenge form sends, for both "create" and "edit". Bound directly from the form
 * ({@code @ModelAttribute}); fields keep their defaults when a parameter is missing. Validation is
 * done by {@code DareService}, which owns the domain rules.
 */
public class DareRequest implements Serializable {

    private String title = "";
    private String description = "";
    private String category = "LIFE";
    private int minutes = 10;

    private String repeatType = "DAILY";
    private List<String> repeatDays = new ArrayList<>();

    @DateTimeFormat(iso = ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = ISO.DATE)
    private LocalDate endDate;

    private int targetValue = 1;
    private String targetUnit = "회";
    private int weeklyTarget = 0;

    private boolean reviewEnabled = false;

    @DateTimeFormat(iso = ISO.DATE)
    private LocalDate reviewBaseDate;

    private String reviewIntervals = "1,3,7,14";

    /** Defaults for a new challenge that starts today. */
    public static DareRequest startingOn(LocalDate today) {
        DareRequest request = new DareRequest();
        request.startDate = today;
        request.reviewBaseDate = today;
        return request;
    }

    /** Pre-fills the edit form from an existing challenge. */
    public static DareRequest from(Dare dare) {
        DareRequest request = new DareRequest();
        request.title = dare.getTitle();
        request.description = dare.getDescription();
        request.category = dare.getCategory();
        request.minutes = dare.getMinutes();
        request.repeatType = dare.getRepeatType();
        request.repeatDays = dare.getRepeatDaySet().stream().sorted().map(DayOfWeek::name).toList();
        request.startDate = dare.getStartDate();
        request.endDate = dare.getEndDate();
        request.targetValue = dare.getTargetValue();
        request.targetUnit = dare.getTargetUnit();
        request.weeklyTarget = dare.getWeeklyTarget();
        request.reviewEnabled = dare.isReviewEnabled();
        request.reviewBaseDate = dare.getReviewBaseDate() != null ? dare.getReviewBaseDate() : dare.getStartDate();
        if (dare.isReviewEnabled()) request.reviewIntervals = dare.getReviewIntervals();
        return request;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public String getRepeatType() {
        return repeatType;
    }

    public void setRepeatType(String repeatType) {
        this.repeatType = repeatType;
    }

    public List<String> getRepeatDays() {
        return repeatDays;
    }

    public void setRepeatDays(List<String> repeatDays) {
        this.repeatDays = repeatDays == null ? new ArrayList<>() : repeatDays;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(int targetValue) {
        this.targetValue = targetValue;
    }

    public String getTargetUnit() {
        return targetUnit;
    }

    public void setTargetUnit(String targetUnit) {
        this.targetUnit = targetUnit;
    }

    public int getWeeklyTarget() {
        return weeklyTarget;
    }

    public void setWeeklyTarget(int weeklyTarget) {
        this.weeklyTarget = weeklyTarget;
    }

    public boolean isReviewEnabled() {
        return reviewEnabled;
    }

    public void setReviewEnabled(boolean reviewEnabled) {
        this.reviewEnabled = reviewEnabled;
    }

    public LocalDate getReviewBaseDate() {
        return reviewBaseDate;
    }

    public void setReviewBaseDate(LocalDate reviewBaseDate) {
        this.reviewBaseDate = reviewBaseDate;
    }

    public String getReviewIntervals() {
        return reviewIntervals;
    }

    public void setReviewIntervals(String reviewIntervals) {
        this.reviewIntervals = reviewIntervals;
    }
}
