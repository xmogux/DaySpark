package com.dayspark.app.support;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/** The fixed calendar every test runs in. 2026-10-07 is a Wednesday. */
public final class TestDates {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    public static final LocalDate TODAY = LocalDate.of(2026, 10, 7);
    public static final Instant TODAY_NOON = TODAY.atTime(LocalTime.NOON).atZone(ZONE).toInstant();

    private TestDates() {}

    public static Instant at(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute).atZone(ZONE).toInstant();
    }
}
