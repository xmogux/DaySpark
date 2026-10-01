package com.dayspark.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dayspark.app.model.Dare;
import com.dayspark.app.service.dto.DareRequest;
import com.dayspark.app.support.DaySparkIntegrationTest;
import com.dayspark.app.support.IntegrationTestSupport;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

@DaySparkIntegrationTest
class DaySparkApplicationTests extends IntegrationTestSupport {

    @Test
    void contextLoads() {}

    @Test
    void mainPagesLoad() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(view().name("index"))
            .andExpect(model().attributeExists("todayDares", "completedIds", "progress", "streak"));
        mvc.perform(get("/challenges")).andExpect(status().isOk()).andExpect(view().name("challenges"));
        mvc.perform(get("/calendar")).andExpect(status().isOk()).andExpect(view().name("calendar"));
        mvc.perform(get("/stats")).andExpect(status().isOk()).andExpect(view().name("stats"));
        mvc.perform(get("/history")).andExpect(status().isOk()).andExpect(view().name("history"));
    }

    @Test
    void createAndEditChallenge() throws Exception {
        long before = dares.count();
        mvc.perform(
            post("/challenges/save")
                .param("title", "알고리즘 1문제")
                .param("description", "매일 한 문제 풀기")
                .param("category", "STUDY")
                .param("minutes", "30")
                .param("repeatType", "CUSTOM")
                .param("repeatDays", TODAY.getDayOfWeek().name())
                .param("startDate", TODAY.toString())
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/challenges"));
        assertThat(dares.count()).isEqualTo(before + 1);
    }

    @Test
    void newAndEditFormsRenderWithTheSavedValues() throws Exception {
        Dare dare = dares.save(new Dare("독서", "30쪽", "STUDY", 20, "CUSTOM", "MONDAY,FRIDAY", TODAY, null));
        mvc.perform(get("/challenges/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("challenge-form"))
            .andExpect(model().attributeExists("form", "categories", "units"));
        mvc.perform(get("/challenges/" + dare.getId() + "/edit"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("challengeId", dare.getId()))
            .andExpect(content().string(containsString("value=\"독서\"")));
    }

    @Test
    void invalidInputIsSentBackToTheFormInsteadOfBeingLost() throws Exception {
        MvcResult result = mvc
            .perform(
                post("/challenges/save")
                    .param("title", "내가 입력한 이름")
                    .param("category", "STUDY")
                    .param("repeatType", "CUSTOM") // no day selected -> validation error
                    .param("startDate", TODAY.toString())
            )
            .andExpect(redirectedUrl("/challenges/new"))
            .andExpect(flash().attribute("error", "반복할 요일을 하나 이상 선택해주세요."))
            .andReturn();
        DareRequest kept = (DareRequest) result.getFlashMap().get("form");
        assertThat(kept.getTitle()).isEqualTo("내가 입력한 이름");
        assertThat(dares.count()).isZero();
    }

    @Test
    void completingScheduledChallengeStoresNoteAndImage() throws Exception {
        Dare dare = dares.save(new Dare("테스트", "오늘 테스트", "STUDY", 5, "DAILY", "", TODAY.minusDays(1), null));
        MockMultipartFile image = new MockMultipartFile(
            "image",
            "proof.png",
            "image/png",
            "fake-png".getBytes(StandardCharsets.UTF_8)
        );
        mvc.perform(
            multipart("/complete").file(image).param("dareId", dare.getId().toString()).param("note", "완료 기록")
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));
        assertThat(completions.findByDareIdAndChallengeDate(dare.getId(), TODAY)).isPresent();
    }

    @Test
    void invalidImageIsRejected() throws Exception {
        Dare dare = dares.save(new Dare("이미지 테스트", "", "LIFE", 5, "DAILY", "", TODAY, null));
        MockMultipartFile image = new MockMultipartFile("image", "bad.exe", "application/octet-stream", new byte[] {
            1,
        });
        mvc.perform(multipart("/complete").file(image).param("dareId", dare.getId().toString()))
            .andExpect(status().is3xxRedirection())
            .andExpect(flash().attribute("error", "JPG, PNG, WEBP, GIF 이미지만 업로드할 수 있습니다."));
    }

    @Test
    void invalidRepeatDayIsRejectedInsteadOfCrashingLater() throws Exception {
        mvc.perform(
            post("/challenges/save")
                .param("title", "깨지는 요일 방지")
                .param("description", "잘못된 요일 입력")
                .param("category", "STUDY")
                .param("minutes", "30")
                .param("repeatType", "CUSTOM")
                .param("repeatDays", "FOO")
                .param("startDate", TODAY.toString())
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(flash().attribute("error", "지원되지 않는 요일 값입니다."));
    }

    @Test
    void invalidCalendarMonthShowsFriendlyErrorPage() throws Exception {
        mvc.perform(get("/calendar").param("year", "2026").param("month", "13"))
            .andExpect(status().isBadRequest())
            .andExpect(view().name("error"))
            .andExpect(model().attributeExists("message"));
    }

    @Test
    void redesignedCalendarExposesMonthSummaryAndDayCards() throws Exception {
        mvc.perform(get("/calendar"))
            .andExpect(status().isOk())
            .andExpect(view().name("calendar"))
            .andExpect(
                model().attributeExists(
                    "monthCompletionCount",
                    "monthPlannedCount",
                    "monthRate",
                    "monthFocusMinutes",
                    "monthFocusRounds",
                    "focusMap",
                    "days"
                )
            )
            .andExpect(content().string(containsString("calendar-v54-grid")))
            .andExpect(content().string(containsString("MONTHLY OVERVIEW")));
    }
}
