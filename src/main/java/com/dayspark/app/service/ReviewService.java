package com.dayspark.app.service;

import com.dayspark.app.model.Dare;
import com.dayspark.app.model.ReviewRecord;
import com.dayspark.app.repository.DareRepository;
import com.dayspark.app.repository.ReviewRecordRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ReviewService {
    private final ReviewRecordRepository records;
    private final DareRepository dares;
    private final Clock clock;

    public ReviewService(ReviewRecordRepository records, DareRepository dares, Clock clock) {
        this.records = records;
        this.dares = dares;
        this.clock = clock;
    }

    public List<Integer> parseIntervals(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        String normalized = raw.trim().replace('，', ',');
        String[] parts = normalized.split("[,\\s]+");
        SortedSet<Integer> values = new TreeSet<>();
        for (String part : parts) {
            if (part.isBlank()) continue;
            final int days;
            try {
                days = Integer.parseInt(part);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("복습 간격은 1, 3, 7처럼 날짜 수(숫자)로 입력해주세요.");
            }
            if (days < 1 || days > 3650) {
                throw new IllegalArgumentException("복습 간격은 1일~3650일 사이로 설정해주세요.");
            }
            values.add(days);
        }
        if (values.isEmpty()) throw new IllegalArgumentException("복습 간격을 하나 이상 입력해주세요.");
        if (values.size() > 12) throw new IllegalArgumentException("복습 일정은 최대 12개까지 만들 수 있습니다.");
        return List.copyOf(values);
    }

    public String normalizeIntervals(String raw) {
        return parseIntervals(raw).stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
    }

    public List<ReviewItem> planFor(Dare dare) {
        if (!dare.isReviewEnabled() || !"STUDY".equals(dare.getCategory())) return List.of();
        LocalDate base = dare.getReviewBaseDate() == null ? dare.getStartDate() : dare.getReviewBaseDate();
        List<Integer> offsets = parseIntervals(dare.getReviewIntervals());
        Map<Integer, ReviewRecord> done = new HashMap<>();
        records.findByDareIdOrderByOffsetDaysAsc(dare.getId()).forEach(r -> done.put(r.getOffsetDays(), r));

        List<ReviewItem> result = new ArrayList<>();
        for (int i = 0; i < offsets.size(); i++) {
            int offset = offsets.get(i);
            ReviewRecord record = done.get(offset);
            result.add(new ReviewItem(dare, i + 1, offset, base.plusDays(offset), record != null,
                    record == null ? null : record.getCompletedAt()));
        }
        return result;
    }

    public List<ReviewItem> forDate(LocalDate date) {
        List<ReviewItem> result = new ArrayList<>();
        for (Dare dare : dares.findAllByOrderByActiveDescIdAsc()) {
            if (!dare.isReviewEnabled() || !"STUDY".equals(dare.getCategory())) continue;
            for (ReviewItem item : planFor(dare)) {
                if (item.dueDate().equals(date) && !dare.isPausedOn(item.dueDate())) result.add(item);
            }
        }
        return result;
    }

    /**
     * Reviews to show on the home screen for {@code today}: everything due today (done or not) plus
     * every earlier review that is still not done. Overdue reviews stay until they are completed.
     */
    public List<ReviewItem> dueOn(LocalDate today) {
        List<ReviewItem> result = new ArrayList<>();
        for (Dare dare : dares.findAllByOrderByActiveDescIdAsc()) {
            if (!dare.isReviewEnabled() || !"STUDY".equals(dare.getCategory())) continue;
            for (ReviewItem item : planFor(dare)) {
                boolean due = item.dueDate().equals(today) || item.isOverdue(today);
                if (due && !dare.isPausedOn(item.dueDate())) result.add(item);
            }
        }
        result.sort(Comparator.comparing(ReviewItem::dueDate).thenComparing(i -> i.dare().getId()));
        return result;
    }

    public List<ReviewItem> between(LocalDate start, LocalDate end) {
        List<ReviewItem> result = new ArrayList<>();
        for (Dare dare : dares.findAllByOrderByActiveDescIdAsc()) {
            if (!dare.isReviewEnabled() || !"STUDY".equals(dare.getCategory())) continue;
            for (ReviewItem item : planFor(dare)) {
                if (!item.dueDate().isBefore(start) && !item.dueDate().isAfter(end) && !dare.isPausedOn(item.dueDate())) result.add(item);
            }
        }
        result.sort(Comparator.comparing(ReviewItem::dueDate).thenComparing(i -> i.dare().getId()));
        return result;
    }

    @Transactional
    public boolean toggle(Dare dare, int offsetDays) {
        if (!dare.isReviewEnabled() || !"STUDY".equals(dare.getCategory())) {
            throw new IllegalArgumentException("복습 플랜이 설정된 공부 도전이 아닙니다.");
        }
        if (!parseIntervals(dare.getReviewIntervals()).contains(offsetDays)) {
            throw new IllegalArgumentException("현재 복습 플랜에 없는 일정입니다.");
        }
        Optional<ReviewRecord> existing = records.findByDareIdAndOffsetDays(dare.getId(), offsetDays);
        if (existing.isPresent()) {
            records.delete(existing.get());
            return false;
        }
        records.save(new ReviewRecord(dare.getId(), offsetDays, LocalDateTime.now(clock)));
        return true;
    }

    @Transactional
    public void clearForDare(Long dareId) {
        records.deleteAllByDareId(dareId);
    }

    public record ReviewItem(Dare dare, int number, int offsetDays, LocalDate dueDate,
                             boolean completed, LocalDateTime completedAt) {
        public boolean isOverdue(LocalDate today) { return !completed && dueDate.isBefore(today); }
        public boolean isToday(LocalDate today) { return !completed && dueDate.equals(today); }
        public long daysOverdue(LocalDate today) { return isOverdue(today) ? java.time.temporal.ChronoUnit.DAYS.between(dueDate, today) : 0; }
    }
}
