package com.axonivy.utils.smart.workflow.governance.ui.bean;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.donut.DonutChartModel;
import org.primefaces.model.charts.line.LineChartModel;

import com.axonivy.utils.smart.workflow.governance.history.ChatHistoryEntry;
import com.axonivy.utils.smart.workflow.governance.history.HistoryFilter;
import com.axonivy.utils.smart.workflow.governance.history.HistoryStorage;
import com.axonivy.utils.smart.workflow.governance.service.HistoryAnalyticsService;
import com.axonivy.utils.smart.workflow.governance.ui.model.DashboardKpi;

@ManagedBean
@ViewScoped
public class UsageAnalyticBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private final HistoryStorage storage = HistoryStorage.create();
  private final HistoryAnalyticsService analyticsService = new HistoryAnalyticsService();

  private String analyticsDateRange = "LAST_7_DAYS";
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
    // Intentionally empty — analytics loads async via p:remoteCommand autoRun.
    // This keeps page initial render fast; skeleton is shown until loadAnalytics() completes.
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
    List<ChatHistoryEntry> current = storage.query(new HistoryFilter(null, null, null, from, to));

    long days = (from != null && to != null) ? ChronoUnit.DAYS.between(from, to) + 1 : 0;
    List<ChatHistoryEntry> previous = days > 0
        ? storage.query(new HistoryFilter(null, null, null,
            from.minusDays(days), from.minusDays(1)))
        : List.of();

    kpi = analyticsService.computeKpi(current, previous);
    tokenTimelineChart = analyticsService.buildTokenTimeline(current);
    modelDistributionChart = analyticsService.buildModelDistribution(current);
    tokenStackedChart = analyticsService.buildTokenStacked(current);
    topCasesChart = analyticsService.buildTopCases(current);
    responseTimeChart = analyticsService.buildResponseTimeHistogram(current);
  }

  private LocalDate resolveDateFrom(String range) {
    return switch (range) {
      case "TODAY" -> LocalDate.now();
      case "LAST_7_DAYS" -> LocalDate.now().minusDays(6);
      case "LAST_30_DAYS" -> LocalDate.now().minusDays(29);
      default -> null;
    };
  }

  private LocalDate resolveDateTo(String range) {
    return switch (range) {
      case "TODAY", "LAST_7_DAYS", "LAST_30_DAYS" -> LocalDate.now();
      default -> null;
    };
  }

  // ── Getters / setters ─────────────────────────────────────────────────────

  public boolean isLoaded() { return loaded; }
  public boolean isAnalyticsCollapsed() { return analyticsCollapsed; }

  public DashboardKpi getKpi() { return kpi; }
  public LineChartModel getTokenTimelineChart() { return tokenTimelineChart; }
  public DonutChartModel getModelDistributionChart() { return modelDistributionChart; }
  public BarChartModel getTokenStackedChart() { return tokenStackedChart; }
  public BarChartModel getTopCasesChart() { return topCasesChart; }
  public BarChartModel getResponseTimeChart() { return responseTimeChart; }
}
