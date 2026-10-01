package com.dayspark.app.service.view;

import com.dayspark.app.model.Completion;
import com.dayspark.app.service.ReviewService.ReviewItem;
import java.util.List;

/** Numbers shown on one challenge's detail page. */
public record DareInsights(
        int progressToday,
        long weekDone,
        long monthDone,
        long monthRate,
        int goalStreak,
        long weekFocusMinutes,
        long monthFocusMinutes,
        int focusCount,
        List<ReviewItem> reviewPlan,
        long reviewDone,
        List<Completion> recentCompletions) {}
