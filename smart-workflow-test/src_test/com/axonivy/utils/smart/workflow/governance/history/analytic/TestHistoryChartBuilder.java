package com.axonivy.utils.smart.workflow.governance.history.analytic;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.primefaces.model.charts.bar.BarChartDataSet;

import com.axonivy.utils.smart.workflow.governance.history.analytic.internal.HistoryChartBuilder;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestHistoryChartBuilder {

  private final HistoryChartBuilder chartBuilder = new HistoryChartBuilder();

  private static final int MIN_TIMELINE_DAYS = 5;
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

  private static AgentConversationEntry entryForProcess(String processName, int totalTokens,
      LocalDateTime lastUpdated) {
    var e = entry("gpt-4o", totalTokens, totalTokens / 2, totalTokens / 2, 1000, lastUpdated);
    e.setProcessName(processName);
    return e;
  }

  // ── buildTokenTimeline ───────────────────────────────────────────────────

  @Test
  void buildTokenTimeline_groupsSameDayAndPadsToMinDays() {
    var e1 = entry("gpt-4o", 100, 60, 40, 1000, T0);
    var e2 = entry("gpt-4o", 200, 120, 80, 2000, T0);              // same day → summed
    var e3 = entry("gpt-4o",  50,  30, 20, 1000, T0.plusDays(1));  // next day

    var model = chartBuilder.buildTokenTimeline(List.of(e1, e2, e3));
    var labels = (List<?>) model.getData().getLabels();
    var ds = (BarChartDataSet) model.getData().getDataSet().get(0);

    // 2 real days + 3 padding days = MIN_TIMELINE_DAYS
    assertThat(labels).hasSize(MIN_TIMELINE_DAYS);
    assertThat(ds.getData().get(0)).isEqualTo(300L); // 100+200 on T0
    assertThat(ds.getData().get(1)).isEqualTo(50L);  // T0+1
    assertThat(ds.getData().get(2)).isEqualTo(0L);   // padding
  }

  @Test
  void buildTokenTimeline_emptyList_padsToMinDays() {
    var model = chartBuilder.buildTokenTimeline(List.of());
    var labels = (List<?>) model.getData().getLabels();
    var ds = (BarChartDataSet) model.getData().getDataSet().get(0);

    assertThat(labels).hasSize(MIN_TIMELINE_DAYS);
    assertThat(ds.getData()).containsOnly(0L);
  }

  // ── buildModelDistribution ───────────────────────────────────────────────

  @Test
  void buildModelDistribution_twoModels_twoLabels() {
    var e1 = entry("gpt-4o", 100, 60, 40, 1000, T0);
    var e2 = entry("gemini", 100, 60, 40, 1000, T0);

    var model = chartBuilder.buildModelDistribution(List.of(e1, e2));

    assertThat((List<?>) model.getData().getLabels()).hasSize(2);
    assertThat((List<?>) model.getData().getDataSet()).hasSize(1);
  }

  @Test
  void buildModelDistribution_emptyList_emptyChart() {
    var model = chartBuilder.buildModelDistribution(List.of());

    assertThat((List<?>) model.getData().getLabels()).isEmpty();
  }

  // ── buildTokenStacked ────────────────────────────────────────────────────

  @Test
  void buildTokenStacked_splitsByInputAndOutput() {
    var e = entry("gpt-4o", 150, 100, 50, 1000, T0);

    var model = chartBuilder.buildTokenStacked(List.of(e));
    var inputDs  = (BarChartDataSet) model.getData().getDataSet().get(0);
    var outputDs = (BarChartDataSet) model.getData().getDataSet().get(1);

    // T0 is first bucket; rest are padding zeros
    assertThat(inputDs.getData().get(0)).isEqualTo(100L);
    assertThat(outputDs.getData().get(0)).isEqualTo(50L);
  }

  @Test
  void buildTokenStacked_paddedToMinDays() {
    var e = entry("gpt-4o", 150, 100, 50, 1000, T0);

    var model = chartBuilder.buildTokenStacked(List.of(e));
    var labels = (List<?>) model.getData().getLabels();

    assertThat(labels).hasSize(MIN_TIMELINE_DAYS);
  }

  @Test
  void buildTokenStacked_emptyList_paddedAllZero() {
    var model = chartBuilder.buildTokenStacked(List.of());
    var inputDs  = (BarChartDataSet) model.getData().getDataSet().get(0);
    var outputDs = (BarChartDataSet) model.getData().getDataSet().get(1);

    assertThat(inputDs.getData()).containsOnly(0L);
    assertThat(outputDs.getData()).containsOnly(0L);
  }

  // ── buildTopCases ────────────────────────────────────────────────────────

  @Test
  void buildTopCases_rankedByTokensDesc() {
    var low  = entryForProcess("low",  100, T0);
    var high = entryForProcess("high", 500, T0);
    var mid  = entryForProcess("mid",  300, T0);

    var model = chartBuilder.buildTopCases(List.of(low, high, mid));
    var labels = (List<?>) model.getData().getLabels();

    assertThat(labels.get(0)).isEqualTo("high");
    assertThat(labels.get(1)).isEqualTo("mid");
    assertThat(labels.get(2)).isEqualTo("low");
  }

  @Test
  void buildTopCases_limitsToFive() {
    var entries = List.of(
        entryForProcess("P1", 600, T0),
        entryForProcess("P2", 500, T0),
        entryForProcess("P3", 400, T0),
        entryForProcess("P4", 300, T0),
        entryForProcess("P5", 200, T0),
        entryForProcess("P6", 100, T0)  // 6th — should be excluded
    );

    var model = chartBuilder.buildTopCases(entries);

    assertThat((List<?>) model.getData().getLabels()).hasSize(5);
    assertThat((List<?>) model.getData().getLabels()).doesNotContain("P6");
  }

  @Test
  void buildTopCases_nullProcessName_groupedUnderUnknown() {
    var e1 = entry("gpt-4o", 200, 100, 100, 1000, T0);  // no processName set
    var e2 = entry("gpt-4o", 100, 50,  50,  1000, T0);  // no processName set

    var model = chartBuilder.buildTopCases(List.of(e1, e2));
    var labels = (List<?>) model.getData().getLabels();
    var ds = (BarChartDataSet) model.getData().getDataSet().get(0);

    // Both null → merged under the same "unknown" label → 1 bar with 300 total
    assertThat(labels).hasSize(1);
    assertThat(ds.getData().get(0)).isEqualTo(300L);
  }

  // ── buildResponseTimeHistogram ───────────────────────────────────────────

  @Test
  void buildResponseTimeHistogram_bucketsByCutoffs() {
    var fast   = entry("gpt-4o", 100, 60, 40, 1000,  T0);  // < 5s  → bucket 0
    var medium = entry("gpt-4o", 100, 60, 40, 7000,  T0);  // 5-10s → bucket 1
    var slow   = entry("gpt-4o", 100, 60, 40, 20000, T0);  // > 15s → bucket 3

    var model = chartBuilder.buildResponseTimeHistogram(List.of(fast, medium, slow));
    var ds = (BarChartDataSet) model.getData().getDataSet().get(0);

    assertThat(ds.getData().get(0)).isEqualTo(1L); // < 5s
    assertThat(ds.getData().get(1)).isEqualTo(1L); // 5-10s
    assertThat(ds.getData().get(2)).isEqualTo(0L); // 10-15s
    assertThat(ds.getData().get(3)).isEqualTo(1L); // > 15s
  }

  @Test
  void buildResponseTimeHistogram_exactBoundary5s_goesToSecondBucket() {
    var boundary = entry("gpt-4o", 100, 60, 40, 5000, T0);  // exactly 5000ms → NOT < 5000

    var model = chartBuilder.buildResponseTimeHistogram(List.of(boundary));
    var ds = (BarChartDataSet) model.getData().getDataSet().get(0);

    assertThat(ds.getData().get(0)).isEqualTo(0L); // < 5s  — not included
    assertThat(ds.getData().get(1)).isEqualTo(1L); // 5-10s — included
  }

  @Test
  void buildResponseTimeHistogram_emptyList_allBucketsZero() {
    var model = chartBuilder.buildResponseTimeHistogram(List.of());
    var ds = (BarChartDataSet) model.getData().getDataSet().get(0);

    assertThat(ds.getData()).containsExactly(0L, 0L, 0L, 0L);
  }

  @Test
  void buildResponseTimeHistogram_hasExactlyFourLabels() {
    var model = chartBuilder.buildResponseTimeHistogram(List.of());

    assertThat((List<?>) model.getData().getLabels()).hasSize(4);
  }
}
