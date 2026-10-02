package com.dayspark.app.service.view;

import com.dayspark.app.model.Completion;
import com.dayspark.app.model.Dare;
import com.dayspark.app.model.FocusSession;
import com.dayspark.app.service.ReviewService.ReviewItem;
import java.time.LocalDate;
import java.util.List;

/**
 * Calendar drill-down for a single date. {@code sessions} are the recorded focus sessions
 * (completed and ended early), {@code focusMinutes} their total and {@code completedRounds} how
 * many of them ran the full planned time.
 */
public record DayDetail(
    LocalDate date,
    List<Dare> planned,
    List<Completion> completed,
    List<ReviewItem> reviews,
    List<FocusSession> sessions,
    int focusMinutes,
    long completedRounds
) {}
