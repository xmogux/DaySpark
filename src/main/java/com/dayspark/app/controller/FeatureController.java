package com.dayspark.app.controller;

import com.dayspark.app.domain.StreakCalculator;
import com.dayspark.app.model.Completion;
import com.dayspark.app.model.Dare;
import com.dayspark.app.model.FocusSession;
import com.dayspark.app.service.CompletionService;
import com.dayspark.app.service.DareService;
import com.dayspark.app.service.FocusService;
import com.dayspark.app.service.PlanningService;
import com.dayspark.app.service.ProgressService;
import com.dayspark.app.service.ReviewService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class FeatureController {
    private final DareService dares;
    private final CompletionService completions;
    private final PlanningService planning;
    private final ProgressService progress;
    private final FocusService focus;
    private final ReviewService reviews;
    private final Clock clock;

    public FeatureController(DareService dares, CompletionService completions, PlanningService planning,
                             ProgressService progress, FocusService focus, ReviewService reviews, Clock clock) {
        this.dares = dares;
        this.completions = completions;
        this.planning = planning;
        this.progress = progress;
        this.focus = focus;
        this.reviews = reviews;
        this.clock = clock;
    }

    @GetMapping("/challenges/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Dare dare = dares.get(id);
        LocalDate today = LocalDate.now(clock);
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate monthStart = today.withDayOfMonth(1);

        List<Completion> dareCompletions = completions.history().stream()
                .filter(c -> c.getDare().getId().equals(id))
                .toList();
        Set<LocalDate> completedDates = dareCompletions.stream().map(Completion::getChallengeDate).collect(Collectors.toSet());

        long weekDone = dareCompletions.stream().filter(c -> !c.getChallengeDate().isBefore(weekStart)).count();
        long monthDone = dareCompletions.stream().filter(c -> !c.getChallengeDate().isBefore(monthStart)).count();
        long monthPlanned = 0;
        for (LocalDate date = monthStart; !date.isAfter(today); date = date.plusDays(1)) {
            if (completedDates.contains(date) || planning.isScheduled(dare, date)) monthPlanned++;
        }

        int goalStreak = goalStreak(dare, today, completedDates);
        List<FocusSession> sessions = focus.completedFor(dare);
        long weekFocus = sessions.stream()
                .filter(s -> s.getCompletedDay() != null && !s.getCompletedDay().isBefore(weekStart))
                .mapToLong(FocusSession::getPlannedMinutes).sum();
        long monthFocus = sessions.stream()
                .filter(s -> s.getCompletedDay() != null && !s.getCompletedDay().isBefore(monthStart))
                .mapToLong(FocusSession::getPlannedMinutes).sum();

        model.addAttribute("dare", dare);
        model.addAttribute("units", DareService.GOAL_UNITS);
        model.addAttribute("progress", progress.progress(dare, today));
        model.addAttribute("weekDone", weekDone);
        model.addAttribute("monthDone", monthDone);
        model.addAttribute("monthRate", monthPlanned == 0 ? 0 : Math.round(100d * monthDone / monthPlanned));
        model.addAttribute("goalStreak", goalStreak);
        model.addAttribute("weekFocus", weekFocus);
        model.addAttribute("monthFocus", monthFocus);
        model.addAttribute("focusCount", sessions.size());
        model.addAttribute("overrides", planning.forDare(id));
        model.addAttribute("today", today);
        var reviewPlan = reviews.planFor(dare);
        long reviewDone = reviewPlan.stream().filter(ReviewService.ReviewItem::completed).count();
        model.addAttribute("reviewPlan", reviewPlan);
        model.addAttribute("reviewDone", reviewDone);
        model.addAttribute("reviewTotal", reviewPlan.size());
        model.addAttribute("recentCompletions", dareCompletions.stream().limit(6).toList());
        return "challenge-detail";
    }

    private int goalStreak(Dare dare, LocalDate today, Set<LocalDate> completed) {
        return StreakCalculator.count(
                today,
                dare.getStartDate(),
                day -> completed.contains(day) || planning.isScheduled(dare, day),
                completed::contains);
    }

    @PostMapping("/challenges/{id}/goals")
    public String goals(@PathVariable Long id, @RequestParam int targetValue, @RequestParam String targetUnit,
                        @RequestParam(defaultValue = "0") int weeklyTarget, RedirectAttributes redirect) {
        try {
            dares.configureGoals(id, targetValue, targetUnit, weeklyTarget);
            redirect.addFlashAttribute("success", "수치·주간 목표를 저장했어요.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/challenges/" + id;
    }

    @PostMapping("/challenges/{id}/progress")
    public String addProgress(@PathVariable Long id, @RequestParam int amount, RedirectAttributes redirect) {
        try {
            progress.add(dares.get(id), LocalDate.now(clock), amount);
            redirect.addFlashAttribute("success", "진행량을 기록했어요.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/challenges/" + id;
    }

    @PostMapping("/challenges/{id}/schedule")
    public String schedule(@PathVariable Long id,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestParam String mode,
                           @RequestParam(defaultValue = "") String note,
                           RedirectAttributes redirect) {
        try {
            planning.setOverride(dares.get(id), date, mode, note);
            redirect.addFlashAttribute("success", "해당 날짜의 일정을 반영했어요.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/challenges/" + id;
    }


    @PostMapping("/challenges/{id}/reviews/{offset}/toggle")
    public String toggleReview(@PathVariable Long id, @PathVariable int offset,
                               @RequestParam(defaultValue = "detail") String returnTo,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               RedirectAttributes redirect) {
        try {
            boolean completed = reviews.toggle(dares.get(id), offset);
            redirect.addFlashAttribute("success", completed ? "복습을 완료로 표시했어요." : "복습 완료 표시를 취소했어요.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        if ("home".equals(returnTo)) return "redirect:/";
        if ("calendar".equals(returnTo) && date != null) return "redirect:/calendar/day?date=" + date;
        return "redirect:/challenges/" + id;
    }

    @GetMapping("/focus")
    public String focus(@RequestParam(required = false) Long dareId, Model model) {
        model.addAttribute("dares", dares.active());
        model.addAttribute("selectedDareId", dareId);
        return "focus";
    }

    @GetMapping("/calendar/day")
    public String day(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, Model model) {
        LocalDate today = LocalDate.now(clock);
        if (date.isAfter(today.plusYears(1)) || date.isBefore(today.minusYears(4))) {
            throw new IllegalArgumentException("날짜 범위를 벗어났습니다.");
        }
        model.addAttribute("date", date);
        model.addAttribute("scheduled", dares.plannedFor(date));
        model.addAttribute("completed", completions.forDate(date));
        model.addAttribute("reviews", reviews.forDate(date));
        List<FocusSession> sessions = focus.completedBetween(date, date);
        model.addAttribute("sessions", sessions);
        model.addAttribute("focusMinutes", sessions.stream().mapToInt(FocusSession::getPlannedMinutes).sum());
        return "calendar-day";
    }
}
