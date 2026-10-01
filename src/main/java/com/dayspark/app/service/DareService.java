package com.dayspark.app.service;

import com.dayspark.app.domain.DareCategory;
import com.dayspark.app.domain.RepeatType;
import com.dayspark.app.model.Completion;
import com.dayspark.app.model.Dare;
import com.dayspark.app.repository.CompletionRepository;
import com.dayspark.app.repository.DareRepository;
import com.dayspark.app.repository.FocusSessionRepository;
import com.dayspark.app.repository.ProgressLogRepository;
import com.dayspark.app.repository.ReviewRecordRepository;
import com.dayspark.app.repository.ScheduleExceptionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
public class DareService {
    private final DareRepository dareRepository;
    private final CompletionRepository completionRepository;
    private final ScheduleExceptionRepository exceptionRepository;
    private final FocusSessionRepository focusRepository;
    private final ProgressLogRepository progressRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final PlanningService planning;
    private final Clock clock;
    private final Path uploadRoot;

    public DareService(DareRepository dareRepository, CompletionRepository completionRepository,
                       ScheduleExceptionRepository exceptionRepository, FocusSessionRepository focusRepository,
                       ProgressLogRepository progressRepository, ReviewRecordRepository reviewRecordRepository, PlanningService planning, Clock clock,
                       @Value("${app.upload-dir:uploads}") String uploadDir) {
        this.dareRepository = dareRepository;
        this.completionRepository = completionRepository;
        this.exceptionRepository = exceptionRepository;
        this.focusRepository = focusRepository;
        this.progressRepository = progressRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.planning = planning;
        this.clock = clock;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public List<Dare> all() {
        return dareRepository.findAllByOrderByActiveDescIdAsc();
    }

    public List<Dare> active() {
        return dareRepository.findByActiveTrueOrderByIdAsc();
    }

    public List<Dare> scheduledFor(LocalDate date) {
        return all().stream().filter(d -> planning.isScheduled(d, date)).toList();
    }

    public long plannedCount(LocalDate date) {
        return scheduledFor(date).size();
    }

    public Dare get(Long id) {
        return dareRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("도전을 찾을 수 없습니다."));
    }

    public Dare create(String title, String description, String category, int minutes, String repeatType,
                       List<String> repeatDays, LocalDate startDate, LocalDate endDate) {
        validate(title, startDate, endDate, repeatType, repeatDays, category);
        return dareRepository.save(new Dare(title, description, category, minutes, RepeatType.parse(repeatType).name(),
                normalizeRepeatDays(repeatDays), startDate, endDate));
    }

    public Dare update(Long id, String title, String description, String category, int minutes, String repeatType,
                       List<String> repeatDays, LocalDate startDate, LocalDate endDate) {
        validate(title, startDate, endDate, repeatType, repeatDays, category);
        Dare dare = get(id);
        dare.update(title, description, category, minutes, RepeatType.parse(repeatType).name(),
                normalizeRepeatDays(repeatDays), startDate, endDate);
        return dareRepository.save(dare);
    }

    private String normalizeRepeatDays(List<String> repeatDays) {
        if (repeatDays == null) return "";
        return repeatDays.stream().map(this::parseDay).map(DayOfWeek::name).distinct().reduce((a, b) -> a + "," + b).orElse("");
    }

    private DayOfWeek parseDay(String value) {
        try {
            return DayOfWeek.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("지원되지 않는 요일 값입니다.");
        }
    }

    private void validateGoals(int targetValue, String targetUnit, int weeklyTarget) {
        if (targetValue < 1 || targetValue > 10000) throw new IllegalArgumentException("목표 수치는 1~10000 사이여야 합니다.");
        if (!List.of("분", "페이지", "회", "개", "잔").contains(targetUnit)) throw new IllegalArgumentException("지원되지 않는 목표 단위입니다.");
        if (weeklyTarget < 0 || weeklyTarget > 7) throw new IllegalArgumentException("주간 목표는 0~7회로 설정해주세요.");
    }

    public Dare configureGoals(Long id, int targetValue, String targetUnit, int weeklyTarget) {
        validateGoals(targetValue, targetUnit, weeklyTarget);
        Dare dare = get(id);
        dare.setGoals(targetValue, targetUnit, weeklyTarget);
        return dareRepository.save(dare);
    }

    @Transactional
    public Dare createWithGoals(String title, String description, String category, int minutes, String repeatType,
                                List<String> repeatDays, LocalDate startDate, LocalDate endDate,
                                int targetValue, String targetUnit, int weeklyTarget) {
        validateGoals(targetValue, targetUnit, weeklyTarget);
        Dare dare = create(title, description, category, minutes, repeatType, repeatDays, startDate, endDate);
        dare.setGoals(targetValue, targetUnit, weeklyTarget);
        return dareRepository.save(dare);
    }

    @Transactional
    public Dare updateWithGoals(Long id, String title, String description, String category, int minutes, String repeatType,
                                List<String> repeatDays, LocalDate startDate, LocalDate endDate,
                                int targetValue, String targetUnit, int weeklyTarget) {
        validateGoals(targetValue, targetUnit, weeklyTarget);
        Dare dare = update(id, title, description, category, minutes, repeatType, repeatDays, startDate, endDate);
        dare.setGoals(targetValue, targetUnit, weeklyTarget);
        return dareRepository.save(dare);
    }

    @Transactional
    public Dare createWithGoalsAndReview(String title, String description, String category, int minutes, String repeatType,
                                         List<String> repeatDays, LocalDate startDate, LocalDate endDate,
                                         int targetValue, String targetUnit, int weeklyTarget,
                                         boolean reviewEnabled, LocalDate reviewBaseDate, String reviewIntervals) {
        validateGoals(targetValue, targetUnit, weeklyTarget);
        String normalized = validateAndNormalizeReview(category, reviewEnabled, reviewBaseDate, reviewIntervals, startDate);
        Dare dare = create(title, description, category, minutes, repeatType, repeatDays, startDate, endDate);
        dare.setGoals(targetValue, targetUnit, weeklyTarget);
        dare.configureReview(reviewEnabled && "STUDY".equals(dare.getCategory()),
                reviewBaseDate == null ? dare.getStartDate() : reviewBaseDate, normalized);
        return dareRepository.save(dare);
    }

    @Transactional
    public Dare updateWithGoalsAndReview(Long id, String title, String description, String category, int minutes, String repeatType,
                                         List<String> repeatDays, LocalDate startDate, LocalDate endDate,
                                         int targetValue, String targetUnit, int weeklyTarget,
                                         boolean reviewEnabled, LocalDate reviewBaseDate, String reviewIntervals) {
        validateGoals(targetValue, targetUnit, weeklyTarget);
        String normalized = validateAndNormalizeReview(category, reviewEnabled, reviewBaseDate, reviewIntervals, startDate);
        Dare before = get(id);
        boolean enabled = reviewEnabled && "STUDY".equals(DareCategory.parse(category).name());
        LocalDate effectiveBase = reviewBaseDate == null ? startDate : reviewBaseDate;
        boolean reviewConfigChanged = before.isReviewEnabled() != enabled
                || !java.util.Objects.equals(before.getReviewBaseDate(), enabled ? effectiveBase : null)
                || !java.util.Objects.equals(before.getReviewIntervals(), enabled ? normalized : "");
        Dare dare = update(id, title, description, category, minutes, repeatType, repeatDays, startDate, endDate);
        dare.setGoals(targetValue, targetUnit, weeklyTarget);
        dare.configureReview(enabled, effectiveBase == null ? dare.getStartDate() : effectiveBase, normalized);
        if (reviewConfigChanged) reviewRecordRepository.deleteAllByDareId(id);
        return dareRepository.save(dare);
    }

    private String validateAndNormalizeReview(String category, boolean enabled, LocalDate baseDate, String intervals, LocalDate startDate) {
        if (!enabled || !"STUDY".equals(DareCategory.parse(category).name())) return "";
        LocalDate base = baseDate == null ? startDate : baseDate;
        if (base == null) throw new IllegalArgumentException("복습 기준일을 입력해주세요.");
        if (intervals == null || intervals.isBlank()) throw new IllegalArgumentException("복습 간격을 하나 이상 입력해주세요.");
        String normalized = intervals.trim().replace('，', ',');
        java.util.SortedSet<Integer> values = new java.util.TreeSet<>();
        for (String part : normalized.split("[,\\s]+")) {
            if (part.isBlank()) continue;
            int days;
            try { days = Integer.parseInt(part); }
            catch (NumberFormatException ex) { throw new IllegalArgumentException("복습 간격은 1, 3, 7처럼 날짜 수(숫자)로 입력해주세요."); }
            if (days < 1 || days > 3650) throw new IllegalArgumentException("복습 간격은 1일~3650일 사이로 설정해주세요.");
            values.add(days);
        }
        if (values.isEmpty()) throw new IllegalArgumentException("복습 간격을 하나 이상 입력해주세요.");
        if (values.size() > 12) throw new IllegalArgumentException("복습 일정은 최대 12개까지 만들 수 있습니다.");
        return values.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
    }

    public Dare toggle(Long id) {
        Dare dare = get(id);
        if (dare.isActive()) {
            dare.pauseFrom(LocalDate.now(clock));
        } else {
            dare.resume();
        }
        return dareRepository.save(dare);
    }

    @Transactional
    public void delete(Long id) {
        get(id);
        completionRepository.findAll().stream()
                .filter(c -> c.getDare().getId().equals(id))
                .map(Completion::getImagePath)
                .filter(path -> path != null && path.startsWith("/uploads/"))
                .forEach(this::deleteUploadedFileQuietly);
        focusRepository.deleteAllByDareId(id);
        progressRepository.deleteAllByDareId(id);
        reviewRecordRepository.deleteAllByDareId(id);
        exceptionRepository.deleteAllByDareId(id);
        completionRepository.deleteAllByDare_Id(id);
        dareRepository.deleteById(id);
    }

    private void deleteUploadedFileQuietly(String webPath) {
        String filename = webPath.substring("/uploads/".length());
        Path file = uploadRoot.resolve(filename).normalize();
        if (!file.startsWith(uploadRoot)) return;
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // The database delete should not fail just because an old proof image was already removed.
        }
    }

    private void validate(String title, LocalDate startDate, LocalDate endDate, String repeatType, List<String> repeatDays, String category) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("도전 이름을 입력해주세요.");
        if (title.trim().length() > 80) throw new IllegalArgumentException("도전 이름은 80자 이하로 입력해주세요.");
        DareCategory.parse(category);
        RepeatType parsedRepeat = RepeatType.parse(repeatType);
        if (endDate != null && startDate != null && endDate.isBefore(startDate)) throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
        if (parsedRepeat == RepeatType.CUSTOM) {
            if (repeatDays == null || repeatDays.isEmpty()) throw new IllegalArgumentException("반복할 요일을 하나 이상 선택해주세요.");
            repeatDays.forEach(this::parseDay);
        } else if (repeatDays != null) {
            repeatDays.forEach(this::parseDay);
        }
    }
}
