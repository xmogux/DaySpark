package com.dayspark.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dayspark.app.model.Completion;
import com.dayspark.app.model.Dare;
import com.dayspark.app.support.DaySparkIntegrationTest;
import com.dayspark.app.support.IntegrationTestSupport;
import java.time.DayOfWeek;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** One test per bug found in the v5.5 review. Each test was committed before its fix. */
@DaySparkIntegrationTest
class RegressionTests extends IntegrationTestSupport {

    /**
     * Bug: after a challenge's repeat rule changed, past completions were still counted but the
     * planned count was recomputed from the new rule, so the weekly rate showed 200%.
     */
    @Test
    void ratesStayAtOrBelow100PercentAfterRepeatRuleChange() throws Exception {
        LocalDate monday = TODAY.with(DayOfWeek.MONDAY);
        Dare a = dares.save(new Dare("A", "", "LIFE", 5, "DAILY", "", monday, null));
        Dare b = dares.save(new Dare("B", "", "LIFE", 5, "DAILY", "", monday, null));
        for (LocalDate day = monday; !day.isAfter(TODAY); day = day.plusDays(1)) {
            completions.save(new Completion(a, day, "", null, day.atTime(9, 0)));
            completions.save(new Completion(b, day, "", null, day.atTime(9, 0)));
        }

        mvc.perform(post("/challenges/save")
                        .param("id", b.getId().toString())
                        .param("title", "B")
                        .param("category", "LIFE")
                        .param("repeatType", "WEEKENDS")
                        .param("startDate", monday.toString()))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("/stats"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("weekRate", 100L))
                .andExpect(model().attribute("monthRate", 100L))
                .andExpect(model().attribute("weekDone", 6L));
    }
}
