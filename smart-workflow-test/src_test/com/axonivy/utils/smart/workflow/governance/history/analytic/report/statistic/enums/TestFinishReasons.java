package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestFinishReasons {

  @ParameterizedTest
  @ValueSource(strings = {"STOP", "stop", "Stop"})
  void stop_matches_caseInsensitive(String reason) {
    assertThat(FinishReasons.STOP.matches(reason)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"LENGTH", "length", "Length"})
  void length_matches_caseInsensitive(String reason) {
    assertThat(FinishReasons.LENGTH.matches(reason)).isTrue();
  }

  @Test
  void stop_doesNotMatch_differentReason() {
    assertThat(FinishReasons.STOP.matches("LENGTH")).isFalse();
  }

  @Test
  void length_doesNotMatch_differentReason() {
    assertThat(FinishReasons.LENGTH.matches("STOP")).isFalse();
  }

  @Test
  void isUnexpected_null_returnsFalse() {
    assertThat(FinishReasons.isUnexpected(null)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"STOP", "stop", "LENGTH", "length"})
  void isUnexpected_knownReasons_returnsFalse(String reason) {
    assertThat(FinishReasons.isUnexpected(reason)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"TIMEOUT", "ERROR", "CANCELLED", ""})
  void isUnexpected_unknownReason_returnsTrue(String reason) {
    assertThat(FinishReasons.isUnexpected(reason)).isTrue();
  }
}
