package com.dayspark.app.service;

import com.dayspark.app.model.Completion;
import com.dayspark.app.model.Dare;
import com.dayspark.app.repository.CompletionRepository;
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
    public static final String AUTO_COMPLETE_NOTE = "목표량 달성으로 자동 완료";

    private final CompletionRepository completionRepository;
    private final PlanningService planning;
    private final ImageStorage images;
    private final Clock clock;

    public CompletionService(CompletionRepository completionRepository, PlanningService planning,
                             ImageStorage images, Clock clock) {
        this.completionRepository = completionRepository;
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
     * Completes {@code dare} on {@code date} because its numeric target was reached. Does nothing
     * (and never throws) when the day is not planned or a completion already exists.
     */
    @Transactional
    public Optional<Completion> autoComplete(Dare dare, LocalDate date) {
        if (!planning.isScheduled(dare, date) || find(dare.getId(), date).isPresent()) return Optional.empty();
        return Optional.of(completionRepository.save(
                new Completion(dare, date, AUTO_COMPLETE_NOTE, null, LocalDateTime.now(clock))));
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

}
