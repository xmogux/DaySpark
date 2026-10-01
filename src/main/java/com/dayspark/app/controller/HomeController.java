package com.dayspark.app.controller;

import com.dayspark.app.service.CompletionService;
import com.dayspark.app.service.DareService;
import com.dayspark.app.service.FocusService;
import com.dayspark.app.service.ReviewService;
import com.dayspark.app.service.StatsService;
import com.dayspark.app.service.view.DayPlan;
import com.dayspark.app.service.view.TodayBoard;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Today's screen (complete / undo) and the completion history. */
@Controller
public class HomeController {
    private static final DateTimeFormatter TODAY_LABEL = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN);

    private final StatsService stats;
    private final CompletionService completions;
    private final DareService dares;
    private final FocusService focus;
    private final ReviewService reviews;
    private final Clock clock;

    public HomeController(StatsService stats, CompletionService completions, DareService dares, FocusService focus,
                          ReviewService reviews, Clock clock) {
        this.stats = stats;
        this.completions = completions;
        this.dares = dares;
        this.focus = focus;
        this.reviews = reviews;
        this.clock = clock;
    }

    @GetMapping("/")
    public String home(Model model) {
        LocalDate today = LocalDate.now(clock);
        TodayBoard board = stats.today(today);
        DayPlan plan = board.plan();
        model.addAttribute("today", today.format(TODAY_LABEL));
        model.addAttribute("todayDate", today);
        model.addAttribute("todayDares", plan.planned());
        model.addAttribute("completedIds", plan.completedIds());
        model.addAttribute("done", plan.done());
        model.addAttribute("total", plan.total());
        model.addAttribute("progress", plan.percent());
        model.addAttribute("dailyProgress", board.progressToday());
        model.addAttribute("weeklyCounts", board.completionsThisWeek());
        model.addAttribute("streak", board.streak());
        model.addAttribute("todayFocus", focus.minutesBetween(today, today));
        model.addAttribute("todayReviews", reviews.dueOn(today));
        model.addAttribute("historyPreview", completions.history().stream().limit(3).toList());
        return "index";
    }

    @PostMapping("/complete")
    public String complete(@RequestParam Long dareId, @RequestParam(required = false) String note,
                           @RequestParam(required = false) MultipartFile image, RedirectAttributes ra) {
        try {
            completions.complete(dares.get(dareId), LocalDate.now(clock), note, image);
            ra.addFlashAttribute("success", "도전을 완료했어요. 오늘의 기록에 저장했습니다 ✨");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (IOException e) {
            ra.addFlashAttribute("error", "사진을 저장하지 못했습니다. 다시 시도해주세요.");
        }
        return "redirect:/";
    }

    @PostMapping("/complete/undo")
    public String undoComplete(@RequestParam Long dareId, RedirectAttributes ra) {
        try {
            completions.undo(dareId, LocalDate.now(clock));
            ra.addFlashAttribute("success", "오늘 완료 기록을 취소했어요.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }

    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("history", completions.history());
        model.addAttribute("streak", stats.overallStreak(LocalDate.now(clock)));
        return "history";
    }
}
