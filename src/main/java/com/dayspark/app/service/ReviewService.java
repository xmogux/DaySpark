package com.dayspark.app.service;

import com.dayspark.app.domain.ReviewIntervals;
import com.dayspark.app.model.Dare;
import com.dayspark.app.model.ReviewRecord;
import com.dayspark.app.repository.DareRepository;
import com.dayspark.app.repository.ReviewRecordRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public List<ReviewItem> planFor(Dare dare) {
        if (!dare.isReviewEnabled() || !"STUDY".equals(dare.getCategory())) return List.of();
        LocalDate base = dare.getReviewBaseDate() == null ? dare.getStartDate() : dare.getReviewBaseDate();
        List<Integer> offsets = ReviewIntervals.ofStored(dare.getReviewIntervals()).offsets();
        Map<Integer, ReviewRecord> done = new HashMap<>();
        records.findByDareIdOrderByOffsetDaysAsc(dare.getId()).forEach(r -> done.put(r.getOffsetDays(), r));

        List<ReviewItem> result = new ArrayList<>();
        for (int i = 0; i < offsets.size(); i++) {
            int offset = offsets.get(i);
            ReviewRecord record = done.get(offset);
            result.add(
                new ReviewItem(
                    dare,
                    i + 1,
                    offset,
                    base.plusDays(offset),
                    record != null,
                    record == null ? null : record.getCompletedAt()
                )
            );
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
                if (
                    !item.dueDate().isBefore(start) && !item.dueDate().isAfter(end) && !dare.isPausedOn(item.dueDate())
                ) result.add(item);
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
        if (!ReviewIntervals.ofStored(dare.getReviewIntervals()).contains(offsetDays)) {
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

    public record ReviewItem(
        Dare dare,
        int number,
        int offsetDays,
        LocalDate dueDate,
        boolean completed,
        LocalDateTime completedAt
    ) {
        public boolean isOverdue(LocalDate today) {
            return !completed && dueDate.isBefore(today);
        }

        public boolean isToday(LocalDate today) {
            return !completed && dueDate.equals(today);
        }

        public long daysOverdue(LocalDate today) {
            return isOverdue(today) ? java.time.temporal.ChronoUnit.DAYS.between(dueDate, today) : 0;
        }
    }
}
