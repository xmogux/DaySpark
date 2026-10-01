package com.dayspark.app.service;

import com.dayspark.app.domain.FocusStatus;
import com.dayspark.app.model.Dare;
import com.dayspark.app.model.FocusSession;
import com.dayspark.app.repository.FocusSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FocusService {

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
        if (minutes < 1 || minutes > 120) throw new IllegalArgumentException("집중 시간은 1~120분으로 설정해주세요.");
        if (!dare.isActive()) throw new IllegalArgumentException("쉬는 중인 도전입니다.");
        if (current() != null) throw new IllegalArgumentException("이미 진행 중인 집중 세션이 있습니다.");
        return repo.save(new FocusSession(dare, minutes, Instant.now(clock)));
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

    @Transactional
    public FocusSession cancel(Long id) {
        FocusSession session = get(id);
        session.cancel(Instant.now(clock));
        return repo.save(session);
    }

    public List<FocusSession> completedBetween(LocalDate from, LocalDate to) {
        return repo.findByCompletedDayBetweenAndStatus(from, to, FocusStatus.COMPLETED.name());
    }

    public List<FocusSession> completedFor(Dare dare) {
        return repo.findByDareIdAndStatus(dare.getId(), FocusStatus.COMPLETED.name());
    }

    public long minutesBetween(LocalDate from, LocalDate to) {
        return completedBetween(from, to).stream().mapToLong(FocusSession::getPlannedMinutes).sum();
    }
}
