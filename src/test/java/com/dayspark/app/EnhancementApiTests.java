package com.dayspark.app;
import com.dayspark.app.model.Dare;
import com.dayspark.app.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
@AutoConfigureMockMvc
class EnhancementApiTests {
 @Autowired MockMvc mvc;
 @Autowired DareRepository dares;
 @Autowired CompletionRepository completions;
 @Autowired ProgressLogRepository progress;
 @Autowired ScheduleExceptionRepository exceptions;
 @Autowired FocusSessionRepository focus;
 @Autowired ReviewRecordRepository reviews;
 @BeforeEach void clean(){
  reviews.deleteAll();focus.deleteAll();progress.deleteAll();exceptions.deleteAll();completions.deleteAll();dares.deleteAll();
 }
 private Dare make(){return dares.save(new Dare("알고리즘 학습","30분 실습","STUDY",30,"DAILY","",LocalDate.now().minusDays(3),null));}
 @Test void newPagesRender() throws Exception{
  Dare d=make();
  mvc.perform(get("/focus")).andExpect(status().isOk()).andExpect(view().name("focus"));
  mvc.perform(get("/challenges/"+d.getId())).andExpect(status().isOk()).andExpect(view().name("challenge-detail"));
  mvc.perform(get("/calendar/day").param("date",LocalDate.now().toString()))
    .andExpect(status().isOk()).andExpect(view().name("calendar-day"));
 }
 @Test void goalsProgressAndWeekAreSaved() throws Exception{
  Dare d=make();
  mvc.perform(post("/challenges/"+d.getId()+"/goals").param("targetValue","45").param("targetUnit","분").param("weeklyTarget","4"))
    .andExpect(status().is3xxRedirection());
  Dare updated=dares.findById(d.getId()).orElseThrow();
  assertThat(updated.getTargetValue()).isEqualTo(45);
  assertThat(updated.getTargetUnit()).isEqualTo("분");
  assertThat(updated.getWeeklyTarget()).isEqualTo(4);
  mvc.perform(post("/challenges/"+d.getId()+"/progress").param("amount","15"))
    .andExpect(status().is3xxRedirection());
  assertThat(progress.findByDareIdAndDay(d.getId(),LocalDate.now())).extracting("amount").contains(15);
 }
 @Test void skipOverrideRemovesTodayAndBlocksCompletion() throws Exception{
  Dare d=make();
  mvc.perform(post("/challenges/"+d.getId()+"/schedule")
    .param("date",LocalDate.now().toString()).param("mode","SKIP").param("note","휴식"))
    .andExpect(status().is3xxRedirection());
  mvc.perform(get("/")).andExpect(status().isOk())
    .andExpect(model().attribute("todayDares",org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(d))));
  mvc.perform(post("/complete").param("dareId",d.getId().toString()))
    .andExpect(status().is3xxRedirection())
    .andExpect(flash().attributeExists("error"));
 }
 @Test void focusApiCanStartPauseResumeCancel() throws Exception{
  Dare d=make();
  mvc.perform(post("/api/focus/start").param("dareId",d.getId().toString()).param("minutes","1"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RUNNING"));
  Long id=focus.findByStatusIn(java.util.List.of("RUNNING")).get(0).getId();
  mvc.perform(post("/api/focus/"+id+"/pause")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAUSED"));
  mvc.perform(post("/api/focus/"+id+"/resume")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RUNNING"));
  mvc.perform(post("/api/focus/"+id+"/finish")).andExpect(status().isBadRequest());
  mvc.perform(post("/api/focus/"+id+"/cancel")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
  assertThat(focus.findByStatusOrderByStartedAtDesc("COMPLETED")).isEmpty();
 }
 @Test void studyReviewPlanCanBeConfiguredAndCompleted() throws Exception{
  LocalDate base=LocalDate.now();
  mvc.perform(post("/challenges/save")
    .param("title","운영체제 Chapter 3").param("description","복습 플랜 테스트")
    .param("category","STUDY").param("minutes","30").param("targetValue","1").param("targetUnit","회")
    .param("weeklyTarget","0").param("repeatType","DAILY").param("startDate",base.toString())
    .param("reviewEnabled","true").param("reviewBaseDate",base.toString()).param("reviewIntervals","7, 1, 3, 7"))
    .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/challenges"));
  Dare d=dares.findAll().stream().filter(x->x.getTitle().equals("운영체제 Chapter 3")).findFirst().orElseThrow();
  assertThat(d.isReviewEnabled()).isTrue();
  assertThat(d.getReviewIntervals()).isEqualTo("1,3,7");
  mvc.perform(get("/challenges/"+d.getId())).andExpect(status().isOk())
    .andExpect(model().attributeExists("reviewPlan","reviewDone","reviewTotal"));
  mvc.perform(post("/challenges/"+d.getId()+"/reviews/1/toggle"))
    .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/challenges/"+d.getId()));
  assertThat(reviews.findByDareIdAndOffsetDays(d.getId(),1)).isPresent();
 }

}
