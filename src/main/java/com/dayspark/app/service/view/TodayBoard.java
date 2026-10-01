package com.dayspark.app.service.view;

import java.util.Map;

/** Everything the home screen shows about today's challenges. Maps are keyed by challenge id. */
public record TodayBoard(DayPlan plan, Map<Long, Integer> progressToday, Map<Long, Long> completionsThisWeek, int streak) {}
