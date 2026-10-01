package com.dayspark.app.controller;

import com.dayspark.app.service.StatsService;
import com.dayspark.app.service.view.StatsOverview;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Weekly and monthly statistics page. */
@Controller
public class StatsController {

    private final StatsService stats;
    private final Clock clock;

    public StatsController(StatsService stats, Clock clock) {
        this.stats = stats;
        this.clock = clock;
    }

    @GetMapping("/stats")
    public String stats(Model model) {
        StatsOverview o = stats.overview(LocalDate.now(clock));
        model.addAttribute("streak", o.streak());
        model.addAttribute("weekDone", o.weekDone());
        model.addAttribute("monthDone", o.monthDone());
        model.addAttribute("weekRate", o.weekRate());
        model.addAttribute("monthRate", o.monthRate());
        model.addAttribute("topCategory", o.topCategory());
        model.addAttribute("weekBars", o.weekBars());
        model.addAttribute("categoryStats", o.categoryStats());
        model.addAttribute("focusToday", o.focusToday());
        model.addAttribute("focusWeek", o.focusWeek());
        model.addAttribute("focusMonth", o.focusMonth());
        model.addAttribute("focusWeekCount", o.focusWeekCount());
        model.addAttribute("focusMonthCount", o.focusMonthCount());
        model.addAttribute("focusPerCategory", o.focusPerCategory());
        return "stats";
    }
}
