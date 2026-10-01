package com.dayspark.app.domain;

import java.util.Locale;

public enum ScheduleMode {
    SKIP,
    ADD,
    RESET;

    public static ScheduleMode parse(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("일정 변경 방식을 선택해주세요.");
        try {
            return ScheduleMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("올바르지 않은 일정 옵션입니다.");
        }
    }
}
