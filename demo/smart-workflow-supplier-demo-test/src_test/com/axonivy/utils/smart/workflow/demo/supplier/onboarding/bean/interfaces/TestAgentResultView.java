package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.agent.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AgentStepStatus;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.LogLineSeverity;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestAgentResultView {

  private static final AgentResultView VIEW = () -> null;

  @ParameterizedTest(name = "{0}")
  @CsvSource({
      "getScoreBarClass_whenScore70_returnsGreen,   70,  so-score-bar-green",
      "getScoreBarClass_whenScore100_returnsGreen,  100, so-score-bar-green",
      "getScoreBarClass_whenScore40_returnsYellow,  40,  so-score-bar-yellow",
      "getScoreBarClass_whenScore69_returnsYellow,  69,  so-score-bar-yellow",
      "getScoreBarClass_whenScore39_returnsRed,     39,  so-score-bar-red",
      "getScoreBarClass_whenScore0_returnsRed,      0,   so-score-bar-red"
  })
  void getScoreBarClass(String testName, int score, String expected) {
    assertThat(VIEW.getScoreBarClass(score)).as(testName).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(value = {
      "getStepStatusIcon_whenCompleted_returnsCircleCheck, COMPLETED, ti-circle-check",
      "getStepStatusIcon_whenRunning_returnsLoader,        RUNNING,   ti-loader",
      "getStepStatusIcon_whenFailed_returnsCircleX,        FAILED,    ti-circle-x",
      "getStepStatusIcon_whenPending_returnsClock,         PENDING,   ti-clock",
      "getStepStatusIcon_whenNull_returnsClock,            NULL,      ti-clock"
  }, nullValues = "NULL")
  void getStepStatusIcon(String testName, AgentStepStatus status, String expected) {
    assertThat(VIEW.getStepStatusIcon(status == null ? null : step(status))).as(testName).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(value = {
      "getStepBubbleClass_whenCompleted_returnsCompleted, COMPLETED, so-tl-bubble-completed",
      "getStepBubbleClass_whenFailed_returnsFailed,       FAILED,    so-tl-bubble-failed",
      "getStepBubbleClass_whenPending_returnsPending,     PENDING,   so-tl-bubble-pending",
      "getStepBubbleClass_whenNull_returnsPending,        NULL,      so-tl-bubble-pending"
  }, nullValues = "NULL")
  void getStepBubbleClass(String testName, AgentStepStatus status, String expected) {
    assertThat(VIEW.getStepBubbleClass(status == null ? null : step(status))).as(testName).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
      "getStepRowClass_whenCompleted_returnsCompleted, COMPLETED, so-checklist-item completed",
      "getStepRowClass_whenRunning_returnsRunning,     RUNNING,   so-checklist-item running",
      "getStepRowClass_whenFailed_returnsFailed,       FAILED,    so-checklist-item failed",
      "getStepRowClass_whenPending_returnsPending,     PENDING,   so-checklist-item pending"
  })
  void getStepRowClass(String testName, AgentStepStatus status, String expected) {
    assertThat(VIEW.getStepRowClass(step(status))).as(testName).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(value = {
      "severity_whenWarning, WARNING, ti-alert-triangle, so-log-line-warning",
      "severity_whenError,   ERROR,   ti-circle-x,       so-log-line-error",
      "severity_whenOK,      OK,      ti-circle-check,   so-log-line-ok",
      "severity_whenNull,    NULL,    ti-circle-check,   so-log-line-ok"
  }, nullValues = "NULL")
  void severity_mapsToIconAndClass(String testName, LogLineSeverity severity, String icon, String cssClass) {
    assertThat(VIEW.getSeverityIcon(severity)).as(testName + ".icon").isEqualTo(icon);
    assertThat(VIEW.getSeverityClass(severity)).as(testName + ".class").isEqualTo(cssClass);
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(value = {
      "getFormattedDuration_whenStepIsNull_returnsEmpty,     NULL,      NULL, ''",
      "getFormattedDuration_whenDurationIsNull_returnsEmpty, PENDING,   NULL, ''",
      "getFormattedDuration_formatsMillisToSeconds,          COMPLETED, 2000, 2.0s"
  }, nullValues = "NULL")
  void getFormattedDuration(String testName, AgentStepStatus status, Long durationMs, String expected) {
    AgentProcessingStep step = status == null ? null : step(status);
    if (step != null && durationMs != null) {
      step.setDurationMs(durationMs);
    }
    assertThat(VIEW.getFormattedDuration(step)).as(testName).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(value = {
      "getRecipientInitials_whenTwoWords_returnsInitials,                    John Doe,      JD",
      "getRecipientInitials_whenTwoWordsLowercase_returnsUppercasedInitials, jane smith,    JS",
      "getRecipientInitials_whenThreeWords_usesFirstAndLastInitial,          John Paul Doe, JD",
      "getRecipientInitials_whenOneWord_returnsFirstTwoChars,                John,          JO",
      "getRecipientInitials_whenOneChar_returnsSingleChar,                   A,             A",
      "getRecipientInitials_whenNull_returnsQuestionMark,                    NULL,          ?",
      "getRecipientInitials_whenBlank_returnsQuestionMark,                   '   ',         ?",
      "getRecipientInitials_whenEmpty_returnsQuestionMark,                   '',            ?"
  }, nullValues = "NULL")
  void getRecipientInitials(String testName, String name, String expected) {
    assertThat(VIEW.getRecipientInitials(name)).as(testName).isEqualTo(expected);
  }

  private static AgentProcessingStep step(AgentStepStatus status) {
    AgentProcessingStep step = new AgentProcessingStep();
    step.setStatus(status);
    return step;
  }
}
