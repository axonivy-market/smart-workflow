package com.axonivy.utils.smart.workflow.governance.ui.bean;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.donut.DonutChartModel;

import com.axonivy.utils.smart.workflow.governance.history.analytic.chart.HistoryAggregator;
import com.axonivy.utils.smart.workflow.governance.history.analytic.chart.ModelDistributionChartBuilder;
import com.axonivy.utils.smart.workflow.governance.history.analytic.chart.ResponseTimeChartBuilder;
import com.axonivy.utils.smart.workflow.governance.history.analytic.chart.TokenStackedChartBuilder;
import com.axonivy.utils.smart.workflow.governance.history.analytic.chart.TokenTimelineChartBuilder;
import com.axonivy.utils.smart.workflow.governance.history.analytic.chart.TopCasesChartBuilder;
import com.axonivy.utils.smart.workflow.governance.history.analytic.chart.model.DashboardKpi;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.filter.HistoryEntryFilter;
import com.axonivy.utils.smart.workflow.governance.history.storage.HistoryStorage;
import com.axonivy.utils.smart.workflow.governance.history.storage.IvyRepoHistoryStorage;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named("usageAnalyticBean")
@ViewScoped
public class UsageAnalyticBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final int ANALYTICS_LOOKBACK_DAYS = 6;

  private final HistoryStorage storage                        = new IvyRepoHistoryStorage();
  private final TokenTimelineChartBuilder tokenTimelineBuilder = new TokenTimelineChartBuilder();
  private final ModelDistributionChartBuilder modelDistBuilder      = new ModelDistributionChartBuilder();
  private final TokenStackedChartBuilder tokenStackedBuilder        = new TokenStackedChartBuilder();
  private final TopCasesChartBuilder topCasesBuilder                = new TopCasesChartBuilder();
  private final ResponseTimeChartBuilder responseTimeBuilder        = new ResponseTimeChartBuilder();

  private boolean loaded = false;

  private DashboardKpi kpi = DashboardKpi.empty();
  private BarChartModel tokenTimelineChart;
  private DonutChartModel modelDistributionChart;
  private BarChartModel tokenStackedChart;
  private BarChartModel topCasesChart;
  private BarChartModel responseTimeChart;

  public void loadAnalytics() {
    refreshAnalytics();
    loaded = true;
  }

  private void refreshAnalytics() {
    LocalDate from = LocalDate.now().minusDays(ANALYTICS_LOOKBACK_DAYS);
    LocalDate to = LocalDate.now();
    List<AgentConversationEntry> current = HistoryEntryFilter.filterByDateRange(storage.findAll(), from, to);

    HistoryAggregator stats = HistoryAggregator.of(current);
    kpi = new DashboardKpi(stats.getTotalSessions(), stats.getTotalTokens(), stats.getAvgResponseMs(), stats.getTopModel());
    tokenTimelineChart     = tokenTimelineBuilder.build(stats);
    modelDistributionChart = modelDistBuilder.build(stats);
    tokenStackedChart      = tokenStackedBuilder.build(stats);
    topCasesChart          = topCasesBuilder.build(stats);
    responseTimeChart      = responseTimeBuilder.build(stats);
  }

  public boolean isLoaded() { return loaded; }
  public DashboardKpi getKpi() { return kpi; }
  public BarChartModel getTokenTimelineChart() { return tokenTimelineChart; }
  public DonutChartModel getModelDistributionChart() { return modelDistributionChart; }
  public BarChartModel getTokenStackedChart() { return tokenStackedChart; }
  public BarChartModel getTopCasesChart() { return topCasesChart; }
  public BarChartModel getResponseTimeChart() { return responseTimeChart; }
}
