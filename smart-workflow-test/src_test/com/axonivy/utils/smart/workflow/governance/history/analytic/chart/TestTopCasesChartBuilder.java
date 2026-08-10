package com.axonivy.utils.smart.workflow.governance.history.analytic.chart;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestTopCasesChartBuilder {

  private static final TopCasesChartBuilder BUILDER = new TopCasesChartBuilder();

  @Test
  void build_emptyAggregator_emptyChart() {
    BarChartModel model = BUILDER.build(HistoryAggregator.of(List.of()));
    assertThat((List<?>) model.getData().getLabels()).isEmpty();
    assertThat(dataValues(model)).isEmpty();
  }

  @Test
  void build_moreThanTopN_onlyTopNReturned() {
    List<AgentConversationEntry> entries = new ArrayList<>();
    String[] processes = {"A", "B", "C", "D", "E", "F"};
    long[] tokens      = {100, 90, 80, 70, 60, 50};
    for (int i = 0; i < processes.length; i++) {
      AgentConversationEntry e = entry("m", tokens[i], tokens[i] / 2, tokens[i] / 2, 1_000);
      e.setProcessName(processes[i]);
      entries.add(e);
    }
    BarChartModel model = BUILDER.build(HistoryAggregator.of(entries));
    assertThat((List<?>) model.getData().getLabels())
        .hasSize(AbstractChartBuilder.ChartConfig.TOP_N_PROCESSES);
  }

  @Test
  void build_twoProcesses_sortedByTokensDescending() {
    AgentConversationEntry low  = entry("m", 10, 5, 5, 1_000);
    AgentConversationEntry high = entry("m", 100, 50, 50, 1_000);
    low.setProcessName("LowProcess");
    high.setProcessName("HighProcess");
    List<?> labels = (List<?>) BUILDER.build(HistoryAggregator.of(List.of(low, high))).getData().getLabels();
    assertThat(labels.get(0)).isEqualTo("HighProcess");
    assertThat(labels.get(1)).isEqualTo("LowProcess");
  }

  @Test
  void build_nullProcessName_replacedWithNonEmptyLabel() {
    // null processName is normalized to "" by aggregator; chart builder replaces "" with CMS label
    HistoryAggregator aggregator = HistoryAggregator.of(List.of(entry("m", 10, 5, 5, 1_000)));
    List<?> labels = (List<?>) BUILDER.build(aggregator).getData().getLabels();
    assertThat(labels.get(0)).isNotEqualTo("");
  }

  private static List<Number> dataValues(BarChartModel model) {
    return ((BarChartDataSet) model.getData().getDataSet().get(0)).getData();
  }

  private static AgentConversationEntry entry(String model, long total, long input, long output, long durationMs) {
    var e = new AgentConversationEntry();
    e.setTokenUsageJson("[{\"modelName\":\"" + model + "\",\"totalTokens\":" + total
        + ",\"inputTokens\":" + input + ",\"outputTokens\":" + output
        + ",\"durationMs\":" + durationMs + "}]");
    return e;
  }
}
