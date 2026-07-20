package com.axonivy.utils.smart.workflow.governance.history.analytic.internal;

import com.axonivy.utils.smart.workflow.governance.ui.model.DashboardKpi;

public class HistoryKpiComputer {

  public DashboardKpi computeKpi(HistoryAggregator stats) {
    if (stats.getTotalSessions() == 0) {
      return DashboardKpi.empty();
    }
    return new DashboardKpi(stats.getTotalSessions(), stats.getTotalTokens(),
        stats.getAvgResponseMs(), stats.getTopModel());
  }
}
