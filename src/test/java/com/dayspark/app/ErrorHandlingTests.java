package com.dayspark.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dayspark.app.support.DaySparkIntegrationTest;
import com.dayspark.app.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

/**
 * HTTP error mapping. Before: one @ExceptionHandler(Exception.class) turned a missing page into
 * 500, a wrong HTTP method into 500, and the JSON API answered some errors with an HTML page.
 */
@DaySparkIntegrationTest
class ErrorHandlingTests extends IntegrationTestSupport {

    @Test
    void unknownPageIs404NotAServerError() throws Exception {
        mvc.perform(get("/no-such-page")).andExpect(status().isNotFound()).andExpect(view().name("error"));
    }

    @Test
    void wrongHttpMethodIs405() throws Exception {
        mvc.perform(post("/stats")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void malformedFormInputGetsAFriendlyMessage() throws Exception {
        mvc.perform(
            post("/challenges/save").param("title", "x").param("minutes", "열").param("startDate", TODAY.toString())
        )
            .andExpect(status().isBadRequest())
            .andExpect(view().name("error"))
            .andExpect(model().attribute("message", "날짜, 숫자 형식이나 필수 입력값을 확인해주세요."));
    }

    @Test
    void focusApiAnswersMissingParametersWithJson() throws Exception {
        mvc.perform(post("/api/focus/start"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists());
    }
}
