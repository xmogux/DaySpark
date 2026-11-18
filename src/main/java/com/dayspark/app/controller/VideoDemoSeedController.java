package com.dayspark.app.controller;

import com.dayspark.app.model.Dare;
import com.dayspark.app.repository.DareRepository;
import com.dayspark.app.service.CompletionService;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Temporary demonstration-only fixture endpoint.
 * Not active in regular DaySpark: requires --app.video-demo=true.
 * Creates explicitly fictional SAMPLE records so the real calendar/stats UI
 * can illustrate its features without misrepresenting actual user history.
 */
@RestController
@ConditionalOnProperty(name = "app.video-demo", havingValue = "true")
public class VideoDemoSeedController {
    private final DareRepository dares;
    private final CompletionService completions;
    private final Clock clock;

    public VideoDemoSeedController(DareRepository dares, CompletionService completions, Clock clock) {
        this.dares = dares;
        this.completions = completions;
        this.clock = clock;
    }

    @PostMapping("/demo-video/seed")
    public Map<String, Object> seed() throws IOException {
        LocalDate today = LocalDate.now(clock);
        LocalDate started = today.minusDays(21);
        String[][] definitions = {
            {"딥러닝 강의 복습", "역전파와 경사하강법을 복습합니다.", "STUDY", "2", "회"},
            {"PyTorch CNN 실습", "CNN을 직접 구현하고 학습합니다.", "STUDY", "2", "회"},
            {"Transformer 논문 읽기", "어텐션 핵심 수식을 정리합니다.", "STUDY", "8", "페이지"},
            {"저녁 산책 20분", "매일 저녁 20분 걷기", "MOVE", "20", "분"},
            {"오늘 배운 내용 요약", "핵심 개념을 한 줄로 정리합니다.", "LIFE", "1", "회"}
        };
        List<Dare> goals = new java.util.ArrayList<>();
        for (String[] def : definitions) {
            Dare dare = new Dare(def[0], def[1], def[2], 25, "DAILY", "", started, null);
            dare.setGoals(Integer.parseInt(def[3]), def[4], 4);
            goals.add(dares.save(dare));
        }
        int n = 0;
        String[] examples = {
            "경사하강법 업데이트 복습", "CNN 합성곱 연습문제 해결",
            "Scaled Dot-Product Attention 읽기", "산책과 스트레칭 완료",
            "오늘의 개념 정리 작성"
        };
        // Spread approximately 30 sample completions over two weeks.
        // Never add them for today: the live interactions are recorded separately.
        for (int daysAgo = 14; daysAgo >= 1; daysAgo--) {
            LocalDate day = today.minusDays(daysAgo);
            for (int i = 0; i < goals.size(); i++) {
                boolean include = (daysAgo + i * 3) % 4 != 0 && ((daysAgo + i) % 3 != 0);
                if (include) {
                    completions.complete(goals.get(i), day, "[시연용 예시] " + examples[i], null);
                    n++;
                }
            }
        }
        return Map.of("sampleRecords", n, "sampleGoals", goals.size(), "today", today.toString());
    }
}
