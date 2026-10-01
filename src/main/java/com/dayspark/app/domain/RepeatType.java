package com.dayspark.app.domain;

import java.util.Locale;

public enum RepeatType {
    DAILY,
    WEEKDAYS,
    WEEKENDS,
    CUSTOM;

    public static RepeatType parse(String value) {
        if (value == null || value.isBlank()) return DAILY;
        try {
            return RepeatType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("지원되지 않는 반복 방식입니다.");
        }
    }
}
