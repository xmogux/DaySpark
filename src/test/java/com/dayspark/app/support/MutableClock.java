package com.dayspark.app.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * A {@link Clock} whose current instant is controlled by the test.
 *
 * <p>Integration tests use it so that "today" is always the same date, and focus-timer tests can
 * move time forward with {@link #advance(Duration)} instead of sleeping.
 */
public final class MutableClock extends Clock {
    private final ZoneId zone;
    private volatile Instant instant;

    public MutableClock(Instant instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    public void advance(Duration duration) {
        this.instant = instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId otherZone) {
        return Clock.fixed(instant, otherZone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
