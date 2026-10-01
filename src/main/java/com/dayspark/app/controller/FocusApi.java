package com.dayspark.app.controller;

import com.dayspark.app.model.FocusSession;
import com.dayspark.app.service.DareService;
import com.dayspark.app.service.FocusService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/focus")
public class FocusApi {
    private final FocusService focus;
    private final DareService dares;
    private final Clock clock;

    public FocusApi(FocusService focus, DareService dares, Clock clock) {
        this.focus = focus;
        this.dares = dares;
        this.clock = clock;
    }

    public record SessionInfo(Long id, Long dareId, String dareTitle, String status,
                              int durationMinutes, long remainingMillis, Instant serverTime) {}

    private SessionInfo info(FocusSession session) {
        Instant now = Instant.now(clock);
        return new SessionInfo(session.getId(), session.getDare().getId(), session.getDare().getTitle(),
                session.getStatus(), session.getPlannedMinutes(), session.remainingMillis(now), now);
    }

    @GetMapping("/current")
    Object current() {
        FocusSession session = focus.current();
        return session == null ? Map.of("active", false) : info(session);
    }

    @PostMapping("/start")
    SessionInfo start(@RequestParam Long dareId, @RequestParam int minutes) {
        return info(focus.start(dares.get(dareId), minutes));
    }

    @PostMapping("/{id}/pause")
    SessionInfo pause(@PathVariable Long id) {
        return info(focus.pause(id));
    }

    @PostMapping("/{id}/resume")
    SessionInfo resume(@PathVariable Long id) {
        return info(focus.resume(id));
    }

    @PostMapping("/{id}/finish")
    SessionInfo finish(@PathVariable Long id) {
        return info(focus.finish(id));
    }

    @PostMapping("/{id}/cancel")
    SessionInfo cancel(@PathVariable Long id) {
        return info(focus.cancel(id));
    }

    @GetMapping("/summary")
    Object summary() {
        LocalDate today = LocalDate.now(clock);
        var sessions = focus.completedBetween(today.with(DayOfWeek.MONDAY), today);
        return Map.of(
                "todayMinutes", focus.minutesBetween(today, today),
                "todayCount", focus.completedBetween(today, today).size(),
                "weekMinutes", sessions.stream().mapToInt(FocusSession::getPlannedMinutes).sum(),
                "weekCount", sessions.size());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    ResponseEntity<Map<String, String>> bad(Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }
}
