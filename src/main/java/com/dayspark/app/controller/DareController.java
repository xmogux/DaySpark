package com.dayspark.app.controller;

import com.dayspark.app.domain.DareCategory;
import com.dayspark.app.model.Dare;
import com.dayspark.app.service.DareService;
import com.dayspark.app.service.PlanningService;
import com.dayspark.app.service.ProgressService;
import com.dayspark.app.service.ReviewService;
import com.dayspark.app.service.StatsService;
import com.dayspark.app.service.dto.DareRequest;
import com.dayspark.app.service.view.DareInsights;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Challenge list, create/edit form, detail page and the actions on a single challenge. */
@Controller
public class DareController {

    private final DareService dares;
    private final StatsService stats;
    private final PlanningService planning;
    private final ProgressService progress;
    private final ReviewService reviews;
    private final Clock clock;

    public DareController(
        DareService dares,
        StatsService stats,
        PlanningService planning,
        ProgressService progress,
        ReviewService reviews,
        Clock clock
    ) {
        this.dares = dares;
        this.stats = stats;
        this.planning = planning;
        this.progress = progress;
        this.reviews = reviews;
        this.clock = clock;
    }

    @GetMapping("/challenges")
    public String list(Model model) {
        model.addAttribute("challenges", dares.all());
        return "challenges";
    }

    @GetMapping("/challenges/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("form")) model.addAttribute("form", DareRequest.startingOn(LocalDate.now(clock)));
        addFormOptions(model);
        return "challenge-form";
    }

    @GetMapping("/challenges/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        if (!model.containsAttribute("form")) model.addAttribute("form", DareRequest.from(dares.get(id)));
        model.addAttribute("challengeId", id);
        addFormOptions(model);
        return "challenge-form";
    }

    /** One endpoint for create (no id) and edit (id). On error the typed values are sent back to the form. */
    @PostMapping("/challenges/save")
    public String save(
        @RequestParam(required = false) Long id,
        @ModelAttribute DareRequest form,
        RedirectAttributes ra
    ) {
        try {
            if (id == null) {
                dares.create(form);
                ra.addFlashAttribute("success", "새 도전을 만들었습니다.");
            } else {
                dares.update(id, form);
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
        Dare dare = dares.toggle(id);
        ra.addFlashAttribute(
            "success",
            dare.isActive() ? "도전을 다시 활성화했습니다." : "도전을 잠시 쉬도록 설정했습니다."
        );
        return "redirect:/challenges";
    }

    @PostMapping("/challenges/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        dares.delete(id);
        ra.addFlashAttribute("success", "도전과 관련 기록을 삭제했습니다.");
        return "redirect:/challenges";
    }

    @GetMapping("/challenges/{id}")
    public String detail(@PathVariable Long id, Model model) {
        LocalDate today = LocalDate.now(clock);
        Dare dare = dares.get(id);
        DareInsights insights = stats.dareInsights(dare, today);
        model.addAttribute("dare", dare);
        model.addAttribute("today", today);
        model.addAttribute("units", DareService.GOAL_UNITS);
        model.addAttribute("overrides", planning.forDare(id));
        model.addAttribute("progress", insights.progressToday());
        model.addAttribute("weekDone", insights.weekDone());
        model.addAttribute("monthDone", insights.monthDone());
        model.addAttribute("monthRate", insights.monthRate());
        model.addAttribute("goalStreak", insights.goalStreak());
        model.addAttribute("weekFocus", insights.weekFocusMinutes());
        model.addAttribute("monthFocus", insights.monthFocusMinutes());
        model.addAttribute("focusCount", insights.focusCount());
        model.addAttribute("reviewPlan", insights.reviewPlan());
        model.addAttribute("reviewDone", insights.reviewDone());
        model.addAttribute("reviewTotal", insights.reviewPlan().size());
        model.addAttribute("recentCompletions", insights.recentCompletions());
        return "challenge-detail";
    }

    @PostMapping("/challenges/{id}/goals")
    public String goals(
        @PathVariable Long id,
        @RequestParam int targetValue,
        @RequestParam String targetUnit,
        @RequestParam(defaultValue = "0") int weeklyTarget,
        RedirectAttributes ra
    ) {
        try {
            dares.configureGoals(id, targetValue, targetUnit, weeklyTarget);
            ra.addFlashAttribute("success", "수치·주간 목표를 저장했어요.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/challenges/" + id;
    }

    @PostMapping("/challenges/{id}/progress")
    public String addProgress(@PathVariable Long id, @RequestParam int amount, RedirectAttributes ra) {
        try {
            progress.add(dares.get(id), LocalDate.now(clock), amount);
            ra.addFlashAttribute("success", "진행량을 기록했어요.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/challenges/" + id;
    }

    @PostMapping("/challenges/{id}/schedule")
    public String schedule(
        @PathVariable Long id,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam String mode,
        @RequestParam(defaultValue = "") String note,
        RedirectAttributes ra
    ) {
        try {
            planning.setOverride(dares.get(id), date, mode, note);
            ra.addFlashAttribute("success", "해당 날짜의 일정을 반영했어요.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/challenges/" + id;
    }

    /** Toggles one review. {@code returnTo} sends the user back to where the button was pressed. */
    @PostMapping("/challenges/{id}/reviews/{offset}/toggle")
    public String toggleReview(
        @PathVariable Long id,
        @PathVariable int offset,
        @RequestParam(defaultValue = "detail") String returnTo,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        RedirectAttributes ra
    ) {
        try {
            boolean completed = reviews.toggle(dares.get(id), offset);
            ra.addFlashAttribute("success", completed ? "복습을 완료로 표시했어요." : "복습 완료 표시를 취소했어요.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        if ("home".equals(returnTo)) return "redirect:/";
        if ("calendar".equals(returnTo) && date != null) return "redirect:/calendar/day?date=" + date;
        return "redirect:/challenges/" + id;
    }

    private void addFormOptions(Model model) {
        model.addAttribute("categories", DareCategory.values());
        model.addAttribute("units", DareService.GOAL_UNITS);
    }
}
