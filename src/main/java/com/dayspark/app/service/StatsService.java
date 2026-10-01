package com.dayspark.app.service;

import com.dayspark.app.domain.StreakCalculator;
import com.dayspark.app.model.Completion;
import com.dayspark.app.model.Dare;
import com.dayspark.app.model.FocusSession;
import com.dayspark.app.service.ReviewService.ReviewItem;
import com.dayspark.app.service.view.CalendarDay;
import com.dayspark.app.service.view.CalendarMonth;
import com.dayspark.app.service.view.CategoryStat;
import com.dayspark.app.service.view.DailyStat;
import com.dayspark.app.service.view.DareInsights;
import com.dayspark.app.service.view.DayDetail;
import com.dayspark.app.service.view.DayPlan;
import com.dayspark.app.service.view.StatsOverview;
import com.dayspark.app.service.view.TodayBoard;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Read-only aggregation for the home, calendar, statistics and challenge-detail screens.
 * Every number is derived from {@link DareService#plannedFor} (the day plan) and the completion
 * records, so all screens agree. {@code today} is passed in by the caller to keep this testable.
 */
@Service
public class StatsService {
    private static final DateTimeFormatter WEEKDAY = DateTimeFormatter.ofPattern("E", Locale.KOREAN);
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("yyyy년 M월");

    private final DareService dares;
    private final CompletionService completions;
    private final PlanningService planning;
    private final ProgressService progress;
    private final FocusService focus;
    private final ReviewService reviews;

    public StatsService(DareService dares, CompletionService completions, PlanningService planning,
                        ProgressService progress, FocusService focus, ReviewService reviews) {
        this.dares = dares;
        this.completions = completions;
        this.planning = planning;
        this.progress = progress;
        this.focus = focus;
        this.reviews = reviews;
    }

    public DayPlan dayPlan(LocalDate date) {
        return new DayPlan(date, dares.plannedFor(date), completions.completedDareIds(date));
    }

    public TodayBoard today(LocalDate today) {
        DayPlan plan = dayPlan(today);
        Map<Long, Long> weekCounts = completions.between(today.with(DayOfWeek.MONDAY), today).stream()
                .collect(Collectors.groupingBy(c -> c.getDare().getId(), Collectors.counting()));
        Map<Long, Integer> progressToday = new HashMap<>();
        Map<Long, Long> completionsThisWeek = new HashMap<>();
        for (Dare dare : plan.planned()) {
            progressToday.put(dare.getId(), progress.progress(dare, today));
            completionsThisWeek.put(dare.getId(), weekCounts.getOrDefault(dare.getId(), 0L));
        }
        return new TodayBoard(plan, progressToday, completionsThisWeek, overallStreak(today));
    }

    /**
     * Overall streak: consecutive planned days with at least one completion
     * (docs/domain-rules.md, "Streak"). A day is planned when any challenge is scheduled or was
     * completed that day.
     */
    public int overallStreak(LocalDate today) {
        Set<LocalDate> completedDates = new HashSet<>();
        completions.history().forEach(c -> completedDates.add(c.getChallengeDate()));
        List<Dare> all = dares.all();
        LocalDate earliest = all.stream().map(Dare::getStartDate).min(Comparator.naturalOrder()).orElse(null);
        return StreakCalculator.count(
                today,
                earliest,
                day -> completedDates.contains(day) || all.stream().anyMatch(d -> planning.isScheduled(d, day)),
                completedDates::contains);
    }

    public CalendarMonth calendarMonth(YearMonth month, LocalDate today) {
        LocalDate gridStart = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate gridEnd = month.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        Map<LocalDate, Long> doneByDay = new HashMap<>();
        completions.between(gridStart, gridEnd).forEach(c -> doneByDay.merge(c.getChallengeDate(), 1L, Long::sum));
        Map<LocalDate, Long> reviewsPlanned = new HashMap<>();
        Map<LocalDate, Long> reviewsDone = new HashMap<>();
        reviews.between(gridStart, gridEnd).forEach(r -> {
            reviewsPlanned.merge(r.dueDate(), 1L, Long::sum);
            if (r.completed()) reviewsDone.merge(r.dueDate(), 1L, Long::sum);
        });

        List<CalendarDay> days = new ArrayList<>();
        long monthDone = 0;
        long monthPlanned = 0;
        for (LocalDate d = gridStart; !d.isAfter(gridEnd); d = d.plusDays(1)) {
            long done = doneByDay.getOrDefault(d, 0L);
            long planned = dares.plannedCount(d);
            boolean inMonth = YearMonth.from(d).equals(month);
            boolean future = d.isAfter(today);
            if (inMonth && !future) {
                monthDone += done;
                monthPlanned += planned;
            }
            days.add(new CalendarDay(d, inMonth, done, planned, percent(done, planned), d.equals(today), future,
                    reviewsPlanned.getOrDefault(d, 0L), reviewsDone.getOrDefault(d, 0L)));
        }

        List<FocusSession> sessions = focus.completedBetween(gridStart, gridEnd);
        Map<LocalDate, Long> focusByDay = new HashMap<>();
        sessions.forEach(s -> focusByDay.merge(s.getCompletedDay(), (long) s.getPlannedMinutes(), Long::sum));
        List<FocusSession> monthSessions = sessions.stream()
                .filter(s -> YearMonth.from(s.getCompletedDay()).equals(month) && !s.getCompletedDay().isAfter(today))
                .toList();
        long focusMinutes = monthSessions.stream().mapToLong(FocusSession::getPlannedMinutes).sum();

        boolean hasElapsed = !month.atDay(1).isAfter(today) && monthPlanned > 0;
        String note = month.equals(YearMonth.from(today)) ? "이번 달 · 오늘까지"
                : month.isAfter(YearMonth.from(today)) ? "아직 시작 전" : "선택한 달 전체";
        return new CalendarMonth(month, days, focusByDay, monthDone, monthPlanned, rate(monthDone, monthPlanned),
                hasElapsed, note, focusMinutes, monthSessions.size());
    }

    public static String monthLabel(YearMonth month) {
        return month.format(MONTH_LABEL);
    }

    public DayDetail dayDetail(LocalDate date) {
        List<FocusSession> sessions = focus.completedBetween(date, date);
        int focusMinutes = sessions.stream().mapToInt(FocusSession::getPlannedMinutes).sum();
        return new DayDetail(date, dares.plannedFor(date), completions.forDate(date), reviews.forDate(date),
                sessions, focusMinutes);
    }

    public StatsOverview overview(LocalDate today) {
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate monthStart = today.withDayOfMonth(1);
        long weekDone = completions.countBetween(weekStart, today);
        long monthDone = completions.countBetween(monthStart, today);

        List<DailyStat> weekBars = new ArrayList<>();
        for (LocalDate d = weekStart; !d.isAfter(weekStart.plusDays(6)); d = d.plusDays(1)) {
            long done = d.isAfter(today) ? 0 : completions.forDate(d).size();
            long planned = d.isAfter(today) ? 0 : dares.plannedCount(d);
            weekBars.add(new DailyStat(d.format(WEEKDAY), done, planned, percent(done, planned)));
        }

        Map<String, Long> byCategory = new LinkedHashMap<>();
        completions.between(monthStart, today).forEach(c -> byCategory.merge(c.getDare().getCategory(), 1L, Long::sum));
        String topCategory = byCategory.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("-");
        long maxCategory = byCategory.values().stream().mapToLong(v -> v).max().orElse(1);
        List<CategoryStat> categoryStats = byCategory.entrySet().stream()
                .map(e -> new CategoryStat(e.getKey(), e.getValue(), (int) Math.round(e.getValue() * 100.0 / maxCategory)))
                .toList();

        List<FocusSession> weekSessions = focus.completedBetween(weekStart, today);
        List<FocusSession> monthSessions = focus.completedBetween(monthStart, today);
        Map<String, Long> focusPerCategory = new LinkedHashMap<>();
        monthSessions.forEach(s -> focusPerCategory.merge(s.getDare().getCategory(), (long) s.getPlannedMinutes(), Long::sum));

        return new StatsOverview(
                overallStreak(today),
                weekDone,
                monthDone,
                rate(weekDone, plannedBetween(weekStart, today)),
                rate(monthDone, plannedBetween(monthStart, today)),
                topCategory,
                weekBars,
                categoryStats,
                focus.minutesBetween(today, today),
                minutes(weekSessions),
                minutes(monthSessions),
                weekSessions.size(),
                monthSessions.size(),
                focusPerCategory);
    }

    public DareInsights dareInsights(Dare dare, LocalDate today) {
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate monthStart = today.withDayOfMonth(1);
        List<Completion> history = completions.history().stream()
                .filter(c -> c.getDare().getId().equals(dare.getId()))
                .toList();
        Set<LocalDate> completedDates = history.stream().map(Completion::getChallengeDate).collect(Collectors.toSet());

        long weekDone = history.stream().filter(c -> !c.getChallengeDate().isBefore(weekStart)).count();
        long monthDone = history.stream().filter(c -> !c.getChallengeDate().isBefore(monthStart)).count();
        long monthPlanned = 0;
        for (LocalDate d = monthStart; !d.isAfter(today); d = d.plusDays(1)) {
            if (completedDates.contains(d) || planning.isScheduled(dare, d)) monthPlanned++;
        }
        int goalStreak = StreakCalculator.count(
                today,
                dare.getStartDate(),
                day -> completedDates.contains(day) || planning.isScheduled(dare, day),
                completedDates::contains);

        List<FocusSession> sessions = focus.completedFor(dare);
        long weekFocus = minutes(sessions.stream().filter(s -> !s.getCompletedDay().isBefore(weekStart)).toList());
        long monthFocus = minutes(sessions.stream().filter(s -> !s.getCompletedDay().isBefore(monthStart)).toList());
        List<ReviewItem> reviewPlan = reviews.planFor(dare);
        long reviewDone = reviewPlan.stream().filter(ReviewItem::completed).count();

        return new DareInsights(progress.progress(dare, today), weekDone, monthDone, rate(monthDone, monthPlanned),
                goalStreak, weekFocus, monthFocus, sessions.size(), reviewPlan, reviewDone,
                history.stream().limit(6).toList());
    }

    private long plannedBetween(LocalDate start, LocalDate end) {
        long total = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) total += dares.plannedCount(d);
        return total;
    }

    /** Percentage for a single day or bar, capped at 100. */
    private static int percent(long done, long planned) {
        return planned == 0 ? 0 : (int) Math.min(100, Math.round(done * 100.0 / planned));
    }

    /** Rate over a period, capped at 100. With the day-plan rule done never exceeds planned anyway. */
    private static long rate(long done, long planned) {
        return planned == 0 ? 0 : Math.min(100, Math.round(done * 100.0 / planned));
    }

    private static long minutes(List<FocusSession> sessions) {
        return sessions.stream().mapToLong(FocusSession::getPlannedMinutes).sum();
    }
}
