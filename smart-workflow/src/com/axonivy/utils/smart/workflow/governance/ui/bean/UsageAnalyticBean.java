package com.axonivy.utils.smart.workflow.governance.ui.bean;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.donut.DonutChartModel;
import org.primefaces.model.charts.line.LineChartModel;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.storage.HistoryStorage;
import com.axonivy.utils.smart.workflow.governance.history.storage.internal.IvyRepoHistoryStorage;
import com.axonivy.utils.smart.workflow.governance.service.HistoryAnalyticsService;
import com.axonivy.utils.smart.workflow.governance.ui.enums.DateRangeFilter;
import com.axonivy.utils.smart.workflow.governance.ui.model.DashboardKpi;

@ManagedBean
@ViewScoped
public class UsageAnalyticBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private HistoryStorage storage;
  private HistoryAnalyticsService analyticsService;

  private DateRangeFilter analyticsDateRange = DateRangeFilter.LAST_7_DAYS;
  private boolean loaded = false;
  private boolean analyticsCollapsed = false;

  private DashboardKpi kpi = DashboardKpi.empty();
  private LineChartModel tokenTimelineChart;
  private DonutChartModel modelDistributionChart;
  private BarChartModel tokenStackedChart;
  private BarChartModel topCasesChart;
  private BarChartModel responseTimeChart;

  @PostConstruct
  public void init() {
    storage = new IvyRepoHistoryStorage();
    analyticsService = new HistoryAnalyticsService();
  }

  public void loadAnalytics() {
    refreshAnalytics();
    loaded = true;
  }

  public void toggleAnalyticsCollapsed() {
    analyticsCollapsed = !analyticsCollapsed;
  }

  private void refreshAnalytics() {
    LocalDate from = analyticsDateRange.toDateFrom();
    LocalDate to = analyticsDateRange.toDateTo();
    List<AgentConversationEntry> all = storage.findAll();
    List<AgentConversationEntry> current = filterByDate(all, from, to);

    long days = (from != null && to != null) ? ChronoUnit.DAYS.between(from, to) + 1 : 0;
    List<AgentConversationEntry> previous = days > 0
        ? filterByDate(all, from.minusDays(days), from.minusDays(1))
        : List.of();

    kpi = analyticsService.computeKpi(current, previous);
    tokenTimelineChart = analyticsService.buildTokenTimeline(current);
    modelDistributionChart = analyticsService.buildModelDistribution(current);
    tokenStackedChart = analyticsService.buildTokenStacked(current);
    topCasesChart = analyticsService.buildTopCases(current);
    responseTimeChart = analyticsService.buildResponseTimeHistogram(current);
  }

  private List<AgentConversationEntry> filterByDate(List<AgentConversationEntry> all,
      LocalDate from, LocalDate to) {
    if (from == null && to == null) {
      return all;
    }
    return all.stream()
        .filter(entry -> {
          if (entry.getLastUpdated() == null) {
            return false;
          }
          try {
            LocalDate entryDate = LocalDateTime.parse(entry.getLastUpdated()).toLocalDate();
            return (from == null || !entryDate.isBefore(from)) && (to == null || !entryDate.isAfter(to));
          } catch (DateTimeParseException ignored) {
            return false;
          }
        })
        .toList();
  }

  public boolean isLoaded() {
    return loaded;
  }

  public boolean isAnalyticsCollapsed() {
    return analyticsCollapsed;
  }

  public DashboardKpi getKpi() {
    return kpi;
  }

  public LineChartModel getTokenTimelineChart() {
    return tokenTimelineChart;
  }

  public DonutChartModel getModelDistributionChart() {
    return modelDistributionChart;
  }

  public BarChartModel getTokenStackedChart() {
    return tokenStackedChart;
  }

  public BarChartModel getTopCasesChart() {
    return topCasesChart;
  }

  public BarChartModel getResponseTimeChart() {
    return responseTimeChart;
  }

  public DateRangeFilter getAnalyticsDateRange() {
    return analyticsDateRange;
  }

  public void setAnalyticsDateRange(DateRangeFilter analyticsDateRange) {
    this.analyticsDateRange = analyticsDateRange;
  }
}
