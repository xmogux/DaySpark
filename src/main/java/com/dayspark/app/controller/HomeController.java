package com.dayspark.app.controller;

import com.dayspark.app.domain.DareCategory;
import com.dayspark.app.model.Completion;
import com.dayspark.app.model.Dare;
import com.dayspark.app.service.CompletionService;
import com.dayspark.app.service.DareService;
import com.dayspark.app.service.ProgressService;
import com.dayspark.app.service.FocusService;
import com.dayspark.app.service.ReviewService;
import com.dayspark.app.service.dto.DareRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Controller
public class HomeController {
    private final DareService dareService;
    private final CompletionService completionService;
    private final ProgressService progressService;
    private final FocusService focusService;
    private final ReviewService reviewService;
    private final Clock clock;

    public HomeController(DareService dareService, CompletionService completionService, ProgressService progressService, FocusService focusService, ReviewService reviewService, Clock clock) {
        this.dareService = dareService;
        this.completionService = completionService;
        this.progressService = progressService;
        this.focusService = focusService;
        this.reviewService = reviewService;
        this.clock = clock;
    }

    @GetMapping("/")
    public String home(Model model) {
        LocalDate today = LocalDate.now(clock);
        List<Dare> todayDares = dareService.plannedFor(today);
        Set<Long> completedIds = completionService.completedDareIds(today);
        int total = todayDares.size();
        int done = (int) todayDares.stream().filter(d -> completedIds.contains(d.getId())).count();
        model.addAttribute("today", today.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)));
        model.addAttribute("todayDares", todayDares);
        Map<Long,Integer> dailyProgress=new HashMap<>();
        Map<Long,Long> weeklyCounts=new HashMap<>();
        LocalDate weekStart=today.with(DayOfWeek.MONDAY);
        for(Dare d:todayDares){
            dailyProgress.put(d.getId(),progressService.progress(d,today));
            weeklyCounts.put(d.getId(),completionService.between(weekStart,today).stream().filter(c->c.getDare().getId().equals(d.getId())).count());
        }
        model.addAttribute("dailyProgress",dailyProgress);
        model.addAttribute("weeklyCounts",weeklyCounts);
        model.addAttribute("todayFocus",focusService.minutesBetween(today,today));
        model.addAttribute("todayReviews", reviewService.dueOn(today));
        model.addAttribute("todayDate", today);
        model.addAttribute("completedIds", completedIds);
        model.addAttribute("done", done);
        model.addAttribute("total", total);
        model.addAttribute("progress", total == 0 ? 0 : Math.round(done * 100f / total));
        model.addAttribute("streak", completionService.calculateStreak(today));
        model.addAttribute("historyPreview", completionService.history().stream().limit(3).toList());
        return "index";
    }

    @PostMapping("/complete")
    public String complete(@RequestParam Long dareId,
                           @RequestParam(required = false) String note,
                           @RequestParam(required = false) MultipartFile image,
                           RedirectAttributes ra) {
        try {
            completionService.complete(dareService.get(dareId), LocalDate.now(clock), note, image);
            ra.addFlashAttribute("success", "도전을 완료했어요. 오늘의 기록에 저장했습니다 ✨");
        } catch (IllegalArgumentException e) { ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) { ra.addFlashAttribute("error", "기록을 저장하지 못했습니다. 다시 시도해주세요."); }
        return "redirect:/";
    }

    @PostMapping("/complete/undo")
    public String undoComplete(@RequestParam Long dareId, RedirectAttributes ra) {
        try {
            completionService.undo(dareId, LocalDate.now(clock));
            ra.addFlashAttribute("success", "오늘 완료 기록을 취소했어요.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }

    @GetMapping("/challenges")
    public String challenges(Model model) {
        model.addAttribute("challenges", dareService.all());
        return "challenges";
    }

    @GetMapping("/challenges/new")
    public String newChallenge(Model model) {
        if (!model.containsAttribute("form")) model.addAttribute("form", DareRequest.startingOn(LocalDate.now(clock)));
        addFormOptions(model);
        return "challenge-form";
    }

    @GetMapping("/challenges/{id}/edit")
    public String editChallenge(@PathVariable Long id, Model model) {
        if (!model.containsAttribute("form")) model.addAttribute("form", DareRequest.from(dareService.get(id)));
        model.addAttribute("challengeId", id);
        addFormOptions(model);
        return "challenge-form";
    }

    private void addFormOptions(Model model) {
        model.addAttribute("categories", DareCategory.values());
        model.addAttribute("units", DareService.GOAL_UNITS);
    }

    /** One endpoint for create (no id) and edit (id). On error the typed values are sent back to the form. */
    @PostMapping("/challenges/save")
    public String saveChallenge(@RequestParam(required = false) Long id, @ModelAttribute DareRequest form,
                                RedirectAttributes ra) {
        try {
            if (id == null) {
                dareService.create(form);
                ra.addFlashAttribute("success", "새 도전을 만들었습니다.");
            } else {
                dareService.update(id, form);
                ra.addFlashAttribute("success", "도전을 수정했습니다.");
            }
            return "redirect:/challenges";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("form", form);
            return id == null ? "redirect:/challenges/new" : "redirect:/challenges/" + id + "/edit";
        }
    }

    @PostMapping("/challenges/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        Dare dare = dareService.toggle(id);
        ra.addFlashAttribute("success", dare.isActive() ? "도전을 다시 활성화했습니다." : "도전을 잠시 쉬도록 설정했습니다.");
        return "redirect:/challenges";
    }

    @PostMapping("/challenges/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        dareService.delete(id);
        ra.addFlashAttribute("success", "도전과 관련 기록을 삭제했습니다.");
        return "redirect:/challenges";
    }

    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("history", completionService.history());
        model.addAttribute("streak", completionService.calculateStreak(LocalDate.now(clock)));
        return "history";
    }

    @GetMapping("/calendar")
    public String calendar(@RequestParam(required = false) Integer year,
                           @RequestParam(required = false) Integer month, Model model) {
        LocalDate today = LocalDate.now(clock);
        YearMonth ym = resolveYearMonth(year, month, today);
        LocalDate first = ym.atDay(1);
        LocalDate gridStart = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate gridEnd = ym.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        Map<LocalDate, Long> doneMap = new HashMap<>();
        completionService.between(gridStart, gridEnd).forEach(c -> doneMap.merge(c.getChallengeDate(), 1L, Long::sum));
        Map<LocalDate, Long> reviewPlanMap = new HashMap<>();
        Map<LocalDate, Long> reviewDoneMap = new HashMap<>();
        reviewService.between(gridStart, gridEnd).forEach(r -> {
            reviewPlanMap.merge(r.dueDate(), 1L, Long::sum);
            if (r.completed()) reviewDoneMap.merge(r.dueDate(), 1L, Long::sum);
        });
        List<CalendarDay> days = new ArrayList<>();
        long monthCompletionCount = 0L;
        long monthPlannedCount = 0L;
        for (LocalDate d = gridStart; !d.isAfter(gridEnd); d = d.plusDays(1)) {
            long completed = doneMap.getOrDefault(d, 0L);
            long planned = dareService.plannedCount(d);
            int percentage = planned == 0 ? 0 : (int)Math.min(100, Math.round(completed * 100.0 / planned));
            boolean inSelectedMonth = YearMonth.from(d).equals(ym);
            boolean future = d.isAfter(today);
            if (inSelectedMonth && !future) {
                monthCompletionCount += completed;
                monthPlannedCount += planned;
            }
            days.add(new CalendarDay(d, inSelectedMonth, completed, planned, percentage, d.equals(today), future,
                    reviewPlanMap.getOrDefault(d, 0L), reviewDoneMap.getOrDefault(d, 0L)));
        }
        Map<LocalDate,Long> focusMap=new HashMap<>();
        var focusSessions = focusService.completedBetween(gridStart,gridEnd);
        focusSessions.forEach(s -> focusMap.merge(s.getCompletedDay(), (long) s.getPlannedMinutes(), Long::sum));
        long monthFocusMinutes = focusMap.entrySet().stream()
                .filter(e -> YearMonth.from(e.getKey()).equals(ym) && !e.getKey().isAfter(today))
                .mapToLong(Map.Entry::getValue).sum();
        long monthFocusRounds = focusSessions.stream()
                .filter(s -> s.getCompletedDay() != null && YearMonth.from(s.getCompletedDay()).equals(ym)
                        && !s.getCompletedDay().isAfter(today)).count();
        boolean monthHasElapsed = !ym.atDay(1).isAfter(today) && monthPlannedCount > 0;
        model.addAttribute("monthCompletionCount", monthCompletionCount);
        model.addAttribute("monthPlannedCount", monthPlannedCount);
        model.addAttribute("monthFocusMinutes", monthFocusMinutes);
        model.addAttribute("monthFocusRounds", monthFocusRounds);
        model.addAttribute("monthHasElapsed", monthHasElapsed);
        model.addAttribute("monthRate", monthPlannedCount == 0 ? 0 : Math.min(100, Math.round(monthCompletionCount * 100.0 / monthPlannedCount)));
        model.addAttribute("monthSummaryNote", ym.equals(YearMonth.from(today)) ? "이번 달 · 오늘까지"
                : ym.isAfter(YearMonth.from(today)) ? "아직 시작 전" : "선택한 달 전체");
        model.addAttribute("focusMap",focusMap);
        model.addAttribute("reviewPlanMap", reviewPlanMap);
        model.addAttribute("reviewDoneMap", reviewDoneMap);
        model.addAttribute("days", days);
        model.addAttribute("monthLabel", ym.format(DateTimeFormatter.ofPattern("yyyy년 M월")));
        model.addAttribute("prev", ym.minusMonths(1));
        model.addAttribute("next", ym.plusMonths(1));
        return "calendar";
    }

    @GetMapping("/stats")
    public String stats(Model model) {
        LocalDate today = LocalDate.now(clock);
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        LocalDate monthStart = today.withDayOfMonth(1);
        long weekDone = completionService.countBetween(weekStart, today);
        long monthDone = completionService.countBetween(monthStart, today);
        long weekPlanned = plannedBetween(weekStart, today);
        long monthPlanned = plannedBetween(monthStart, today);

        List<DailyStat> weekBars = new ArrayList<>();
        for (LocalDate d = weekStart; !d.isAfter(weekStart.plusDays(6)); d = d.plusDays(1)) {
            long completed = d.isAfter(today) ? 0 : completionService.forDate(d).size();
            long planned = d.isAfter(today) ? 0 : dareService.plannedCount(d);
            int pct = planned == 0 ? 0 : (int)Math.min(100, Math.round(completed * 100.0 / planned));
            weekBars.add(new DailyStat(d.format(DateTimeFormatter.ofPattern("E", Locale.KOREAN)), completed, planned, pct));
        }

        Map<String, Long> categories = new LinkedHashMap<>();
        completionService.between(monthStart, today).forEach(c -> categories.merge(c.getDare().getCategory(), 1L, Long::sum));
        String topCategory = categories.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("-");
        long maxCategory = categories.values().stream().mapToLong(v -> v).max().orElse(1);
        List<CategoryStat> categoryStats = categories.entrySet().stream()
                .map(e -> new CategoryStat(e.getKey(), e.getValue(), (int)Math.round(e.getValue() * 100.0 / maxCategory))).toList();

        model.addAttribute("streak", completionService.calculateStreak(today));
        model.addAttribute("focusToday",focusService.minutesBetween(today,today));
        model.addAttribute("focusWeek",focusService.minutesBetween(weekStart,today));
        model.addAttribute("focusMonth",focusService.minutesBetween(monthStart,today));
        model.addAttribute("focusWeekCount",focusService.completedBetween(weekStart,today).size());
        model.addAttribute("focusMonthCount",focusService.completedBetween(monthStart,today).size());
        Map<String,Long> focusPerCategory=new LinkedHashMap<>();
        focusService.completedBetween(monthStart,today).forEach(s->focusPerCategory.merge(s.getDare().getCategory(),(long)s.getPlannedMinutes(),Long::sum));
        model.addAttribute("focusPerCategory",focusPerCategory);
        model.addAttribute("weekDone", weekDone);
        model.addAttribute("monthDone", monthDone);
        model.addAttribute("weekRate", weekPlanned == 0 ? 0 : Math.round(weekDone * 100.0 / weekPlanned));
        model.addAttribute("monthRate", monthPlanned == 0 ? 0 : Math.round(monthDone * 100.0 / monthPlanned));
        model.addAttribute("topCategory", topCategory);
        model.addAttribute("weekBars", weekBars);
        model.addAttribute("categoryStats", categoryStats);
        return "stats";
    }

    private YearMonth resolveYearMonth(Integer year, Integer month, LocalDate today) {
        if (year == null || month == null) return YearMonth.from(today);
        if (year < today.getYear() - 4 || year > today.getYear() + 1 || month < 1 || month > 12) {
            throw new IllegalArgumentException("달력 조회 범위를 벗어났습니다.");
        }
        return YearMonth.of(year, month);
    }

    private long plannedBetween(LocalDate start, LocalDate end) {
        long total = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) total += dareService.plannedCount(d);
        return total;
    }

    public record CalendarDay(LocalDate date, boolean inMonth, long completed, long planned, int percentage, boolean today, boolean future, long reviewPlanned, long reviewCompleted) {}
    public record DailyStat(String label, long completed, long planned, int percentage) {}
    public record CategoryStat(String category, long count, int percentage) {}
}
