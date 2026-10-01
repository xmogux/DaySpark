package com.dayspark.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dayspark.app.model.Completion;
import com.dayspark.app.model.Dare;
import com.dayspark.app.service.ReviewService.ReviewItem;
import com.dayspark.app.support.DaySparkIntegrationTest;
import com.dayspark.app.support.IntegrationTestSupport;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** Executable versions of the rules in docs/domain-rules.md. */
@DaySparkIntegrationTest
class DomainRuleTests extends IntegrationTestSupport {

    /**
     * Review plan rule: the plan belongs to one study unit, not to every repetition. Base date
     * 10/2 with intervals 1,3,7 always yields exactly three reviews (10/3, 10/5, 10/9), even if the
     * DAILY study challenge is completed every day.
     */
    @Test
    void reviewPlanCreatesOneScheduleForTheStudyUnit() throws Exception {
        LocalDate base = LocalDate.of(2026, 10, 2);
        mvc.perform(
            post("/challenges/save")
                .param("title", "운영체제 3장")
                .param("category", "STUDY")
                .param("repeatType", "DAILY")
                .param("startDate", base.toString())
                .param("reviewEnabled", "true")
                .param("reviewBaseDate", base.toString())
                .param("reviewIntervals", "1, 3, 7")
        ).andExpect(flash().attributeExists("success"));
        Dare study = dares.findAll().get(0);
        for (LocalDate day = base; !day.isAfter(TODAY); day = day.plusDays(1)) {
            completions.save(new Completion(study, day, "", null, day.atTime(20, 0)));
        }

        MvcResult detail = mvc
            .perform(get("/challenges/" + study.getId()))
            .andExpect(status().isOk())
            .andReturn();
        @SuppressWarnings("unchecked")
        List<ReviewItem> plan = (List<ReviewItem>) detail.getModelAndView().getModel().get("reviewPlan");

        assertThat(plan)
            .extracting(ReviewItem::dueDate)
            .containsExactly(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 9));
    }

    /** Streak rule: a planned day counts when at least one challenge was completed that day. */
    @Test
    void streakCountsDaysWithAtLeastOneCompletion() throws Exception {
        Dare water = dailyDare("물", "HEALTH");
        dailyDare("독서", "STUDY");
        dailyDare("산책", "MOVE");
        dailyDare("정리", "LIFE");
        for (int i = 0; i < 3; i++) {
            LocalDate day = TODAY.minusDays(i);
            completions.save(new Completion(water, day, "", null, day.atTime(9, 0)));
        }
        // 1 of 4 challenges done on each of the last three days (the challenges started 3 days ago).
        mvc.perform(get("/")).andExpect(model().attribute("streak", 3));
    }

    /** Streak rule: unfinished today does not break the streak; a missed planned day does. */
    @Test
    void streakIgnoresUnfinishedTodayButBreaksOnAMissedDay() throws Exception {
        Dare water = dailyDare("물", "HEALTH");
        LocalDate yesterday = TODAY.minusDays(1);
        LocalDate threeDaysAgo = TODAY.minusDays(3);
        completions.save(new Completion(water, yesterday, "", null, yesterday.atTime(9, 0)));
        completions.save(new Completion(water, threeDaysAgo, "", null, threeDaysAgo.atTime(9, 0)));
        // Today not done yet, yesterday done, two days ago missed.
        mvc.perform(get("/")).andExpect(model().attribute("streak", 1));
    }

    /**
     * Numeric goal rule: the daily target is a guide, not a gate. Manual completion is allowed even
     * when the logged progress is below the target.
     */
    @Test
    void manualCompletionIsAllowedBelowTheNumericTarget() throws Exception {
        Dare reading = dailyDare("책 읽기", "STUDY");
        String id = reading.getId().toString();
        mvc.perform(
            post("/challenges/" + id + "/goals")
                .param("targetValue", "20")
                .param("targetUnit", "페이지")
        ).andExpect(flash().attributeExists("success"));
        mvc.perform(post("/challenges/" + id + "/progress").param("amount", "3")).andExpect(
            flash().attributeExists("success")
        );

        mvc.perform(post("/complete").param("dareId", id)).andExpect(flash().attributeExists("success"));

        assertThat(completions.findByDareIdAndChallengeDate(reading.getId(), TODAY)).isPresent();
    }
}
