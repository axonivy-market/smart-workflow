package com.axonivy.utils.smart.workflow.governance.history.analytic.chart;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.utils.DatePatternUtils;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestTokenStackedChartBuilder {

  private static final TokenStackedChartBuilder BUILDER = new TokenStackedChartBuilder();

  @Test
  void build_emptyAggregator_twoDatasetsMinDaysAllZero() {
    BarChartModel model = BUILDER.build(HistoryAggregator.of(List.of()));
    assertThat(model.getData().getDataSet()).hasSize(2);
    assertThat((List<?>) model.getData().getLabels())
        .hasSize(AbstractChartBuilder.ChartConfig.MIN_TIMELINE_DAYS);
    assertThat(inputValues(model)).allMatch(v -> v.longValue() == 0L);
    assertThat(outputValues(model)).allMatch(v -> v.longValue() == 0L);
  }

  @Test
  void build_singleEntry_inputAndOutputInSeparateDatasets() {
    LocalDateTime today = LocalDateTime.now().withHour(9);
    HistoryAggregator aggregator = HistoryAggregator.of(List.of(
        entryWithDate("m", 100, 40, 60, 1_000, today)));
    BarChartModel model = BUILDER.build(aggregator);
    String todayLabel = today.toLocalDate().format(DatePatternUtils.DAY_FMT);
    int idx = ((List<?>) model.getData().getLabels()).indexOf(todayLabel);
    assertThat(inputValues(model).get(idx).longValue()).isEqualTo(40L);
    assertThat(outputValues(model).get(idx).longValue()).isEqualTo(60L);
  }

  @Test
  void build_twoOutOfOrderEntries_labelsInChronologicalOrder() {
    LocalDateTime older = LocalDateTime.now().withHour(9).minusDays(3);
    LocalDateTime newer = LocalDateTime.now().withHour(9);
    HistoryAggregator aggregator = HistoryAggregator.of(List.of(
        entryWithDate("m", 10, 5, 5, 1_000, newer),
        entryWithDate("m", 10, 5, 5, 1_000, older)));
    List<?> labels = (List<?>) BUILDER.build(aggregator).getData().getLabels();
    String olderLabel = older.toLocalDate().format(DatePatternUtils.DAY_FMT);
    String newerLabel = newer.toLocalDate().format(DatePatternUtils.DAY_FMT);
    assertThat(labels.indexOf(olderLabel)).isLessThan(labels.indexOf(newerLabel));
  }

  private static List<Number> inputValues(BarChartModel model) {
    return ((BarChartDataSet) model.getData().getDataSet().get(0)).getData();
  }

  private static List<Number> outputValues(BarChartModel model) {
    return ((BarChartDataSet) model.getData().getDataSet().get(1)).getData();
  }

  private static AgentConversationEntry entryWithDate(String model, long total, long input, long output,
      long durationMs, LocalDateTime date) {
    var e = new AgentConversationEntry();
    e.setTokenUsageJson("[{\"modelName\":\"" + model + "\",\"totalTokens\":" + total
        + ",\"inputTokens\":" + input + ",\"outputTokens\":" + output
        + ",\"durationMs\":" + durationMs + "}]");
    e.setLastUpdated(date.toString());
    return e;
  }
}
