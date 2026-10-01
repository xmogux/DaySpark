package com.dayspark.app;
import com.dayspark.app.model.Dare;
import com.dayspark.app.model.FocusSession;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
class FocusSessionUnitTests {
 @Test void pauseResumeAndCompletionRequireFullElapsedTime(){
  Dare d=new Dare("알고리즘","","STUDY",25,"DAILY","",LocalDate.of(2026,1,1),null);
  Instant t=Instant.parse("2026-09-01T00:00:00Z");
  FocusSession s=new FocusSession(d,1,t);
  assertThat(s.remainingMillis(t.plusSeconds(20))).isEqualTo(40000);
  s.pause(t.plusSeconds(20));
  assertThat(s.remainingMillis(t.plusSeconds(60))).isEqualTo(40000);
  s.resume(t.plusSeconds(50));
  assertThat(s.remainingMillis(t.plusSeconds(80))).isEqualTo(10000);
  assertThatThrownBy(()->s.complete(t.plusSeconds(80),LocalDate.of(2026,9,1)))
   .isInstanceOf(IllegalStateException.class);
  s.complete(t.plusSeconds(90),LocalDate.of(2026,9,1));
  assertThat(s.getStatus()).isEqualTo("COMPLETED");
 }
 @Test void cancellationNeverBecomesCompletion(){
  Dare d=new Dare("독서","","STUDY",25,"DAILY","",LocalDate.of(2026,1,1),null);
  Instant start=Instant.parse("2026-09-01T00:00:00Z");
  FocusSession s=new FocusSession(d,1,start);
  s.cancel(start.plusSeconds(12));
  assertThat(s.getStatus()).isEqualTo("CANCELLED");
  assertThatThrownBy(()->s.complete(start.plusSeconds(120),LocalDate.of(2026,9,1)))
   .isInstanceOf(IllegalStateException.class);
 }
 @Test void completedDayUsesActualExpectedFinishTimeAcrossMidnight(){
  Dare d=new Dare("심야 공부","","STUDY",25,"DAILY","",LocalDate.of(2026,1,1),null);
  Instant start=Instant.parse("2026-09-01T14:50:00Z"); // 23:50 Asia/Seoul
  FocusSession s=new FocusSession(d,25,start);
  assertThat(s.expectedCompletedDay(ZoneId.of("Asia/Seoul"))).isEqualTo(LocalDate.of(2026,9,2));
 }
}
