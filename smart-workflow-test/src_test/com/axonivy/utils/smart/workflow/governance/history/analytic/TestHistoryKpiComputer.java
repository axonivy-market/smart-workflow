package com.axonivy.utils.smart.workflow.governance.history.analytic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.analytic.internal.HistoryAggregator;
import com.axonivy.utils.smart.workflow.governance.history.analytic.internal.HistoryKpiComputer;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestHistoryKpiComputer {

  private final HistoryKpiComputer kpiComputer = new HistoryKpiComputer();

  @Test
  void computeKpi_emptyAggregator_returnsEmptyKpi() {
    DashboardKpi kpi = kpiComputer.computeKpi(HistoryAggregator.of(List.of()));
    assertThat(kpi.getTotalSessions()).isZero();
  }

  @Test
  void computeKpi_withData_mapsAllFieldsFromAggregator() {
    var e = TestHistoryAggregator.entry("gpt-4", 200, 120, 80, 5_000);
    HistoryAggregator stats = HistoryAggregator.of(List.of(e));
    DashboardKpi kpi = kpiComputer.computeKpi(stats);
    assertThat(kpi.getTotalSessions()).isEqualTo(1);
    assertThat(kpi.getTotalTokens()).isEqualTo(200);
    assertThat(kpi.getAvgResponseMs()).isEqualTo(5_000);
    assertThat(kpi.getTopModel()).isEqualTo("gpt-4");
  }
}
