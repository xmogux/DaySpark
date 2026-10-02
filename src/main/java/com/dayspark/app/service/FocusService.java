package com.dayspark.app.service;

import com.dayspark.app.domain.FocusStatus;
import com.dayspark.app.model.Dare;
import com.dayspark.app.model.FocusSession;
import com.dayspark.app.repository.FocusSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FocusService {

    /** Sessions whose focus time counts in the statistics. */
    private static final List<String> RECORDED = List.of(FocusStatus.COMPLETED.name(), FocusStatus.STOPPED.name());

    /** Result of {@link #switchTo}: the session that was ended (null if none) and the new one. */
    public record Switch(FocusSession stopped, FocusSession started) {}

    private final FocusSessionRepository repo;
    private final ProgressService progress;
    private final Clock clock;

    public FocusService(FocusSessionRepository repo, ProgressService progress, Clock clock) {
        this.repo = repo;
        this.progress = progress;
        this.clock = clock;
    }

    public List<FocusSession> active() {
        return repo.findByStatusIn(List.of(FocusStatus.RUNNING.name(), FocusStatus.PAUSED.name()));
    }

    /**
     * Read-only lookup. Completion is deliberately handled by POST /finish, not by GET /current.
     */
    public FocusSession current() {
        return active().stream().findFirst().orElse(null);
    }

    @Transactional
    public FocusSession start(Dare dare, int minutes) {
        checkCanStart(dare, minutes);
        if (current() != null) throw new IllegalArgumentException("이미 진행 중인 집중 세션이 있습니다.");
        return repo.save(new FocusSession(dare, minutes, Instant.now(clock)));
    }

    /**
     * Ends the active session (if any) with its focus time recorded, then starts a session for
     * {@code dare}. The new session is validated first, so a rejected switch leaves the running
     * session untouched.
     */
    @Transactional
    public Switch switchTo(Dare dare, int minutes) {
        checkCanStart(dare, minutes);
        FocusSession running = current();
        if (running != null && running.getDare().getId().equals(dare.getId())) {
            throw new IllegalArgumentException("이미 이 도전으로 집중 중입니다.");
        }
        Instant now = Instant.now(clock);
        FocusSession stopped = running == null ? null : stopInternal(running, now);
        return new Switch(stopped, repo.save(new FocusSession(dare, minutes, now)));
    }

    private static void checkCanStart(Dare dare, int minutes) {
        if (minutes < 1 || minutes > 120) throw new IllegalArgumentException("집중 시간은 1~120분으로 설정해주세요.");
        if (!dare.isActive()) throw new IllegalArgumentException("쉬는 중인 도전입니다.");
    }

    public FocusSession get(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));
    }

    @Transactional
    public FocusSession pause(Long id) {
        FocusSession session = get(id);
        session.pause(Instant.now(clock));
        return repo.save(session);
    }

    @Transactional
    public FocusSession resume(Long id) {
        FocusSession session = get(id);
        session.resume(Instant.now(clock));
        return repo.save(session);
    }

    @Transactional
    public FocusSession finish(Long id) {
        FocusSession session = get(id);
        if (FocusStatus.COMPLETED.name().equals(session.getStatus())) return session;
        return finishInternal(session, Instant.now(clock));
    }

    private FocusSession finishInternal(FocusSession session, Instant now) {
        session.complete(now, clock.getZone());
        repo.save(session);
        progress.focusEarned(session.getDare(), session.getCompletedDay(), session.getPlannedMinutes());
        return session;
    }

    /**
     * "종료 및 기록": ends the session now and keeps the time actually focused. If the planned time
     * had already run out (for example the tab was closed), it is a full session and completes
     * normally. Calling it again on an ended session returns it unchanged.
     */
    @Transactional
    public FocusSession stop(Long id) {
        FocusSession session = get(id);
        if (!FocusStatus.parse(session.getStatus()).isActive()) return session;
        return stopInternal(session, Instant.now(clock));
    }

    private FocusSession stopInternal(FocusSession session, Instant now) {
        if (session.remainingMillis(now) == 0) return finishInternal(session, now);
        session.stop(now, clock.getZone());
        return repo.save(session);
    }

    /** Discards the session without recording any time (kept for the API; the UI uses stop). */
    @Transactional
    public FocusSession cancel(Long id) {
        FocusSession session = get(id);
        session.cancel(Instant.now(clock));
        return repo.save(session);
    }

    /** Fully completed sessions: these are the "완료 횟수". */
    public List<FocusSession> completedBetween(LocalDate from, LocalDate to) {
        return repo.findByCompletedDayBetweenAndStatus(from, to, FocusStatus.COMPLETED.name());
    }

    /** Completed and stopped sessions: everything whose focus time counts. */
    public List<FocusSession> recordedBetween(LocalDate from, LocalDate to) {
        return repo.findByCompletedDayBetweenAndStatusIn(from, to, RECORDED);
    }

    public List<FocusSession> recordedFor(Dare dare) {
        return repo.findByDareIdAndStatusIn(dare.getId(), RECORDED);
    }

    /** Focus minutes actually spent between the two dates, including sessions ended early. */
    public long minutesBetween(LocalDate from, LocalDate to) {
        return totalMinutes(recordedBetween(from, to));
    }

    /**
     * Each session counts its whole minutes ({@link FocusSession#getFocusedMinutes}), so every total
     * on every screen is the plain sum of the per-session numbers shown in the day detail.
     */
    public static long totalMinutes(Collection<FocusSession> sessions) {
        return sessions.stream().mapToLong(FocusSession::getFocusedMinutes).sum();
    }
}
