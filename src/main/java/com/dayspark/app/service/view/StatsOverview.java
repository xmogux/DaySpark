package com.dayspark.app.service.view;

import java.util.List;
import java.util.Map;

/** Statistics page: rates for this week and month, charts and focus totals. Rates are 0-100. */
public record StatsOverview(
        int streak,
        long weekDone,
        long monthDone,
        long weekRate,
        long monthRate,
        String topCategory,
        List<DailyStat> weekBars,
        List<CategoryStat> categoryStats,
        long focusToday,
        long focusWeek,
        long focusMonth,
        int focusWeekCount,
        int focusMonthCount,
        Map<String, Long> focusPerCategory) {}
