package com.dayspark.app.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ReviewIntervalsTest {

    @Test
    void inputIsSortedAndDeduplicated() {
        ReviewIntervals intervals = ReviewIntervals.parse("7, 1, 3, 7");
        assertThat(intervals.offsets()).containsExactly(1, 3, 7);
        assertThat(intervals.normalized()).isEqualTo("1,3,7");
    }

    @Test
    void spacesAndFullWidthCommasAreAccepted() {
        assertThat(ReviewIntervals.parse("1 3，14").normalized()).isEqualTo("1,3,14");
    }

    @Test
    void emptyOrNonNumericInputIsRejectedWithAUserMessage() {
        assertThatThrownBy(() -> ReviewIntervals.parse(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("하나 이상");
        assertThatThrownBy(() -> ReviewIntervals.parse("1, 사흘"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("숫자");
    }

    @Test
    void valuesMustBeBetweenOneDayAndTenYears() {
        assertThat(ReviewIntervals.parse("1,3650").offsets()).containsExactly(1, 3650);
        assertThatThrownBy(() -> ReviewIntervals.parse("0")).hasMessageContaining("1일~3650일");
        assertThatThrownBy(() -> ReviewIntervals.parse("3651")).hasMessageContaining("1일~3650일");
    }

    @Test
    void atMostTwelveReviews() {
        assertThat(ReviewIntervals.parse("1,2,3,4,5,6,7,8,9,10,11,12").size()).isEqualTo(12);
        assertThatThrownBy(() -> ReviewIntervals.parse("1,2,3,4,5,6,7,8,9,10,11,12,13"))
                .hasMessageContaining("최대 12개");
    }

    @Test
    void storedEmptyValueMeansNoPlan() {
        assertThat(ReviewIntervals.ofStored("").size()).isZero();
        assertThat(ReviewIntervals.ofStored(null).offsets()).isEmpty();
        assertThat(ReviewIntervals.ofStored("1,3,7").contains(3)).isTrue();
        assertThat(ReviewIntervals.ofStored("1,3,7").contains(2)).isFalse();
    }
}
