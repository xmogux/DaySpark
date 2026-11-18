package com.dayspark.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dayspark.app.model.Dare;
import com.dayspark.app.model.FocusSession;
import com.dayspark.app.repository.CompletionRepository;
import com.dayspark.app.repository.DareRepository;
import com.dayspark.app.repository.FocusSessionRepository;
import com.dayspark.app.repository.ProgressLogRepository;
import com.dayspark.app.repository.ReviewRecordRepository;
import com.dayspark.app.repository.ScheduleExceptionRepository;
import com.dayspark.app.service.PlanningService;
import com.dayspark.app.service.ReviewService;
import com.dayspark.app.service.StatsService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Four additional integration scenarios, using real MVC -> service -> JPA -> isolated H2.
 * No user's data files or external services are used.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:dayspark_more_integration;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.seed-demo-data=false",
    "app.upload-dir=target/test-uploads/more-integration"
})
@AutoConfigureMockMvc
class DaySparkMoreIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired DareRepository dares;
    @Autowired CompletionRepository completions;
    @Autowired ProgressLogRepository progress;
    @Autowired FocusSessionRepository sessions;
    @Autowired ScheduleExceptionRepository scheduleExceptions;
    @Autowired ReviewRecordRepository reviewRecords;
    @Autowired PlanningService planning;
    @Autowired ReviewService reviews;
    @Autowired StatsService stats;
    @Autowired Clock clock;

    private Dare newDare(String title, String targetUnit, int target) {
        Dare dare = new Dare(
            title, "", "STUDY", 25, "DAILY", "",
            LocalDate.now(clock).minusDays(7), null
        );
        dare.setGoals(target, targetUnit, 0);
        return dares.saveAndFlush(dare);
    }

    @Test
    void scheduleExceptionUpdatesHomeAndCanBeReset() throws Exception {
        LocalDate today = LocalDate.now(clock);
        Dare dare = newDare("IT03 일정 예외 반영", "회", 1);
        String id = dare.getId().toString();

        mvc.perform(get("/")).andExpect(status().isOk())
            .andExpect(content().string(containsString(dare.getTitle())));

        mvc.perform(post("/challenges/{id}/schedule", id)
                .param("date", today.toString()).param("mode", "SKIP"))
            .andExpect(status().is3xxRedirection());
        assertThat(planning.isScheduled(dare, today)).isFalse();
        mvc.perform(get("/")).andExpect(status().isOk())
            .andExpect(content().string(not(containsString(dare.getTitle()))));

        mvc.perform(post("/challenges/{id}/schedule", id)
                .param("date", today.toString()).param("mode", "ADD"))
            .andExpect(status().is3xxRedirection());
        assertThat(planning.isScheduled(dare, today)).isTrue();
        mvc.perform(get("/")).andExpect(content().string(containsString(dare.getTitle())));

        mvc.perform(post("/challenges/{id}/schedule", id)
                .param("date", today.toString()).param("mode", "RESET"))
            .andExpect(status().is3xxRedirection());
        assertThat(scheduleExceptions.findByDareIdAndDay(dare.getId(), today)).isEmpty();
        assertThat(planning.isScheduled(dare, today)).isTrue();
    }

    @Test
    void undoCompletionUpdatesPersistenceHistoryAndStatistics() throws Exception {
        LocalDate today = LocalDate.now(clock);
        Dare dare = newDare("IT04 완료 취소 통계", "회", 1);
        long monthBefore = stats.overview(today).monthDone();

        mvc.perform(post("/complete").param("dareId", dare.getId().toString())
                .param("note", "integration-undo-marker"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
        assertThat(completions.findByDareIdAndChallengeDate(dare.getId(), today)).isPresent();
        assertThat(stats.overview(today).monthDone()).isEqualTo(monthBefore + 1);
        mvc.perform(get("/history")).andExpect(status().isOk())
            .andExpect(content().string(containsString("integration-undo-marker")));

        mvc.perform(post("/complete/undo").param("dareId", dare.getId().toString()))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
        assertThat(completions.findByDareIdAndChallengeDate(dare.getId(), today)).isEmpty();
        assertThat(stats.overview(today).monthDone()).isEqualTo(monthBefore);
        mvc.perform(get("/history")).andExpect(status().isOk())
            .andExpect(content().string(not(containsString("integration-undo-marker"))));
    }

    @Test
    void reviewTogglePersistsAndIsReflectedInPlan() throws Exception {
        Dare dare = newDare("IT05 복습 토글", "회", 1);
        dare.configureReview(true, LocalDate.now(clock).minusDays(1), "1,3,7");
        dare = dares.saveAndFlush(dare);
        Long id = dare.getId();

        assertThat(reviews.planFor(dare)).hasSize(3);
        assertThat(reviews.planFor(dare).get(0).completed()).isFalse();
        mvc.perform(post("/challenges/{id}/reviews/1/toggle", id))
            .andExpect(status().is3xxRedirection());
        assertThat(reviewRecords.findByDareIdAndOffsetDays(id, 1)).isPresent();
        assertThat(reviews.planFor(dare).get(0).completed()).isTrue();

        mvc.perform(get("/challenges/{id}", id))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("완료 취소")));

        mvc.perform(post("/challenges/{id}/reviews/1/toggle", id))
            .andExpect(status().is3xxRedirection());
        assertThat(reviewRecords.findByDareIdAndOffsetDays(id, 1)).isEmpty();
        assertThat(reviews.planFor(dare).get(0).completed()).isFalse();
    }

    @Test
    void deletingDareRemovesAllRelatedRecords() throws Exception {
        LocalDate today = LocalDate.now(clock);
        Dare dare = newDare("IT06 연관 데이터 삭제", "페이지", 10);
        dare.configureReview(true, today.minusDays(1), "1,3,7");
        dare = dares.saveAndFlush(dare);
        Long id = dare.getId();

        mvc.perform(post("/complete").param("dareId", id.toString()))
            .andExpect(status().is3xxRedirection());
        mvc.perform(post("/challenges/{id}/progress", id).param("amount", "2"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(post("/challenges/{id}/schedule", id)
                .param("date", today.plusDays(1).toString()).param("mode", "SKIP"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(post("/challenges/{id}/reviews/1/toggle", id))
            .andExpect(status().is3xxRedirection());

        FocusSession focus = sessions.saveAndFlush(
            new FocusSession(dare, 25, Instant.now(clock).minusSeconds(30))
        );
        Long focusId = focus.getId();
        assertThat(completions.findByDareIdAndChallengeDate(id, today)).isPresent();
        assertThat(progress.findByDareIdAndDay(id, today)).hasSize(1);
        assertThat(scheduleExceptions.findByDareIdAndDay(id, today.plusDays(1))).isPresent();
        assertThat(reviewRecords.findByDareIdAndOffsetDays(id, 1)).isPresent();
        assertThat(sessions.findById(focusId)).isPresent();

        mvc.perform(post("/challenges/{id}/delete", id))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/challenges"));

        assertThat(dares.findById(id)).isEmpty();
        assertThat(completions.findByDareIdAndChallengeDate(id, today)).isEmpty();
        assertThat(progress.findByDareIdAndDay(id, today)).isEmpty();
        assertThat(scheduleExceptions.findByDareIdAndDay(id, today.plusDays(1))).isEmpty();
        assertThat(reviewRecords.findByDareIdAndOffsetDays(id, 1)).isEmpty();
        assertThat(sessions.findById(focusId)).isEmpty();
    }
}
