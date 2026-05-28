package com.axonivy.utils.smart.workflow.governance.history.analytic;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.analytic.internal.HistoryKpiComputer;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.ui.model.DashboardKpi;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestHistoryKpiComputer {

  private final HistoryKpiComputer kpiComputer = new HistoryKpiComputer();

  private static final LocalDateTime T0 = LocalDateTime.of(2025, 6, 1, 10, 0, 0);

  // ── Fixture ───────────────────────────────────────────────────────────────

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

  // ── computeKpi — empty / basic ────────────────────────────────────────────

  @Test
  void computeKpi_emptyList_returnsEmptyKpi() {
    DashboardKpi kpi = kpiComputer.computeKpi(List.of(), List.of());
    assertThat(kpi.getTotalSessions()).isZero();
    assertThat(kpi.getTotalTokens()).isZero();
    assertThat(kpi.getTopModel()).isEqualTo("\u2014");
  }

  @Test
  void computeKpi_singleEntry_countsCorrectly() {
    var e = entry("gpt-4o", 150, 100, 50, 3000, T0);
    DashboardKpi kpi = kpiComputer.computeKpi(List.of(e), List.of());

    assertThat(kpi.getTotalSessions()).isEqualTo(1);
    assertThat(kpi.getTotalTokens()).isEqualTo(150L);
    assertThat(kpi.getTopModel()).isEqualTo("gpt-4o");
    assertThat(kpi.getTopModelPct()).isEqualTo(100);
    assertThat(kpi.getAvgResponseMs()).isEqualTo(3000L);
  }

  // ── computeKpi — top model ────────────────────────────────────────────────

  @Test
  void computeKpi_topModel_picksHighestCount() {
    var e1 = entry("gpt-4o", 100, 60, 40, 1000, T0);
    var e2 = entry("gemini", 100, 60, 40, 1000, T0.plusMinutes(1));
    var e3 = entry("gpt-4o", 100, 60, 40, 1000, T0.plusMinutes(2));
    DashboardKpi kpi = kpiComputer.computeKpi(List.of(e1, e2, e3), List.of());

    assertThat(kpi.getTopModel()).isEqualTo("gpt-4o");
    assertThat(kpi.getTopModelPct()).isEqualTo(67); // round(2/3 * 100)
  }

  // ── computeKpi — trend: zero baseline ────────────────────────────────────

  @Test
  void computeKpi_trendPct_zeroOldValueReturnsZero() {
    var current = entry("gpt-4o", 100, 60, 40, 2000, T0);
    DashboardKpi kpi = kpiComputer.computeKpi(List.of(current), List.of());
    // previous is empty → oldVal = 0 → trendPct returns 0
    assertThat(kpi.getSessionsTrend()).isZero();
    assertThat(kpi.getTokensTrend()).isZero();
  }

  // ── computeKpi — trend: positive ─────────────────────────────────────────

  @Test
  void computeKpi_trendPct_doubledTokensReturns100Percent() {
    var prev = entry("gpt-4o", 100, 60, 40, 2000, T0);
    var curr = entry("gpt-4o", 200, 120, 80, 2000, T0.plusDays(1));
    DashboardKpi kpi = kpiComputer.computeKpi(List.of(curr), List.of(prev));

    assertThat(kpi.getTokensTrend()).isEqualTo(100);
  }

  @Test
  void computeKpi_sessionsTrend_positiveWhenMore() {
    var prev = entry("gpt-4o", 100, 60, 40, 2000, T0);
    var curr1 = entry("gpt-4o", 100, 60, 40, 2000, T0.plusDays(1));
    var curr2 = entry("gpt-4o", 100, 60, 40, 2000, T0.plusDays(1));
    DashboardKpi kpi = kpiComputer.computeKpi(List.of(curr1, curr2), List.of(prev));

    // 1 → 2 sessions: +100%
    assertThat(kpi.getSessionsTrend()).isEqualTo(100);
  }

  // ── computeKpi — trend: negative ─────────────────────────────────────────

  @Test
  void computeKpi_responseMsTrend_negativeWhenFaster() {
    var prev = entry("gpt-4o", 100, 60, 40, 4000, T0);
    var curr = entry("gpt-4o", 100, 60, 40, 2000, T0.plusDays(1));
    DashboardKpi kpi = kpiComputer.computeKpi(List.of(curr), List.of(prev));

    // 4000ms → 2000ms: -50%
    assertThat(kpi.getResponseMsTrend()).isEqualTo(-50);
  }
}
