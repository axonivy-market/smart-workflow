package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestAgentGrade {

  @ParameterizedTest(name = "{argumentSetNameOrArgumentsWithNames}")
  @MethodSource("gradeFromArgs")
  void from_score_returnsCorrectGrade(int score, AgentGrade expectedGrade) {
    assertThat(AgentGrade.from(score)).isEqualTo(expectedGrade);
  }

  @SuppressWarnings("unused")
  static Stream<Arguments> gradeFromArgs() {
    return Stream.of(
        Arguments.argumentSet("100_returnsA",     100, AgentGrade.A),
        Arguments.argumentSet("90_returnsA",       90, AgentGrade.A),
        Arguments.argumentSet("89_returnsB",       89, AgentGrade.B),
        Arguments.argumentSet("75_returnsB",       75, AgentGrade.B),
        Arguments.argumentSet("74_returnsC",       74, AgentGrade.C),
        Arguments.argumentSet("60_returnsC",       60, AgentGrade.C),
        Arguments.argumentSet("59_returnsD",       59, AgentGrade.D),
        Arguments.argumentSet("40_returnsD",       40, AgentGrade.D),
        Arguments.argumentSet("39_returnsF",       39, AgentGrade.F),
        Arguments.argumentSet("0_returnsF",         0, AgentGrade.F),
        Arguments.argumentSet("negative_returnsF", -1, AgentGrade.F)
    );
  }

  @Test
  void format_containsNameScoreAndFeedback() {
    String result = AgentGrade.A.format(95);
    assertThat(result).contains("A").contains("95").contains("Excellent");
  }

  @Test
  void format_gradeF_containsFeedback() {
    String result = AgentGrade.F.format(10);
    assertThat(result).contains("F").contains("10").contains("Critical");
  }

  @Test
  void getMinScore_returnsCorrectThreshold() {
    assertThat(AgentGrade.A.getMinScore()).isEqualTo(90);
    assertThat(AgentGrade.B.getMinScore()).isEqualTo(75);
    assertThat(AgentGrade.C.getMinScore()).isEqualTo(60);
    assertThat(AgentGrade.D.getMinScore()).isEqualTo(40);
    assertThat(AgentGrade.F.getMinScore()).isEqualTo(0);
  }
}
