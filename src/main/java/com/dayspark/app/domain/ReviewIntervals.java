package com.dayspark.app.domain;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Review offsets in days after the review base date, e.g. "1,3,7". Immutable, sorted, no duplicates.
 *
 * <p>Single place for parsing user input ({@link #parse}) and reading the stored value
 * ({@link #ofStored}); previously the same parsing code lived in two services.
 */
public final class ReviewIntervals {
    public static final int MIN_DAYS = 1;
    public static final int MAX_DAYS = 3650;
    public static final int MAX_COUNT = 12;

    private static final ReviewIntervals NONE = new ReviewIntervals(List.of());

    private final List<Integer> offsets;

    private ReviewIntervals(List<Integer> offsets) {
        this.offsets = List.copyOf(offsets);
    }

    /**
     * Parses user input such as "7, 1, 3, 7" or "1 3 7" (full-width commas allowed).
     *
     * @throws IllegalArgumentException with a message for the user when the input is invalid
     */
    public static ReviewIntervals parse(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("복습 간격을 하나 이상 입력해주세요.");
        SortedSet<Integer> values = new TreeSet<>();
        for (String part : raw.trim().replace('，', ',').split("[,\\s]+")) {
            if (part.isBlank()) continue;
            int days;
            try {
                days = Integer.parseInt(part);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("복습 간격은 1, 3, 7처럼 날짜 수(숫자)로 입력해주세요.");
            }
            if (days < MIN_DAYS || days > MAX_DAYS) {
                throw new IllegalArgumentException("복습 간격은 1일~3650일 사이로 설정해주세요.");
            }
            values.add(days);
        }
        if (values.isEmpty()) throw new IllegalArgumentException("복습 간격을 하나 이상 입력해주세요.");
        if (values.size() > MAX_COUNT) throw new IllegalArgumentException("복습 일정은 최대 12개까지 만들 수 있습니다.");
        return new ReviewIntervals(List.copyOf(values));
    }

    /** Reads the normalized value stored in the database; empty or null means "no review plan". */
    public static ReviewIntervals ofStored(String stored) {
        return stored == null || stored.isBlank() ? NONE : parse(stored);
    }

    public List<Integer> offsets() {
        return offsets;
    }

    public int size() {
        return offsets.size();
    }

    public boolean contains(int offsetDays) {
        return offsets.contains(offsetDays);
    }

    /** Storage form, e.g. "1,3,7". */
    public String normalized() {
        return offsets.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    @Override
    public String toString() {
        return normalized();
    }
}
