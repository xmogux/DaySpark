package com.dayspark.app.support;

import com.dayspark.app.model.Dare;
import com.dayspark.app.repository.CompletionRepository;
import com.dayspark.app.repository.DareRepository;
import com.dayspark.app.repository.FocusSessionRepository;
import com.dayspark.app.repository.ProgressLogRepository;
import com.dayspark.app.repository.ReviewRecordRepository;
import com.dayspark.app.repository.ScheduleExceptionRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Shared state for integration tests: empty database and the clock reset to {@link TestDates#TODAY}
 * before every test. Subclasses must be annotated with {@link DaySparkIntegrationTest}.
 */
public abstract class IntegrationTestSupport {

    protected static final LocalDate TODAY = TestDates.TODAY;

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected MutableClock clock;

    @Autowired
    protected DareRepository dares;

    @Autowired
    protected CompletionRepository completions;

    @Autowired
    protected ProgressLogRepository progressLogs;

    @Autowired
    protected ScheduleExceptionRepository exceptions;

    @Autowired
    protected FocusSessionRepository focusSessions;

    @Autowired
    protected ReviewRecordRepository reviewRecords;

    @BeforeEach
    protected void resetState() {
        clock.setInstant(TestDates.TODAY_NOON);
        reviewRecords.deleteAll();
        focusSessions.deleteAll();
        progressLogs.deleteAll();
        exceptions.deleteAll();
        completions.deleteAll();
        dares.deleteAll();
    }

    /** A DAILY challenge that started a few days before {@link #TODAY}. */
    protected Dare dailyDare(String title, String category) {
        return dares.save(new Dare(title, "", category, 30, "DAILY", "", TODAY.minusDays(3), null));
    }
}
