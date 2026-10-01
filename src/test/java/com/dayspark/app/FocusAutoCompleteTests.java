package com.dayspark.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dayspark.app.model.Dare;
import com.dayspark.app.model.FocusSession;
import com.dayspark.app.support.DaySparkIntegrationTest;
import com.dayspark.app.support.IntegrationTestSupport;
import com.dayspark.app.support.TestDates;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Pomodoro -> numeric goal -> completion. Rules (docs/domain-rules.md, "포모도로 연결"):
 * focus minutes count only for '분' goals on a planned day; reaching the target for the first time
 * completes the challenge; finishing a session never fails because of auto-completion.
 */
@DaySparkIntegrationTest
class FocusAutoCompleteTests extends IntegrationTestSupport {

    private Dare minutesGoal(String repeatType, String repeatDays, int targetMinutes) throws Exception {
        Dare dare = dares.save(new Dare("집중 공부", "", "STUDY", 25, repeatType, repeatDays, TODAY.minusDays(7), null));
        mvc.perform(post("/challenges/" + dare.getId() + "/goals")
                        .param("targetValue", String.valueOf(targetMinutes))
                        .param("targetUnit", "분"))
                .andExpect(flash().attributeExists("success"));
        return dare;
    }

    private Long focus(Dare dare, int minutes) throws Exception {
        mvc.perform(post("/api/focus/start").param("dareId", dare.getId().toString()).param("minutes", String.valueOf(minutes)))
                .andExpect(status().isOk());
        Long id = focusSessions.findByStatusIn(java.util.List.of("RUNNING")).get(0).getId();
        clock.advance(Duration.ofMinutes(minutes));
        mvc.perform(post("/api/focus/" + id + "/finish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        return id;
    }

    private int progressOn(Dare dare, LocalDate day) {
        return progressLogs.findByDareIdAndDay(dare.getId(), day).stream().mapToInt(p -> p.getAmount()).sum();
    }

    @Test
    void focusMinutesReachingTheTargetCompleteTheChallenge() throws Exception {
        Dare dare = minutesGoal("DAILY", "", 25);

        Long session = focus(dare, 25);

        assertThat(progressOn(dare, TODAY)).isEqualTo(25);
        assertThat(completions.findByDareIdAndChallengeDate(dare.getId(), TODAY)).isPresent();
        // finish is idempotent: a repeated call from the browser does not fail.
        mvc.perform(post("/api/focus/" + session + "/finish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void manualProgressReachingTheTargetCompletesTheChallenge() throws Exception {
        Dare reading = dares.save(new Dare("책 읽기", "", "STUDY", 20, "DAILY", "", TODAY.minusDays(1), null));
        mvc.perform(post("/challenges/" + reading.getId() + "/goals").param("targetValue", "20").param("targetUnit", "페이지"));

        mvc.perform(post("/challenges/" + reading.getId() + "/progress").param("amount", "15"));
        assertThat(completions.findByDareIdAndChallengeDate(reading.getId(), TODAY)).isEmpty();

        mvc.perform(post("/challenges/" + reading.getId() + "/progress").param("amount", "5"));
        assertThat(completions.findByDareIdAndChallengeDate(reading.getId(), TODAY)).isPresent();
    }

    @Test
    void autoCompletionHappensOnlyWhenTheTargetIsFirstReached() throws Exception {
        Dare dare = minutesGoal("DAILY", "", 25);
        focus(dare, 25);
        mvc.perform(post("/complete/undo").param("dareId", dare.getId().toString()))
                .andExpect(flash().attributeExists("success"));

        focus(dare, 25); // 50/25: the target was already reached earlier today

        assertThat(progressOn(dare, TODAY)).isEqualTo(50);
        assertThat(completions.findByDareIdAndChallengeDate(dare.getId(), TODAY)).isEmpty();
    }

    @Test
    void focusOnAnUnplannedDayFinishesNormallyWithoutProgressOrCompletion() throws Exception {
        Dare weekendOnly = minutesGoal("WEEKENDS", "", 25); // TODAY is a Wednesday

        focus(weekendOnly, 25);

        assertThat(progressOn(weekendOnly, TODAY)).isZero();
        assertThat(completions.findByDareIdAndChallengeDate(weekendOnly.getId(), TODAY)).isEmpty();
        assertThat(focusSessions.findByStatusOrderByStartedAtDesc("COMPLETED")).hasSize(1);
    }

    @Test
    void sessionCrossingMidnightFinishesOnTheNextDayAndIsNotLeftRunning() throws Exception {
        Dare wednesdayOnly = minutesGoal("CUSTOM", "WEDNESDAY", 25);
        clock.setInstant(TestDates.at(TODAY, 23, 50));

        focus(wednesdayOnly, 25); // ends Thursday 00:15, a day this challenge is not planned

        LocalDate thursday = TODAY.plusDays(1);
        FocusSession finished = focusSessions.findByStatusOrderByStartedAtDesc("COMPLETED").get(0);
        assertThat(finished.getCompletedDay()).isEqualTo(thursday);
        assertThat(progressOn(wednesdayOnly, thursday)).isZero();
        assertThat(completions.findByDareIdAndChallengeDate(wednesdayOnly.getId(), thursday)).isEmpty();
        mvc.perform(get("/api/focus/current")).andExpect(jsonPath("$.active").value(false));
    }
}
