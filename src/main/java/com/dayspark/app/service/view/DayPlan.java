package com.dayspark.app.service.view;

import com.dayspark.app.model.Dare;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** The challenges planned for one day (see docs/domain-rules.md, "하루의 계획") and which are done. */
public record DayPlan(LocalDate date, List<Dare> planned, Set<Long> completedIds) {
    public int total() {
        return planned.size();
    }

    public int done() {
        return (int) planned.stream().filter(d -> completedIds.contains(d.getId())).count();
    }

    /** 0-100; 0 when nothing is planned. */
    public int percent() {
        return total() == 0 ? 0 : Math.round(done() * 100f / total());
    }
}
