package com.dayspark.app.service.view;

import java.time.LocalDate;

/** One cell of the month calendar. {@code completed <= planned} always holds. */
public record CalendarDay(
        LocalDate date,
        boolean inMonth,
        long completed,
        long planned,
        int percentage,
        boolean today,
        boolean future,
        long reviewPlanned,
        long reviewCompleted) {}
