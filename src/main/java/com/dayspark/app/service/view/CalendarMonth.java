package com.dayspark.app.service.view;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/** Month calendar grid (Monday-first, whole weeks) plus the month summary cards. */
public record CalendarMonth(
        YearMonth month,
        List<CalendarDay> days,
        Map<LocalDate, Long> focusMinutesByDay,
        long completionCount,
        long plannedCount,
        long rate,
        boolean hasElapsed,
        String summaryNote,
        long focusMinutes,
        long focusRounds) {}
