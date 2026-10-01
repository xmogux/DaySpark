package com.dayspark.app.service;

import com.dayspark.app.model.Completion;
import com.dayspark.app.model.Dare;
import com.dayspark.app.repository.CompletionRepository;
import com.dayspark.app.repository.DareRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CompletionService {
    private final CompletionRepository completionRepository;
    private final DareRepository dareRepository;
    private final PlanningService planning;
    private final ImageStorage images;
    private final Clock clock;

    public CompletionService(CompletionRepository completionRepository, DareRepository dareRepository,
                             PlanningService planning, ImageStorage images, Clock clock) {
        this.completionRepository = completionRepository;
        this.dareRepository = dareRepository;
        this.planning = planning;
        this.images = images;
        this.clock = clock;
    }

    public Optional<Completion> find(Long dareId, LocalDate date) {
        return completionRepository.findByDareIdAndChallengeDate(dareId, date);
    }

    public List<Completion> forDate(LocalDate date) {
        return completionRepository.findByChallengeDate(date);
    }

    public Set<Long> completedDareIds(LocalDate date) {
        Set<Long> ids = new HashSet<>();
        forDate(date).forEach(c -> ids.add(c.getDare().getId()));
        return ids;
    }

    public Completion complete(Dare dare, LocalDate date, String note, MultipartFile image) throws IOException {
        if (!planning.isScheduled(dare, date)) throw new IllegalArgumentException("오늘 예정된 도전이 아닙니다.");
        Optional<Completion> existing = find(dare.getId(), date);
        if (existing.isPresent()) return existing.get();

        String imagePath = images.save(image);
        String cleanNote = note == null ? "" : note.trim();
        if (cleanNote.length() > 300) cleanNote = cleanNote.substring(0, 300);
        return completionRepository.save(new Completion(dare, date, cleanNote, imagePath, LocalDateTime.now(clock)));
    }

    /**
     * Removes the completion of {@code dareId} on {@code date} together with its photo.
     * The plan of that day is unchanged, so the challenge simply becomes "not done" again.
     */
    @Transactional
    public void undo(Long dareId, LocalDate date) {
        Completion completion = find(dareId, date)
                .orElseThrow(() -> new IllegalArgumentException("취소할 완료 기록이 없습니다."));
        completionRepository.delete(completion);
        images.deleteQuietly(completion.getImagePath());
    }

    public List<Completion> history() {
        return completionRepository.findAllByOrderByChallengeDateDescCompletedAtDesc();
    }

    public List<Completion> between(LocalDate start, LocalDate end) {
        return completionRepository.findByChallengeDateBetweenOrderByChallengeDateAsc(start, end);
    }

    public long countBetween(LocalDate start, LocalDate end) {
        return completionRepository.countByChallengeDateBetween(start, end);
    }

    /**
     * Overall streak counts consecutive scheduled days only. Non-scheduled days are skipped.
     */
    public int calculateStreak(LocalDate today) {
        Set<LocalDate> completedDates = new HashSet<>();
        history().forEach(c -> completedDates.add(c.getChallengeDate()));

        LocalDate cursor = completedDates.contains(today) ? today : today.minusDays(1);
        // Fetch once instead of querying for each historical day.
        List<Dare> allDares = dareRepository.findAll();
        int streak = 0;
        int checked = 0;
        while (checked < 366) {
            // A mutable cursor cannot be captured in a Java lambda.
            boolean hasSchedule = completedDates.contains(cursor);
            for (Dare dare : allDares) {
                if (hasSchedule) break;
                if (planning.isScheduled(dare, cursor)) {
                    hasSchedule = true;
                    break;
                }
            }
            if (!hasSchedule) {
                cursor = cursor.minusDays(1);
                checked++;
                continue;
            }
            if (!completedDates.contains(cursor)) break;
            streak++;
            cursor = cursor.minusDays(1);
            checked++;
        }
        return streak;
    }

    public int calculateCurrentStreak() {
        return calculateStreak(LocalDate.now(clock));
    }
}
