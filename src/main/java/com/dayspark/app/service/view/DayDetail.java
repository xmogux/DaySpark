package com.dayspark.app.service.view;

import com.dayspark.app.model.Completion;
import com.dayspark.app.model.Dare;
import com.dayspark.app.model.FocusSession;
import com.dayspark.app.service.ReviewService.ReviewItem;
import java.time.LocalDate;
import java.util.List;

/** Calendar drill-down for a single date. */
public record DayDetail(
    LocalDate date,
    List<Dare> planned,
    List<Completion> completed,
    List<ReviewItem> reviews,
    List<FocusSession> sessions,
    int focusMinutes
) {}
