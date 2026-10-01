package com.dayspark.app.domain;

import java.util.Locale;

public enum DareCategory {
    STUDY,
    HEALTH,
    MOVE,
    LIFE,
    WORK;

    public static DareCategory parse(String value) {
        if (value == null || value.isBlank()) return LIFE;
        try {
            return DareCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("지원되지 않는 카테고리입니다.");
        }
    }
}
