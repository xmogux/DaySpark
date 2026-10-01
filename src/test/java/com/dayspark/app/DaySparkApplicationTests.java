package com.dayspark.app;

import com.dayspark.app.model.Dare;
import com.dayspark.app.repository.CompletionRepository;
import com.dayspark.app.repository.DareRepository;
import com.dayspark.app.repository.FocusSessionRepository;
import com.dayspark.app.repository.ProgressLogRepository;
import com.dayspark.app.repository.ScheduleExceptionRepository;
import com.dayspark.app.repository.ReviewRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DaySparkApplicationTests {
    @Autowired MockMvc mockMvc;
    @Autowired CompletionRepository completionRepository;
    @Autowired DareRepository dareRepository;
    @Autowired FocusSessionRepository focusRepository;
    @Autowired ProgressLogRepository progressRepository;
    @Autowired ScheduleExceptionRepository exceptionRepository;
    @Autowired ReviewRecordRepository reviewRepository;

    @BeforeEach
    void cleanDatabase() {
        reviewRepository.deleteAll();
        focusRepository.deleteAll();
        progressRepository.deleteAll();
        exceptionRepository.deleteAll();
        completionRepository.deleteAll();
        dareRepository.deleteAll();
    }

    @Test void contextLoads() {}

    @Test
    void mainPagesLoad() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("index"))
                .andExpect(model().attributeExists("todayDares", "completedIds", "progress", "streak"));
        mockMvc.perform(get("/challenges")).andExpect(status().isOk()).andExpect(view().name("challenges"));
        mockMvc.perform(get("/calendar")).andExpect(status().isOk()).andExpect(view().name("calendar"));
        mockMvc.perform(get("/stats")).andExpect(status().isOk()).andExpect(view().name("stats"));
        mockMvc.perform(get("/history")).andExpect(status().isOk()).andExpect(view().name("history"));
    }

    @Test
    void createAndEditChallenge() throws Exception {
        long before = dareRepository.count();
        mockMvc.perform(post("/challenges/save")
                        .param("title", "알고리즘 1문제")
                        .param("description", "매일 한 문제 풀기")
                        .param("category", "STUDY")
                        .param("minutes", "30")
                        .param("repeatType", "CUSTOM")
                        .param("repeatDays", LocalDate.now().getDayOfWeek().name())
                        .param("startDate", LocalDate.now().toString()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/challenges"));
        assertThat(dareRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void completingScheduledChallengeStoresNoteAndImage() throws Exception {
        Dare dare = dareRepository.save(new Dare("테스트", "오늘 테스트", "STUDY", 5,
                "DAILY", "", LocalDate.now().minusDays(1), null));
        MockMultipartFile image = new MockMultipartFile("image", "proof.png", "image/png",
                "fake-png".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/complete").file(image)
                        .param("dareId", dare.getId().toString())
                        .param("note", "완료 기록"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
        assertThat(completionRepository.findByDareIdAndChallengeDate(dare.getId(), LocalDate.now())).isPresent();
    }

    @Test
    void invalidImageIsRejected() throws Exception {
        Dare dare = dareRepository.save(new Dare("이미지 테스트", "", "LIFE", 5,
                "DAILY", "", LocalDate.now(), null));
        MockMultipartFile image = new MockMultipartFile("image", "bad.exe", "application/octet-stream", new byte[]{1});
        mockMvc.perform(multipart("/complete").file(image).param("dareId", dare.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "JPG, PNG, WEBP, GIF 이미지만 업로드할 수 있습니다."));
    }

    @Test
    void invalidRepeatDayIsRejectedInsteadOfCrashingLater() throws Exception {
        mockMvc.perform(post("/challenges/save")
                        .param("title", "깨지는 요일 방지")
                        .param("description", "잘못된 요일 입력")
                        .param("category", "STUDY")
                        .param("minutes", "30")
                        .param("repeatType", "CUSTOM")
                        .param("repeatDays", "FOO")
                        .param("startDate", LocalDate.now().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "지원되지 않는 요일 값입니다."));
    }

    @Test
    void invalidCalendarMonthShowsFriendlyErrorPage() throws Exception {
        mockMvc.perform(get("/calendar").param("year", "2026").param("month", "13"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("message"));
    }

    @Test
    void redesignedCalendarExposesMonthSummaryAndDayCards() throws Exception {
        mockMvc.perform(get("/calendar"))
                .andExpect(status().isOk())
                .andExpect(view().name("calendar"))
                .andExpect(model().attributeExists(
                        "monthCompletionCount", "monthPlannedCount", "monthRate",
                        "monthFocusMinutes", "monthFocusRounds", "focusMap", "days"))
                .andExpect(content().string(containsString("calendar-v54-grid")))
                .andExpect(content().string(containsString("MONTHLY OVERVIEW")));
    }
}
