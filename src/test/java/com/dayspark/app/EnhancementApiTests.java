package com.dayspark.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dayspark.app.model.Dare;
import com.dayspark.app.support.DaySparkIntegrationTest;
import com.dayspark.app.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

@DaySparkIntegrationTest
class EnhancementApiTests extends IntegrationTestSupport {

    private Dare make() {
        return dares.save(new Dare("알고리즘 학습", "30분 실습", "STUDY", 30, "DAILY", "", TODAY.minusDays(3), null));
    }

    @Test
    void newPagesRender() throws Exception {
        Dare d = make();
        mvc.perform(get("/focus")).andExpect(status().isOk()).andExpect(view().name("focus"));
        mvc.perform(get("/challenges/" + d.getId()))
            .andExpect(status().isOk())
            .andExpect(view().name("challenge-detail"));
        mvc.perform(get("/calendar/day").param("date", TODAY.toString()))
            .andExpect(status().isOk())
            .andExpect(view().name("calendar-day"));
    }

    @Test
    void goalsProgressAndWeekAreSaved() throws Exception {
        Dare d = make();
        mvc.perform(
            post("/challenges/" + d.getId() + "/goals")
                .param("targetValue", "45")
                .param("targetUnit", "분")
                .param("weeklyTarget", "4")
        ).andExpect(status().is3xxRedirection());
        Dare updated = dares.findById(d.getId()).orElseThrow();
        assertThat(updated.getTargetValue()).isEqualTo(45);
        assertThat(updated.getTargetUnit()).isEqualTo("분");
        assertThat(updated.getWeeklyTarget()).isEqualTo(4);

        mvc.perform(post("/challenges/" + d.getId() + "/progress").param("amount", "15")).andExpect(
            status().is3xxRedirection()
        );
        assertThat(progressLogs.findByDareIdAndDay(d.getId(), TODAY)).extracting("amount").contains(15);
    }

    @Test
    void skipOverrideRemovesTodayAndBlocksCompletion() throws Exception {
        Dare d = make();
        mvc.perform(
            post("/challenges/" + d.getId() + "/schedule")
                .param("date", TODAY.toString())
                .param("mode", "SKIP")
                .param("note", "휴식")
        ).andExpect(status().is3xxRedirection());

        // Compare ids: Dare has no equals(), so hasItem(d) on freshly loaded entities is always false
        // and the old assertion passed even when SKIP did nothing.
        MvcResult home = mvc.perform(get("/")).andExpect(status().isOk()).andReturn();
        @SuppressWarnings("unchecked")
        List<Dare> todayDares = (List<Dare>) home.getModelAndView().getModel().get("todayDares");
        assertThat(todayDares).extracting(Dare::getId).doesNotContain(d.getId());

        mvc.perform(post("/complete").param("dareId", d.getId().toString()))
            .andExpect(status().is3xxRedirection())
            .andExpect(flash().attributeExists("error"));
    }

    @Test
    void focusApiCanStartPauseResumeCancel() throws Exception {
        Dare d = make();
        mvc.perform(post("/api/focus/start").param("dareId", d.getId().toString()).param("minutes", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RUNNING"));
        Long id = focusSessions.findByStatusIn(List.of("RUNNING")).get(0).getId();
        mvc.perform(post("/api/focus/" + id + "/pause"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAUSED"));
        mvc.perform(post("/api/focus/" + id + "/resume"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RUNNING"));
        mvc.perform(post("/api/focus/" + id + "/finish")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/focus/" + id + "/cancel"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(focusSessions.findByStatusOrderByStartedAtDesc("COMPLETED")).isEmpty();
    }

    @Test
    void studyReviewPlanCanBeConfiguredAndCompleted() throws Exception {
        mvc.perform(
            post("/challenges/save")
                .param("title", "운영체제 Chapter 3")
                .param("description", "복습 플랜 테스트")
                .param("category", "STUDY")
                .param("minutes", "30")
                .param("targetValue", "1")
                .param("targetUnit", "회")
                .param("weeklyTarget", "0")
                .param("repeatType", "DAILY")
                .param("startDate", TODAY.toString())
                .param("reviewEnabled", "true")
                .param("reviewBaseDate", TODAY.toString())
                .param("reviewIntervals", "7, 1, 3, 7")
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/challenges"));
        Dare d = dares
            .findAll()
            .stream()
            .filter(x -> x.getTitle().equals("운영체제 Chapter 3"))
            .findFirst()
            .orElseThrow();
        assertThat(d.isReviewEnabled()).isTrue();
        assertThat(d.getReviewIntervals()).isEqualTo("1,3,7");

        mvc.perform(get("/challenges/" + d.getId()))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("reviewPlan", "reviewDone", "reviewTotal"));
        mvc.perform(post("/challenges/" + d.getId() + "/reviews/1/toggle"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/challenges/" + d.getId()));
        assertThat(reviewRecords.findByDareIdAndOffsetDays(d.getId(), 1)).isPresent();
    }
}
