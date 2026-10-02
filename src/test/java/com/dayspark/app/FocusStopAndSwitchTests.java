package com.dayspark.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dayspark.app.model.Dare;
import com.dayspark.app.model.FocusSession;
import com.dayspark.app.support.DaySparkIntegrationTest;
import com.dayspark.app.support.IntegrationTestSupport;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * "실제 집중한 시간" vs "끝까지 완료한 세션" (docs/domain-rules.md 7절).
 * Ending a session early keeps the minutes actually focused (pauses excluded) but does not count
 * as a completed session, adds no goal progress and never auto-completes the challenge.
 */
@DaySparkIntegrationTest
class FocusStopAndSwitchTests extends IntegrationTestSupport {

    private Long start(Dare dare, int minutes) throws Exception {
        mvc.perform(
            post("/api/focus/start")
                .param("dareId", dare.getId().toString())
                .param("minutes", "" + minutes)
        ).andExpect(status().isOk());
        return focusSessions.findByStatusIn(List.of("RUNNING")).get(0).getId();
    }

    private void minutesGoal(Dare dare, int target) throws Exception {
        mvc.perform(
            post("/challenges/" + dare.getId() + "/goals")
                .param("targetValue", "" + target)
                .param("targetUnit", "분")
        ).andExpect(flash().attributeExists("success"));
    }

    private void advanceMinutes(long minutes) {
        clock.advance(Duration.ofMinutes(minutes));
    }

    /** Test 1: 25-minute timer, 10 minutes of focus, then stop -> 10 minutes are recorded. */
    @Test
    void stoppingEarlyKeepsTheMinutesActuallyFocused() throws Exception {
        Dare study = dailyDare("알고리즘", "STUDY");
        Long id = start(study, 25);
        advanceMinutes(10);

        mvc.perform(post("/api/focus/" + id + "/stop"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("STOPPED"))
            .andExpect(jsonPath("$.focusedMinutes").value(10));

        mvc.perform(get("/api/focus/summary"))
            .andExpect(jsonPath("$.todayMinutes").value(10))
            .andExpect(jsonPath("$.weekMinutes").value(10));
        mvc.perform(get("/stats")).andExpect(model().attribute("focusToday", 10L));
        mvc.perform(get("/")).andExpect(model().attribute("todayFocus", 10L));
        mvc.perform(get("/api/focus/current")).andExpect(jsonPath("$.active").value(false));
    }

    /** Test 2: a stopped session is not a completed session and never completes the challenge. */
    @Test
    void stoppedSessionIsNotCountedAsCompletedAndDoesNotAutoComplete() throws Exception {
        Dare study = dailyDare("알고리즘", "STUDY");
        minutesGoal(study, 10);
        Long id = start(study, 25);
        advanceMinutes(12);

        mvc.perform(post("/api/focus/" + id + "/stop")).andExpect(status().isOk());

        mvc.perform(get("/api/focus/summary"))
            .andExpect(jsonPath("$.todayCount").value(0))
            .andExpect(jsonPath("$.weekCount").value(0));
        mvc.perform(get("/stats"))
            .andExpect(model().attribute("focusWeekCount", 0))
            .andExpect(model().attribute("focusWeek", 12L));
        mvc.perform(get("/calendar"))
            .andExpect(model().attribute("monthFocusRounds", 0L))
            .andExpect(model().attribute("monthFocusMinutes", 12L));
        mvc.perform(get("/challenges/" + study.getId()))
            .andExpect(model().attribute("focusCount", 0))
            .andExpect(model().attribute("weekFocus", 12L));
        // 12 minutes >= the 10-minute goal, but an unfinished session adds no progress and no completion.
        assertThat(progressLogs.findByDareIdAndDay(study.getId(), TODAY)).isEmpty();
        assertThat(completions.findByDareIdAndChallengeDate(study.getId(), TODAY)).isEmpty();
    }

    /** Test 3: a full 25-minute session still counts minutes and one completed round. */
    @Test
    void completedSessionCountsMinutesAndOneRound() throws Exception {
        Dare study = dailyDare("알고리즘", "STUDY");
        Long id = start(study, 25);
        advanceMinutes(25);

        mvc.perform(post("/api/focus/" + id + "/finish"))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.focusedMinutes").value(25));

        mvc.perform(get("/api/focus/summary"))
            .andExpect(jsonPath("$.todayMinutes").value(25))
            .andExpect(jsonPath("$.todayCount").value(1));
        mvc.perform(get("/stats"))
            .andExpect(model().attribute("focusToday", 25L))
            .andExpect(model().attribute("focusWeekCount", 1));
        mvc.perform(get("/calendar")).andExpect(model().attribute("monthFocusRounds", 1L));
    }

    /** Test 4: time spent paused is never focus time. */
    @Test
    void pausedTimeIsExcludedFromFocusTime() throws Exception {
        Dare study = dailyDare("알고리즘", "STUDY");
        Long id = start(study, 25);
        advanceMinutes(5);
        mvc.perform(post("/api/focus/" + id + "/pause")).andExpect(status().isOk());
        advanceMinutes(30);
        mvc.perform(post("/api/focus/" + id + "/resume")).andExpect(status().isOk());
        advanceMinutes(3);

        mvc.perform(post("/api/focus/" + id + "/stop")).andExpect(jsonPath("$.focusedMinutes").value(8));
    }

    /** Test 4b: stopping while paused records only the time before the pause. */
    @Test
    void stoppingWhilePausedRecordsOnlyTheTimeBeforeThePause() throws Exception {
        Dare study = dailyDare("알고리즘", "STUDY");
        Long id = start(study, 25);
        advanceMinutes(7);
        mvc.perform(post("/api/focus/" + id + "/pause")).andExpect(status().isOk());
        advanceMinutes(60);

        mvc.perform(post("/api/focus/" + id + "/stop"))
            .andExpect(jsonPath("$.status").value("STOPPED"))
            .andExpect(jsonPath("$.focusedMinutes").value(7));
    }

    /** Test 5: switching to another challenge records the old session and starts a new one. */
    @Test
    void switchingToAnotherChallengeRecordsTheOldSessionAndStartsANewOne() throws Exception {
        Dare algorithms = dailyDare("알고리즘", "STUDY");
        Dare reading = dailyDare("독서", "STUDY");
        Long first = start(algorithms, 25);
        advanceMinutes(6);

        mvc.perform(post("/api/focus/switch").param("dareId", reading.getId().toString()).param("minutes", "25"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stopped.dareId").value(algorithms.getId().intValue()))
            .andExpect(jsonPath("$.stopped.status").value("STOPPED"))
            .andExpect(jsonPath("$.stopped.focusedMinutes").value(6))
            .andExpect(jsonPath("$.started.dareId").value(reading.getId().intValue()))
            .andExpect(jsonPath("$.started.status").value("RUNNING"));

        FocusSession old = focusSessions.findById(first).orElseThrow();
        assertThat(old.getStatus()).isEqualTo("STOPPED");
        mvc.perform(get("/api/focus/current"))
            .andExpect(jsonPath("$.dareId").value(reading.getId().intValue()))
            .andExpect(jsonPath("$.status").value("RUNNING"));
        mvc.perform(get("/api/focus/summary")).andExpect(jsonPath("$.todayMinutes").value(6));
    }

    /** Switching without an active session simply starts the new one. */
    @Test
    void switchingWithoutAnActiveSessionJustStarts() throws Exception {
        Dare reading = dailyDare("독서", "STUDY");

        mvc.perform(post("/api/focus/switch").param("dareId", reading.getId().toString()).param("minutes", "25"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stopped").doesNotExist())
            .andExpect(jsonPath("$.started.status").value("RUNNING"));
    }

    /**
     * Stopping after the planned time already ran out (e.g. the tab was closed) is a full session:
     * it completes normally, including goal progress and auto-completion.
     */
    @Test
    void stoppingAfterTheTimeRanOutCompletesTheSession() throws Exception {
        Dare study = dailyDare("알고리즘", "STUDY");
        minutesGoal(study, 25);
        Long id = start(study, 25);
        advanceMinutes(40);

        mvc.perform(post("/api/focus/" + id + "/stop"))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.focusedMinutes").value(25));

        assertThat(completions.findByDareIdAndChallengeDate(study.getId(), TODAY)).isPresent();
    }
}
