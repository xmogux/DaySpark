package com.dayspark.app.controller;

import com.dayspark.app.service.StatsService;
import com.dayspark.app.service.view.CalendarMonth;
import com.dayspark.app.service.view.DayDetail;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Month calendar and the drill-down for one date. Viewable range: 4 years back to 1 year ahead. */
@Controller
public class CalendarController {
    private final StatsService stats;
    private final Clock clock;

    public CalendarController(StatsService stats, Clock clock) {
        this.stats = stats;
        this.clock = clock;
    }

    @GetMapping("/calendar")
    public String month(@RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month,
                        Model model) {
        LocalDate today = LocalDate.now(clock);
        YearMonth ym = resolveYearMonth(year, month, today);
        CalendarMonth view = stats.calendarMonth(ym, today);
        model.addAttribute("monthLabel", StatsService.monthLabel(ym));
        model.addAttribute("prev", ym.minusMonths(1));
        model.addAttribute("next", ym.plusMonths(1));
        model.addAttribute("days", view.days());
        model.addAttribute("focusMap", view.focusMinutesByDay());
        model.addAttribute("monthCompletionCount", view.completionCount());
        model.addAttribute("monthPlannedCount", view.plannedCount());
        model.addAttribute("monthRate", view.rate());
        model.addAttribute("monthHasElapsed", view.hasElapsed());
        model.addAttribute("monthSummaryNote", view.summaryNote());
        model.addAttribute("monthFocusMinutes", view.focusMinutes());
        model.addAttribute("monthFocusRounds", view.focusRounds());
        return "calendar";
    }

    @GetMapping("/calendar/day")
    public String day(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, Model model) {
        LocalDate today = LocalDate.now(clock);
        if (date.isAfter(today.plusYears(1)) || date.isBefore(today.minusYears(4))) {
            throw new IllegalArgumentException("날짜 범위를 벗어났습니다.");
        }
        DayDetail detail = stats.dayDetail(date);
        model.addAttribute("date", date);
        model.addAttribute("scheduled", detail.planned());
        model.addAttribute("completed", detail.completed());
        model.addAttribute("reviews", detail.reviews());
        model.addAttribute("sessions", detail.sessions());
        model.addAttribute("focusMinutes", detail.focusMinutes());
        return "calendar-day";
    }

    private static YearMonth resolveYearMonth(Integer year, Integer month, LocalDate today) {
        if (year == null || month == null) return YearMonth.from(today);
        if (year < today.getYear() - 4 || year > today.getYear() + 1 || month < 1 || month > 12) {
            throw new IllegalArgumentException("달력 조회 범위를 벗어났습니다.");
        }
        return YearMonth.of(year, month);
    }
}
