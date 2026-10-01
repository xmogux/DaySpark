package com.dayspark.app.domain;

import java.util.Locale;

public enum FocusStatus {
    RUNNING,
    PAUSED,
    COMPLETED,
    CANCELLED;

    public static FocusStatus parse(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("타이머 상태가 비어 있습니다.");
        try {
            return FocusStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("지원되지 않는 타이머 상태입니다.");
        }
    }
}
