package com.dayspark.app.service;

import com.dayspark.app.domain.DareCategory;
import com.dayspark.app.domain.RepeatType;
import com.dayspark.app.domain.ReviewIntervals;
import com.dayspark.app.model.Completion;
import com.dayspark.app.model.Dare;
import com.dayspark.app.repository.CompletionRepository;
import com.dayspark.app.repository.DareRepository;
import com.dayspark.app.repository.FocusSessionRepository;
import com.dayspark.app.repository.ProgressLogRepository;
import com.dayspark.app.repository.ReviewRecordRepository;
import com.dayspark.app.repository.ScheduleExceptionRepository;
import com.dayspark.app.service.dto.DareRequest;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creating, editing, pausing and deleting challenges, and deciding which challenges a day plans. */
@Service
public class DareService {

    /** Units offered for the numeric daily goal. The form and the validation both use this list. */
    public static final List<String> GOAL_UNITS = List.of("분", "페이지", "회", "개", "잔");

    private final DareRepository dareRepository;
    private final CompletionRepository completionRepository;
    private final ScheduleExceptionRepository exceptionRepository;
    private final FocusSessionRepository focusRepository;
    private final ProgressLogRepository progressRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final PlanningService planning;
    private final ImageStorage images;
    private final Clock clock;

    public DareService(
        DareRepository dareRepository,
        CompletionRepository completionRepository,
        ScheduleExceptionRepository exceptionRepository,
        FocusSessionRepository focusRepository,
        ProgressLogRepository progressRepository,
        ReviewRecordRepository reviewRecordRepository,
        PlanningService planning,
        ImageStorage images,
        Clock clock
    ) {
        this.dareRepository = dareRepository;
        this.completionRepository = completionRepository;
        this.exceptionRepository = exceptionRepository;
        this.focusRepository = focusRepository;
        this.progressRepository = progressRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.planning = planning;
        this.images = images;
        this.clock = clock;
    }

    public List<Dare> all() {
        return dareRepository.findAllByOrderByActiveDescIdAsc();
    }

    public List<Dare> active() {
        return dareRepository.findByActiveTrueOrderByIdAsc();
    }

    public Dare get(Long id) {
        return dareRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("도전을 찾을 수 없습니다."));
    }

    /**
     * Challenges that count towards a day's plan: those scheduled by their current rule plus any
     * challenge completed that day. A completion proves the challenge was planned when it was
     * recorded, so editing a repeat rule or pausing later never makes done exceed planned.
     */
    public List<Dare> plannedFor(LocalDate date) {
        Set<Long> completedIds = completionRepository
            .findByChallengeDate(date)
            .stream()
            .map(c -> c.getDare().getId())
            .collect(Collectors.toSet());
        return all()
            .stream()
            .filter(d -> completedIds.contains(d.getId()) || planning.isScheduled(d, date))
            .toList();
    }

    public long plannedCount(LocalDate date) {
        return plannedFor(date).size();
    }

    @Transactional
    public Dare create(DareRequest request) {
        Validated v = validate(request);
        Dare dare = new Dare(
            request.getTitle(),
            request.getDescription(),
            v.category().name(),
            request.getMinutes(),
            v.repeatType().name(),
            v.repeatDays(),
            request.getStartDate(),
            request.getEndDate()
        );
        dare.setGoals(request.getTargetValue(), request.getTargetUnit(), request.getWeeklyTarget());
        dare.configureReview(v.review() != null, v.reviewBase(), v.review() == null ? "" : v.review().normalized());
        return dareRepository.save(dare);
    }

    /** Applies the whole form. If the review plan itself changed, old review check marks are cleared. */
    @Transactional
    public Dare update(Long id, DareRequest request) {
        Validated v = validate(request);
        Dare dare = get(id);
        String reviewBefore = reviewFingerprint(dare);
        dare.describe(request.getTitle(), request.getDescription(), v.category().name(), request.getMinutes());
        dare.reschedule(v.repeatType().name(), v.repeatDays(), request.getStartDate(), request.getEndDate());
        dare.setGoals(request.getTargetValue(), request.getTargetUnit(), request.getWeeklyTarget());
        dare.configureReview(v.review() != null, v.reviewBase(), v.review() == null ? "" : v.review().normalized());
        if (!reviewBefore.equals(reviewFingerprint(dare))) reviewRecordRepository.deleteAllByDareId(id);
        return dareRepository.save(dare);
    }

    /** Goal-only update from the challenge detail page. */
    @Transactional
    public Dare configureGoals(Long id, int targetValue, String targetUnit, int weeklyTarget) {
        validateGoals(targetValue, targetUnit, weeklyTarget);
        Dare dare = get(id);
        dare.setGoals(targetValue, targetUnit, weeklyTarget);
        return dareRepository.save(dare);
    }

    @Transactional
    public Dare toggle(Long id) {
        Dare dare = get(id);
        if (dare.isActive()) {
            dare.pauseFrom(LocalDate.now(clock));
        } else {
            dare.resume();
        }
        return dareRepository.save(dare);
    }

    /** Deletes the challenge with all its records and uploaded photos. */
    @Transactional
    public void delete(Long id) {
        get(id);
        completionRepository
            .findAll()
            .stream()
            .filter(c -> c.getDare().getId().equals(id))
            .map(Completion::getImagePath)
            .forEach(images::deleteQuietly);
        focusRepository.deleteAllByDareId(id);
        progressRepository.deleteAllByDareId(id);
        reviewRecordRepository.deleteAllByDareId(id);
        exceptionRepository.deleteAllByDareId(id);
        completionRepository.deleteAllByDare_Id(id);
        dareRepository.deleteById(id);
    }

    /** Parsed, checked form values. {@code review} is null when no review plan applies. */
    private record Validated(
        DareCategory category,
        RepeatType repeatType,
        String repeatDays,
        ReviewIntervals review,
        LocalDate reviewBase
    ) {}

    private Validated validate(DareRequest r) {
        String title = r.getTitle();
        if (title == null || title.isBlank()) throw new IllegalArgumentException("도전 이름을 입력해주세요.");
        if (title.trim().length() > 80) throw new IllegalArgumentException("도전 이름은 80자 이하로 입력해주세요.");
        if (r.getStartDate() == null) throw new IllegalArgumentException("시작일을 입력해주세요.");
        if (r.getEndDate() != null && r.getEndDate().isBefore(r.getStartDate())) {
            throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
        }
        DareCategory category = DareCategory.parse(r.getCategory());
        RepeatType repeatType = RepeatType.parse(r.getRepeatType());
        List<DayOfWeek> days = r.getRepeatDays().stream().map(DareService::parseDay).distinct().toList();
        if (repeatType == RepeatType.CUSTOM && days.isEmpty()) {
            throw new IllegalArgumentException("반복할 요일을 하나 이상 선택해주세요.");
        }
        validateGoals(r.getTargetValue(), r.getTargetUnit(), r.getWeeklyTarget());

        boolean reviewApplies = r.isReviewEnabled() && category == DareCategory.STUDY;
        ReviewIntervals review = reviewApplies ? ReviewIntervals.parse(r.getReviewIntervals()) : null;
        LocalDate reviewBase =
            reviewApplies && r.getReviewBaseDate() != null ? r.getReviewBaseDate() : r.getStartDate();
        String repeatDays = days.stream().map(DayOfWeek::name).collect(Collectors.joining(","));
        return new Validated(category, repeatType, repeatDays, review, reviewBase);
    }

    private static void validateGoals(int targetValue, String targetUnit, int weeklyTarget) {
        if (targetValue < 1 || targetValue > 10000) throw new IllegalArgumentException(
            "목표 수치는 1~10000 사이여야 합니다."
        );
        if (!GOAL_UNITS.contains(targetUnit)) throw new IllegalArgumentException("지원되지 않는 목표 단위입니다.");
        if (weeklyTarget < 0 || weeklyTarget > 7) throw new IllegalArgumentException(
            "주간 목표는 0~7회로 설정해주세요."
        );
    }

    private static DayOfWeek parseDay(String value) {
        try {
            return DayOfWeek.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("지원되지 않는 요일 값입니다.");
        }
    }

    private static String reviewFingerprint(Dare dare) {
        return (
            dare.isReviewEnabled() + "|" + Objects.toString(dare.getReviewBaseDate()) + "|" + dare.getReviewIntervals()
        );
    }
}
