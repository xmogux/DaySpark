package com.dayspark.app.controller;

import com.dayspark.app.model.FocusSession;
import com.dayspark.app.service.DareService;
import com.dayspark.app.service.FocusService;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/focus")
public class FocusApi {

    private static final Logger log = LoggerFactory.getLogger(FocusApi.class);

    private final FocusService focus;
    private final DareService dares;
    private final Clock clock;

    public FocusApi(FocusService focus, DareService dares, Clock clock) {
        this.focus = focus;
        this.dares = dares;
        this.clock = clock;
    }

    /** {@code focusedMinutes}: whole minutes actually focused so far (pauses excluded). */
    public record SessionInfo(
        Long id,
        Long dareId,
        String dareTitle,
        String status,
        int durationMinutes,
        long remainingMillis,
        int focusedMinutes,
        Instant serverTime
    ) {}

    /** Answer of /switch: the session that was ended and recorded (null if none) and the new one. */
    public record SwitchInfo(SessionInfo stopped, SessionInfo started) {}

    private SessionInfo info(FocusSession session) {
        Instant now = Instant.now(clock);
        return new SessionInfo(
            session.getId(),
            session.getDare().getId(),
            session.getDare().getTitle(),
            session.getStatus(),
            session.getPlannedMinutes(),
            session.remainingMillis(now),
            (int) (session.focusedMillis(now) / 60_000),
            now
        );
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

    /** "종료 및 기록": end now, keep the focused time, not a completed round. */
    @PostMapping("/{id}/stop")
    SessionInfo stop(@PathVariable Long id) {
        return info(focus.stop(id));
    }

    /** End the active session (recorded) and start one for another challenge in one step. */
    @PostMapping("/switch")
    SwitchInfo switchTo(@RequestParam Long dareId, @RequestParam int minutes) {
        FocusService.Switch result = focus.switchTo(dares.get(dareId), minutes);
        return new SwitchInfo(result.stopped() == null ? null : info(result.stopped()), info(result.started()));
    }

    /** Discards the session without recording time. Not used by the page any more. */
    @PostMapping("/{id}/cancel")
    SessionInfo cancel(@PathVariable Long id) {
        return info(focus.cancel(id));
    }

    @GetMapping("/summary")
    Object summary() {
        LocalDate today = LocalDate.now(clock);
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        // Minutes include sessions ended early; counts are fully completed sessions only.
        return Map.of(
            "todayMinutes",
            focus.minutesBetween(today, today),
            "todayCount",
            focus.completedBetween(today, today).size(),
            "weekMinutes",
            focus.minutesBetween(weekStart, today),
            "weekCount",
            focus.completedBetween(weekStart, today).size()
        );
    }

    /** Timer rule violations (e.g. "not finished yet") are shown to the user as they are. */
    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    ResponseEntity<Map<String, String>> rejected(RuntimeException e) {
        log.warn("Focus API rejected: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler({ MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class })
    ResponseEntity<Map<String, String>> malformed(Exception e) {
        log.warn("Focus API malformed request: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "도전과 집중 시간을 확인해주세요."));
    }

    /** Always JSON: the timer page parses every response. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, String>> unexpected(Exception e) {
        log.error("Focus API failed", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            Map.of("error", "서버가 요청을 처리하지 못했습니다.")
        );
    }
}
