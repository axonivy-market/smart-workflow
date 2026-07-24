package com.axonivy.utils.smart.workflow.governance.history.analytic.chart;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.primefaces.model.charts.donut.DonutChartModel;
import org.primefaces.model.charts.pie.PieChartDataSet;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestModelDistributionChartBuilder {

  private static final ModelDistributionChartBuilder BUILDER = new ModelDistributionChartBuilder();

  @Test
  void build_emptyAggregator_emptyLabelsAndData() {
    DonutChartModel model = BUILDER.build(HistoryAggregator.of(List.of()));
    assertThat((List<?>) model.getData().getLabels()).isEmpty();
    assertThat(dataValues(model)).isEmpty();
  }

  @Test
  void build_twoModels_labelsAndValuesPaired() {
    HistoryAggregator aggregator = HistoryAggregator.of(List.of(
        entry("gpt-4",  100, 60, 40, 1_000),
        entry("gpt-4",   50, 30, 20, 1_000),
        entry("claude",  80, 50, 30, 1_000)));
    DonutChartModel model = BUILDER.build(aggregator);
    List<?> labels = (List<?>) model.getData().getLabels();
    List<Number> values = dataValues(model);
    assertThat(labels).hasSize(2);
    assertThat(values).hasSize(2);
    int gptIdx    = labels.indexOf("gpt-4");
    int claudeIdx = labels.indexOf("claude");
    assertThat(gptIdx).isNotNegative();
    assertThat(claudeIdx).isNotNegative();
    assertThat(values.get(gptIdx).longValue()).isEqualTo(2L);
    assertThat(values.get(claudeIdx).longValue()).isEqualTo(1L);
  }

  private static List<Number> dataValues(DonutChartModel model) {
    return ((PieChartDataSet) model.getData().getDataSet().get(0)).getData();
  }

  private static AgentConversationEntry entry(String model, long total, long input, long output, long durationMs) {
    var e = new AgentConversationEntry();
    e.setTokenUsageJson("[{\"modelName\":\"" + model + "\",\"totalTokens\":" + total
        + ",\"inputTokens\":" + input + ",\"outputTokens\":" + output
        + ",\"durationMs\":" + durationMs + "}]");
    return e;
  }
}
