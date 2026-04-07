package com.axonivy.utils.smart.workflow.governance.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.primefaces.model.charts.bar.BarChartDataSet;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.service.HistoryAnalyticsService;
import com.axonivy.utils.smart.workflow.governance.ui.model.DashboardKpi;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestHistoryAnalyticsService {

  private final HistoryAnalyticsService service = new HistoryAnalyticsService();

  private static final LocalDateTime T0 = LocalDateTime.of(2025, 6, 1, 10, 0, 0);

  // ── Fixtures ──────────────────────────────────────────────────────────────

  private static AgentConversationEntry entry(String model, int totalTokens, int inputTokens,
      int outputTokens, long durationMs, LocalDateTime lastUpdated) {
    var e = new AgentConversationEntry();
    e.setCaseUuid("case-1");
    e.setTaskUuid("task-1");
    e.setLastUpdated(lastUpdated.toString());
    e.setTokenUsageJson(String.format(
        "[{\"totalTokens\":%d,\"inputTokens\":%d,\"outputTokens\":%d,\"modelName\":\"%s\",\"durationMs\":%d}]",
        totalTokens, inputTokens, outputTokens, model, durationMs));
    return e;
  }

  // ── computeKpi ────────────────────────────────────────────────────────────

  @Test
  void computeKpi_emptyList_returnsEmptyKpi() {
    DashboardKpi kpi = service.computeKpi(List.of(), List.of());
    assertThat(kpi.getTotalSessions()).isZero();
    assertThat(kpi.getTotalTokens()).isZero();
    assertThat(kpi.getTopModel()).isEqualTo("\u2014");
  }

  @Test
  void computeKpi_singleEntry_countsCorrectly() {
    var e = entry("gpt-4o", 150, 100, 50, 3000, T0);
    DashboardKpi kpi = service.computeKpi(List.of(e), List.of());

    assertThat(kpi.getTotalSessions()).isEqualTo(1);
    assertThat(kpi.getTotalTokens()).isEqualTo(150L);
    assertThat(kpi.getTopModel()).isEqualTo("gpt-4o");
    assertThat(kpi.getTopModelPct()).isEqualTo(100);
    assertThat(kpi.getAvgResponseMs()).isEqualTo(3000L);
  }

  @Test
  void computeKpi_trendPct_zeroOldValueReturnsZero() {
    var current = entry("gpt-4o", 100, 60, 40, 2000, T0);
    DashboardKpi kpi = service.computeKpi(List.of(current), List.of());
    // previous is empty → oldVal = 0 → trendPct returns 0
    assertThat(kpi.getSessionsTrend()).isZero();
    assertThat(kpi.getTokensTrend()).isZero();
  }

  @Test
  void computeKpi_trendPct_doubledTokensReturns100Percent() {
    var prev = entry("gpt-4o", 100, 60, 40, 2000, T0);
    var curr = entry("gpt-4o", 200, 120, 80, 2000, T0.plusDays(1));
    DashboardKpi kpi = service.computeKpi(List.of(curr), List.of(prev));
    assertThat(kpi.getTokensTrend()).isEqualTo(100);
  }

  @Test
  void computeKpi_topModel_picksHighestCount() {
    var e1 = entry("gpt-4o",   100, 60, 40, 1000, T0);
    var e2 = entry("gemini",   100, 60, 40, 1000, T0.plusMinutes(1));
    var e3 = entry("gpt-4o",   100, 60, 40, 1000, T0.plusMinutes(2));
    DashboardKpi kpi = service.computeKpi(List.of(e1, e2, e3), List.of());
    assertThat(kpi.getTopModel()).isEqualTo("gpt-4o");
    assertThat(kpi.getTopModelPct()).isEqualTo(67); // round(200/3)
  }

  // ── buildTokenTimeline ───────────────────────────────────────────────────

  @Test
  void buildTokenTimeline_groupsByDay() {
    var e1 = entry("gpt-4o", 100, 60, 40, 1000, T0);
    var e2 = entry("gpt-4o", 200, 120, 80, 2000, T0);             // same day
    var e3 = entry("gpt-4o",  50,  30, 20, 1000, T0.plusDays(1)); // next day
    var model = service.buildTokenTimeline(List.of(e1, e2, e3));

    assertThat((List<?>) model.getData().getLabels()).hasSize(2);
    assertThat((List<?>) model.getData().getDataSet()).hasSize(1);
  }

  // ── buildModelDistribution ───────────────────────────────────────────────

  @Test
  void buildModelDistribution_twoModels_twoLabels() {
    var e1 = entry("gpt-4o",  100, 60, 40, 1000, T0);
    var e2 = entry("gemini",  100, 60, 40, 1000, T0);
    var model = service.buildModelDistribution(List.of(e1, e2));

    assertThat((List<?>) model.getData().getLabels()).hasSize(2);
  }

  // ── buildResponseTimeHistogram ───────────────────────────────────────────

  @Test
  void buildResponseTimeHistogram_bucketsByCutoffs() {
    var fast   = entry("gpt-4o", 100, 60, 40, 1000,  T0);  // < 5s
    var medium = entry("gpt-4o", 100, 60, 40, 7000,  T0);  // 5-10s
    var slow   = entry("gpt-4o", 100, 60, 40, 20000, T0);  // > 15s
    var model  = service.buildResponseTimeHistogram(List.of(fast, medium, slow));

    BarChartDataSet ds = (BarChartDataSet) model.getData().getDataSet().get(0);
    assertThat(ds.getData().get(0)).isEqualTo(1L); // < 5s
    assertThat(ds.getData().get(1)).isEqualTo(1L); // 5-10s
    assertThat(ds.getData().get(2)).isEqualTo(0L); // 10-15s
    assertThat(ds.getData().get(3)).isEqualTo(1L); // > 15s
  }
}
