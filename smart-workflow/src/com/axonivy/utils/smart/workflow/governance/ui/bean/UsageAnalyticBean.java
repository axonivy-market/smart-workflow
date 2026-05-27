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

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.storage.HistoryStorage;
import com.axonivy.utils.smart.workflow.governance.history.analytic.internal.HistoryChartBuilder;
import com.axonivy.utils.smart.workflow.governance.history.analytic.internal.HistoryKpiComputer;
import com.axonivy.utils.smart.workflow.governance.history.storage.internal.IvyRepoHistoryStorage;
import com.axonivy.utils.smart.workflow.governance.ui.model.DashboardKpi;

import ch.ivyteam.ivy.environment.Ivy;

@ManagedBean
@ViewScoped
public class UsageAnalyticBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final String RANGE_TODAY   = "TODAY";
  private static final String RANGE_LAST_7  = "LAST_7_DAYS";
  private static final String RANGE_LAST_30 = "LAST_30_DAYS";
  private static final int    DAYS_IN_7_RANGE  = 6;  // inclusive: today counts as day 1
  private static final int    DAYS_IN_30_RANGE = 29;

  // ── Collaborators — wired in @PostConstruct (C2: not at field-declaration level) ──
  private HistoryStorage storage;
  private HistoryKpiComputer kpiComputer;
  private HistoryChartBuilder chartBuilder;

  private String analyticsDateRange = RANGE_LAST_7;
  private boolean loaded = false;
  private boolean analyticsCollapsed = false;

  private DashboardKpi kpi = DashboardKpi.empty();
  private BarChartModel tokenTimelineChart;
  private DonutChartModel modelDistributionChart;
  private BarChartModel tokenStackedChart;
  private BarChartModel topCasesChart;
  private BarChartModel responseTimeChart;

  @PostConstruct
  public void init() {
    storage = new IvyRepoHistoryStorage();
    kpiComputer = new HistoryKpiComputer();
    chartBuilder = new HistoryChartBuilder();
    // Analytics loads async via p:remoteCommand autoRun — skeleton is shown until loadAnalytics() completes.
  }

  /**
   * Called once on first page render by p:remoteCommand autoRun="true".
   * Sets loaded=true so the skeleton is replaced by real content.
   */
  public void loadAnalytics() {
    refreshAnalytics();
    loaded = true;
  }

  public void toggleAnalyticsCollapsed() {
    analyticsCollapsed = !analyticsCollapsed;
  }

  private void refreshAnalytics() {
    LocalDate from = resolveDateFrom(analyticsDateRange);
    LocalDate to = resolveDateTo(analyticsDateRange);
    List<AgentConversationEntry> all = storage.findAll();
    List<AgentConversationEntry> current = filterByDate(all, from, to);

    long days = (from != null && to != null) ? ChronoUnit.DAYS.between(from, to) + 1 : 0;
    List<AgentConversationEntry> previous = days > 0
        ? filterByDate(all, from.minusDays(days), from.minusDays(1))
        : List.of();

    kpi = kpiComputer.computeKpi(current, previous);
    tokenTimelineChart = chartBuilder.buildTokenTimeline(current);
    modelDistributionChart = chartBuilder.buildModelDistribution(current);
    tokenStackedChart = chartBuilder.buildTokenStacked(current);
    topCasesChart = chartBuilder.buildTopCases(current);
    responseTimeChart = chartBuilder.buildResponseTimeHistogram(current);
  }

  private List<AgentConversationEntry> filterByDate(List<AgentConversationEntry> all,
      LocalDate from, LocalDate to) {
    if (from == null && to == null) {
      return all;
    }
    return all.stream().filter(entry -> {
      if (entry.getLastUpdated() == null) {
        return false;
      }
      try {
        LocalDate d = LocalDateTime.parse(entry.getLastUpdated()).toLocalDate();
        return (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to));
      } catch (DateTimeParseException ex) {
        Ivy.log().warn("Skipping entry with unparseable date ''{0}'': {1}",
            entry.getLastUpdated(), ex.getMessage());
        return false;
      }
    }).toList();
  }

  private LocalDate resolveDateFrom(String range) {
    return switch (range) {
      case RANGE_TODAY   -> LocalDate.now();
      case RANGE_LAST_7  -> LocalDate.now().minusDays(DAYS_IN_7_RANGE);
      case RANGE_LAST_30 -> LocalDate.now().minusDays(DAYS_IN_30_RANGE);
      default -> null;
    };
  }

  private LocalDate resolveDateTo(String range) {
    return switch (range) {
      case RANGE_TODAY, RANGE_LAST_7, RANGE_LAST_30 -> LocalDate.now();
      default -> null;
    };
  }

  // ── Getters / setters ─────────────────────────────────────────────────────

  public boolean isLoaded() {
    return loaded;
  }

  public boolean isAnalyticsCollapsed() {
    return analyticsCollapsed;
  }

  public DashboardKpi getKpi() {
    return kpi;
  }

  public BarChartModel getTokenTimelineChart() {
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

  public String getAnalyticsDateRange() {
    return analyticsDateRange;
  }

  public void setAnalyticsDateRange(String v) {
    this.analyticsDateRange = v;
  }
}
