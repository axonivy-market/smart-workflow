package com.axonivy.utils.smart.workflow.governance.history.analytic.chart;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestResponseTimeChartBuilder {

  private static final ResponseTimeChartBuilder BUILDER = new ResponseTimeChartBuilder();

  @Test
  void build_emptyAggregator_structureCorrectAndAllBarsZero() {
    BarChartModel model = BUILDER.build(HistoryAggregator.of(List.of()));
    assertThat((List<?>) model.getData().getLabels()).hasSize(4);
    assertThat(dataValues(model)).hasSize(4).allMatch(v -> v.longValue() == 0L);
    assertThat((List<?>) dataSet(model).getBackgroundColor()).hasSize(4);
    assertThat(model.getOptions().getIndexAxis()).isEqualTo("y");
  }

  @ParameterizedTest(name = "{argumentSetNameOrArgumentsWithNames}")
  @MethodSource("bucketRoutingArgs")
  void build_singleEntry_correctBucketIncremented(long durationMs, int expectedBucket) {
    HistoryAggregator aggregator = HistoryAggregator.of(List.of(entry("m", 10, 5, 5, durationMs)));
    List<Number> values = dataValues(BUILDER.build(aggregator));
    for (int i = 0; i < values.size(); i++) {
      long expected = i == expectedBucket ? 1L : 0L;
      assertThat(values.get(i).longValue()).as("bucket[%d]", i).isEqualTo(expected);
    }
  }

  @SuppressWarnings("unused")
  static Stream<Arguments> bucketRoutingArgs() {
    return Stream.of(
        Arguments.argumentSet("under_5s_bucket0",      2_000L, 0),
        Arguments.argumentSet("exactly_5s_bucket1",    5_000L, 1),
        Arguments.argumentSet("exactly_10s_bucket2",  10_000L, 2),
        Arguments.argumentSet("exactly_15s_bucket3",  15_000L, 3),
        Arguments.argumentSet("over_15s_bucket3",     20_000L, 3)
    );
  }

  @Test
  void build_entriesAcrossAllBuckets_valuesMatchDistribution() {
    HistoryAggregator aggregator = HistoryAggregator.of(List.of(
        entry("m", 10, 5, 5,  1_000),   // bucket 0: < 5s
        entry("m", 10, 5, 5,  7_000),   // bucket 1: 5–10s
        entry("m", 10, 5, 5,  7_000),   // bucket 1: 5–10s
        entry("m", 10, 5, 5, 12_000),   // bucket 2: 10–15s
        entry("m", 10, 5, 5, 20_000)    // bucket 3: > 15s
    ));
    List<Number> values = dataValues(BUILDER.build(aggregator));
    assertThat(values.get(0).longValue()).isEqualTo(1L);
    assertThat(values.get(1).longValue()).isEqualTo(2L);
    assertThat(values.get(2).longValue()).isEqualTo(1L);
    assertThat(values.get(3).longValue()).isEqualTo(1L);
  }

  private static List<Number> dataValues(BarChartModel model) {
    return ((BarChartDataSet) model.getData().getDataSet().get(0)).getData();
  }

  private static BarChartDataSet dataSet(BarChartModel model) {
    return (BarChartDataSet) model.getData().getDataSet().get(0);
  }

  private static AgentConversationEntry entry(String model, long total, long input, long output, long durationMs) {
    var e = new AgentConversationEntry();
    e.setTokenUsageJson("[{\"modelName\":\"" + model + "\",\"totalTokens\":" + total
        + ",\"inputTokens\":" + input + ",\"outputTokens\":" + output
        + ",\"durationMs\":" + durationMs + "}]");
    return e;
  }
}
