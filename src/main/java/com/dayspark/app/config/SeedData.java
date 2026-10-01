package com.dayspark.app.config;

import com.dayspark.app.model.Dare;
import com.dayspark.app.repository.DareRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

@Configuration
public class SeedData {
    @Bean
    CommandLineRunner seedDares(DareRepository repository) {
        return args -> {
            if (repository.count() > 0) return;
            LocalDate today = LocalDate.now();
            Dare water = new Dare("물 6잔 마시기", "하루 동안 천천히 물 6잔을 채워보세요.", "HEALTH", 5, "DAILY", "", today.minusDays(30), null);
            water.setGoals(6, "잔", 6);
            Dare reading = new Dare("책 5페이지 읽기", "부담 없이 5페이지만 읽고 핵심 한 줄을 남겨보세요.", "STUDY", 15, "WEEKDAYS", "", today.minusDays(30), null);
            reading.setGoals(5, "페이지", 4);
            Dare walking = new Dare("20분 산책하기", "휴대폰은 잠시 넣어두고 주변을 걸어보세요.", "MOVE", 20, "CUSTOM", "MONDAY,WEDNESDAY,FRIDAY", today.minusDays(30), null);
            walking.setGoals(20, "분", 3);
            Dare review = new Dare("이번 주 정리하기", "이번 주에 끝낸 일과 다음 주에 할 일을 10분 동안 정리해보세요.", "LIFE", 10, "CUSTOM", "SUNDAY", today.minusDays(30), null);
            review.setGoals(10, "분", 1);
            repository.saveAll(List.of(water, reading, walking, review));
        };
    }
}
