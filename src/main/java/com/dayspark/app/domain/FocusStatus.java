package com.dayspark.app.domain;

import java.util.Locale;

/**
 * Life cycle of a focus session. RUNNING and PAUSED are active; the others are final.
 * COMPLETED and STOPPED are "recorded": their focus time counts in the statistics.
 */
public enum FocusStatus {
    RUNNING,
    PAUSED,
    /** The full planned time was focused: counts as a completed round and as goal progress. */
    COMPLETED,
    /** Discarded without recording (sessions cancelled before v5.7, and the API's /cancel). */
    CANCELLED,
    /** Ended early by the user ("종료 및 기록"): the focused time counts, the round does not. */
    STOPPED;

    public boolean isActive() {
        return this == RUNNING || this == PAUSED;
    }

    /** Whether the session's focus time is part of the statistics. */
    public boolean isRecorded() {
        return this == COMPLETED || this == STOPPED;
    }

    public static FocusStatus parse(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("타이머 상태가 비어 있습니다.");
        try {
            return FocusStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("지원되지 않는 타이머 상태입니다.");
        }
    }
}
