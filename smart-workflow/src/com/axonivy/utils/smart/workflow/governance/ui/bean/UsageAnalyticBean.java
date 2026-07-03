package com.axonivy.utils.smart.workflow.governance.ui.bean;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.donut.DonutChartModel;

import com.axonivy.utils.smart.workflow.governance.history.analytic.internal.HistoryAggregator;
import com.axonivy.utils.smart.workflow.governance.history.analytic.internal.HistoryKpiComputer;
import com.axonivy.utils.smart.workflow.governance.history.analytic.internal.ModelDistributionChartBuilder;
import com.axonivy.utils.smart.workflow.governance.history.analytic.internal.ResponseTimeChartBuilder;
import com.axonivy.utils.smart.workflow.governance.history.analytic.internal.TokenStackedChartBuilder;
import com.axonivy.utils.smart.workflow.governance.history.analytic.internal.TokenTimelineChartBuilder;
import com.axonivy.utils.smart.workflow.governance.history.analytic.internal.TopCasesChartBuilder;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.internal.HistoryEntryFilter;
import com.axonivy.utils.smart.workflow.governance.history.storage.HistoryStorage;
import com.axonivy.utils.smart.workflow.governance.history.storage.internal.IvyRepoHistoryStorage;
import com.axonivy.utils.smart.workflow.governance.ui.model.DashboardKpi;

@ManagedBean
@ViewScoped
public class UsageAnalyticBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final int ANALYTICS_LOOKBACK_DAYS = 6; // last 7 days inclusive of today

  private final HistoryStorage storage                             = new IvyRepoHistoryStorage();
  private final HistoryKpiComputer kpiComputer                     = new HistoryKpiComputer();
  private final TokenTimelineChartBuilder tokenTimelineBuilder      = new TokenTimelineChartBuilder();
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
    kpi = kpiComputer.computeKpi(stats);
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
