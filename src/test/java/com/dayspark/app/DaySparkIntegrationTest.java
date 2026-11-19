package com.dayspark.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dayspark.app.model.Dare;
import com.dayspark.app.model.FocusSession;
import com.dayspark.app.model.ProgressLog;
import com.dayspark.app.repository.CompletionRepository;
import com.dayspark.app.repository.DareRepository;
import com.dayspark.app.repository.FocusSessionRepository;
import com.dayspark.app.repository.ProgressLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Two cross-layer checks with real MVC controllers, services, JPA repositories and isolated H2.
 * The normal user's file database and demo data must never be touched by these tests.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:dayspark_integration;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.seed-demo-data=false",
    "app.upload-dir=target/test-uploads/integration"
})
@AutoConfigureMockMvc
class DaySparkIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired DareRepository dares;
    @Autowired FocusSessionRepository sessions;
    @Autowired ProgressLogRepository progress;
    @Autowired CompletionRepository completions;
    @Autowired Clock clock;

    @Test
    void creatingChallengeThroughMvcPersistsItAndShowsItInList() throws Exception {
        String title = "통합검증 도전 생성";

        mvc.perform(post("/challenges/save")
                .param("title", title)
                .param("category", "STUDY")
                .param("minutes", "25")
                .param("repeatType", "DAILY")
                .param("startDate", LocalDate.now(clock).toString())
                .param("targetValue", "1")
                .param("targetUnit", "회"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/challenges"));

        assertThat(dares.findAll()).extracting(Dare::getTitle).contains(title);
        mvc.perform(get("/challenges"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(title)));
    }

    @Test
    void finishingTimerWritesProgressAndAutoCompletesExactlyOnce() throws Exception {
        Dare dare = new Dare(
            "집중 자동완료 검증", "", "STUDY", 1, "DAILY", "",
            LocalDate.now(clock).minusDays(3), null
        );
        dare.setGoals(1, "분", 0);
        Dare saved = dares.saveAndFlush(dare);

        // Concurrent starts and switches must observe the single-active-session rule.
        // A one-minute session started two minutes ago: no real sleeping needed.
        FocusSession session = sessions.saveAndFlush(
            new FocusSession(saved, 1, Instant.now(clock).minusSeconds(120))
        );
        LocalDate completedDay = session.expectedCompletedDay(clock.getZone());

        mvc.perform(post("/api/focus/start")
                .param("dareId", saved.getId().toString()).param("minutes", "1"))
            .andExpect(status().isBadRequest());

        mvc.perform(post("/api/focus/{id}/finish", session.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));

        assertThat(progress.findByDareIdAndDay(saved.getId(), completedDay))
            .extracting(ProgressLog::getAmount)
            .containsExactly(1);
        assertThat(completions.findByDareIdAndChallengeDate(saved.getId(), completedDay)).isPresent();

        // The same finish request must not add another progress or completion.
        mvc.perform(post("/api/focus/{id}/finish", session.getId()))
            .andExpect(status().isOk());
        assertThat(progress.findByDareIdAndDay(saved.getId(), completedDay)).hasSize(1);
        assertThat(completions.findByDareIdAndChallengeDate(saved.getId(), completedDay)).isPresent();
    }
}
