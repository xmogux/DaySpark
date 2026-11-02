package com.dayspark.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

import com.dayspark.app.model.Dare;
import com.dayspark.app.model.FocusSession;
import com.dayspark.app.repository.FocusSessionRepository;
import com.dayspark.app.service.FocusService;
import com.dayspark.app.service.ProgressService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Service-level unit tests using Mockito: no Spring context or database. */
class FocusServiceUnitTests {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Instant START = Instant.parse("2026-09-01T03:00:00Z");

    private static Clock atMinutes(int minutesAfterStart) {
        return Clock.fixed(START.plusSeconds(minutesAfterStart * 60L), SEOUL);
    }

    @Test
    void rejectedSwitchDoesNotChangeCurrentSession() {
        FocusSessionRepository repo = mock(FocusSessionRepository.class);
        ProgressService progress = mock(ProgressService.class);
        Dare currentDare = mock(Dare.class);
        when(currentDare.isActive()).thenReturn(true);
        when(currentDare.getId()).thenReturn(7L);

        FocusSession current = new FocusSession(currentDare, 25, START);
        when(repo.findByStatusIn(anyCollection())).thenReturn(List.of(current));
        FocusService service = new FocusService(repo, progress, atMinutes(5));

        assertThatThrownBy(() -> service.switchTo(currentDare, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("1~120분");
        assertThatThrownBy(() -> service.switchTo(currentDare, 25))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("이미 이 도전");

        assertThat(current.getStatus()).isEqualTo("RUNNING");
        assertThat(current.getEndedAt()).isNull();
        assertThat(current.getPausedAt()).isNull();
        verify(repo, never()).save(any(FocusSession.class));
        verifyNoInteractions(progress);
    }

    @Test
    void onlyCompletedSessionEarnsGoalProgress() {
        FocusSessionRepository repo = mock(FocusSessionRepository.class);
        ProgressService progress = mock(ProgressService.class);
        Dare dare = new Dare("공부", "", "STUDY", 25, "DAILY", "",
            LocalDate.of(2026, 1, 1), null);
        dare.setGoals(25, "분", 0);

        FocusSession early = new FocusSession(dare, 25, START);
        FocusSession full = new FocusSession(dare, 25, START);
        when(repo.findById(1L)).thenReturn(Optional.of(early));
        when(repo.findById(2L)).thenReturn(Optional.of(full));
        when(repo.save(any(FocusSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        new FocusService(repo, progress, atMinutes(10)).stop(1L);
        assertThat(early.getStatus()).isEqualTo("STOPPED");
        verifyNoInteractions(progress);

        FocusService finishing = new FocusService(repo, progress, atMinutes(25));
        finishing.finish(2L);
        assertThat(full.getStatus()).isEqualTo("COMPLETED");
        verify(progress).focusEarned(same(dare), eq(LocalDate.of(2026, 9, 1)), eq(25));

        // Repeated finish requests must not grant progress twice.
        finishing.finish(2L);
        verifyNoMoreInteractions(progress);
        verify(repo, times(2)).save(any(FocusSession.class));
    }
}
