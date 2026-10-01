package com.dayspark.app.service;

import com.dayspark.app.model.Completion;
import com.dayspark.app.model.Dare;
import com.dayspark.app.repository.CompletionRepository;
import com.dayspark.app.repository.DareRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CompletionService {
    private final CompletionRepository completionRepository;
    private final DareRepository dareRepository;
    private final Path uploadRoot;
    private final PlanningService planning;
    private final Clock clock;

    public CompletionService(CompletionRepository completionRepository, DareRepository dareRepository, PlanningService planning,
                             Clock clock, @Value("${app.upload-dir:uploads}") String uploadDir) throws IOException {
        this.completionRepository = completionRepository;
        this.dareRepository = dareRepository;
        this.planning = planning;
        this.clock = clock;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadRoot);
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

        String imagePath = saveImage(image);
        String cleanNote = note == null ? "" : note.trim();
        if (cleanNote.length() > 300) cleanNote = cleanNote.substring(0, 300);
        return completionRepository.save(new Completion(dare, date, cleanNote, imagePath));
    }

    private String saveImage(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) return null;
        String original = image.getOriginalFilename() == null ? "image" : image.getOriginalFilename();
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) extension = original.substring(dot).toLowerCase();
        if (!List.of(".jpg", ".jpeg", ".png", ".webp", ".gif").contains(extension)) {
            throw new IllegalArgumentException("JPG, PNG, WEBP, GIF 이미지만 업로드할 수 있습니다.");
        }
        String filename = UUID.randomUUID() + extension;
        Path target = uploadRoot.resolve(filename).normalize();
        if (!target.startsWith(uploadRoot)) throw new IllegalArgumentException("올바르지 않은 업로드 경로입니다.");
        try (InputStream in = image.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return "/uploads/" + filename;
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
            boolean hasSchedule = false;
            for (Dare dare : allDares) {
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
