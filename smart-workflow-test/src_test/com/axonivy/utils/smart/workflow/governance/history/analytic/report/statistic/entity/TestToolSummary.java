package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestToolSummary {

  @Test
  void getSuccessCount_noNullsNoErrors_returnsCallCount() {
    var tool = new ToolSummary("search", 10, 0, 0, null);
    assertThat(tool.getSuccessCount()).isEqualTo(10);
  }

  @Test
  void getSuccessCount_someNullResults_subtractsNulls() {
    var tool = new ToolSummary("fetch", 10, 3, 0, null);
    assertThat(tool.getSuccessCount()).isEqualTo(7);
  }

  @Test
  void getSuccessCount_someErrors_subtractsErrors() {
    var tool = new ToolSummary("fetch", 10, 0, 2, null);
    assertThat(tool.getSuccessCount()).isEqualTo(8);
  }

  @Test
  void getSuccessCount_nullsAndErrors_subtractsBoth() {
    var tool = new ToolSummary("fetch", 10, 2, 3, null);
    assertThat(tool.getSuccessCount()).isEqualTo(5);
  }

  @Test
  void getSuccessRate_perfectRate_returns100() {
    var tool = new ToolSummary("search", 10, 0, 0, null);
    assertThat(tool.getSuccessRate()).isEqualTo(100);
  }

  @Test
  void getSuccessRate_halfSuccessful_returns50() {
    var tool = new ToolSummary("search", 10, 5, 0, null);
    assertThat(tool.getSuccessRate()).isEqualTo(50);
  }

  @Test
  void getSuccessRate_zeroCalls_returnsZero() {
    var tool = new ToolSummary("search", 0, 0, 0, null);
    assertThat(tool.getSuccessRate()).isEqualTo(0);
  }

  @Test
  void getSuccessRate_integerTruncation_noRounding() {
    // 2 successes out of 3 = 66.6... → truncated to 66
    var tool = new ToolSummary("search", 3, 1, 0, null);
    assertThat(tool.getSuccessRate()).isEqualTo(66);
  }

  @Test
  void toString_containsNameCallsNullsErrors() {
    var tool = new ToolSummary("fetch", 5, 1, 2, null);
    assertThat(tool.toString()).contains("fetch").contains("5").contains("1").contains("2");
  }
}
