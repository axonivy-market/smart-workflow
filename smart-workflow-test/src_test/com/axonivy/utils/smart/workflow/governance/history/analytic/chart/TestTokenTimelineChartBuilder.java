package com.axonivy.utils.smart.workflow.governance.history.analytic.chart;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.utils.DatePatternUtils;

import ch.ivyteam.ivy.environment.IvyTest;

import software.xdev.chartjs.model.charts.BarChart;
import software.xdev.chartjs.model.dataset.BarDataset;

@IvyTest
public class TestTokenTimelineChartBuilder {

  private static final TokenTimelineChartBuilder BUILDER = new TokenTimelineChartBuilder();

  @Test
  void build_emptyAggregator_singleDatasetPaddedToMinDaysAllZero() {
    BarChart model = BUILDER.build(HistoryAggregator.of(List.of()));
    assertThat(model.getData().getDatasets()).hasSize(1);
    assertThat((List<?>) model.getData().getLabels())
        .hasSize(AbstractChartBuilder.ChartConfig.MIN_TIMELINE_DAYS);
    assertThat(dataValues(model))
        .hasSize(AbstractChartBuilder.ChartConfig.MIN_TIMELINE_DAYS)
        .allMatch(v -> v.longValue() == 0L);
  }

  @Test
  void build_twoOutOfOrderEntries_labelsInChronologicalOrder() {
    LocalDateTime older = LocalDateTime.now().withHour(9).minusDays(3);
    LocalDateTime newer = LocalDateTime.now().withHour(9);
    // intentionally newer first to expose ordering bug if TreeMap not used
    HistoryAggregator aggregator = HistoryAggregator.of(List.of(
        entryWithDate("m", 10, 5, 5, 1_000, newer),
        entryWithDate("m", 10, 5, 5, 1_000, older)));
    List<?> labels = (List<?>) BUILDER.build(aggregator).getData().getLabels();
    String olderLabel = older.toLocalDate().format(DatePatternUtils.DAY_FMT);
    String newerLabel = newer.toLocalDate().format(DatePatternUtils.DAY_FMT);
    assertThat(labels.indexOf(olderLabel)).isLessThan(labels.indexOf(newerLabel));
  }

  @Test
  void build_twoEntriesSameDay_inputAndOutputSummedInOneBar() {
    LocalDateTime today = LocalDateTime.now().withHour(9);
    HistoryAggregator aggregator = HistoryAggregator.of(List.of(
        entryWithDate("m", 70, 40, 30, 1_000, today),
        entryWithDate("m", 50, 20, 30, 1_000, today)));
    BarChart model = BUILDER.build(aggregator);
    String todayLabel = today.toLocalDate().format(DatePatternUtils.DAY_FMT);
    int idx = ((List<?>) model.getData().getLabels()).indexOf(todayLabel);
    // total = input(40+20) + output(30+30) = 120
    assertThat(dataValues(model).get(idx).longValue()).isEqualTo(120L);
  }

  private static List<Number> dataValues(BarChart model) {
    return ((BarDataset) model.getData().getDatasets().get(0)).getData(Number.class);
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
