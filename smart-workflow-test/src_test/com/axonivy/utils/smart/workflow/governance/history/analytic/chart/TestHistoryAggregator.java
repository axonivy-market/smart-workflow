package com.axonivy.utils.smart.workflow.governance.history.analytic.chart;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.axonivy.utils.smart.workflow.governance.history.analytic.chart.model.DashboardKpi;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestHistoryAggregator {

  @Test
  void of_emptyList_returnsZeroStats() {
    HistoryAggregator stats = HistoryAggregator.of(List.of());
    assertThat(stats.getTotalSessions()).isZero();
    assertThat(stats.getTotalTokens()).isZero();
    assertThat(stats.getAvgResponseMs()).isZero();
    assertThat(stats.getTopModel()).isEqualTo(DashboardKpi.NO_DATA);
    assertThat(stats.getTokensByDay()).isEmpty();
    assertThat(stats.getCountByModel()).isEmpty();
    assertThat(stats.getTokensByProcess()).isEmpty();
    assertThat(stats.getResponseTimeBuckets()).containsOnly(0L);
  }

  @Test
  void of_singleEntry_aggregatesSessionsAndTokens() {
    var entry = entry("gpt-4", 100, 60, 40, 3_000);
    HistoryAggregator stats = HistoryAggregator.of(List.of(entry));
    assertThat(stats.getTotalSessions()).isEqualTo(1);
    assertThat(stats.getTotalTokens()).isEqualTo(100);
  }

  @ParameterizedTest(name = "{argumentSetNameOrArgumentsWithNames}")
  @MethodSource("avgResponseMsArgs")
  void of_avgResponseMs_calculatedCorrectly(List<AgentConversationEntry> entries, long expectedAvgMs) {
    assertThat(HistoryAggregator.of(entries).getAvgResponseMs()).isEqualTo(expectedAvgMs);
  }

  @SuppressWarnings("unused")
  static Stream<Arguments> avgResponseMsArgs() {
    return Stream.of(
        Arguments.argumentSet("single_entry_duration_returned",
            List.of(entry("gpt-4", 100, 60, 40, 3_000)),
            3_000L),
        Arguments.argumentSet("two_entries_duration_averaged",
            List.of(entry("gpt-4", 100, 60, 40, 2_000), entry("gpt-4", 50, 30, 20, 4_000)),
            3_000L)
    );
  }

  @ParameterizedTest(name = "{argumentSetNameOrArgumentsWithNames}")
  @MethodSource("topModelArgs")
  void of_topModel_mostFrequentModelSelected(List<AgentConversationEntry> entries, String expectedModel) {
    assertThat(HistoryAggregator.of(entries).getTopModel()).isEqualTo(expectedModel);
  }

  @SuppressWarnings("unused")
  static Stream<Arguments> topModelArgs() {
    return Stream.of(
        Arguments.argumentSet("single_model_returned",
            List.of(entry("gpt-4", 10, 5, 5, 1_000)),
            "gpt-4"),
        Arguments.argumentSet("two_models_most_frequent_returned",
            List.of(entry("gpt-4", 10, 5, 5, 1_000),
                entry("claude", 10, 5, 5, 1_000),
                entry("gpt-4",  10, 5, 5, 1_000)),
            "gpt-4")
    );
  }

  @Test
  void of_tokensByDay_twoEntriesSameDay_inputOutputSummed() {
    LocalDateTime today = LocalDateTime.now().withHour(9);
    HistoryAggregator stats = HistoryAggregator.of(List.of(
        entryWithDate("gpt-4", 60, 40, 20, 1_000, today),
        entryWithDate("gpt-4", 40, 15, 25, 1_000, today)));
    HistoryAggregator.TokenPair pair = stats.getTokensByDay().get(today.toLocalDate());
    assertThat(pair.input()).isEqualTo(55);
    assertThat(pair.output()).isEqualTo(45);
  }

  @Test
  void of_tokensByDay_twoDifferentDays_separateEntries() {
    LocalDateTime today     = LocalDateTime.now().withHour(9);
    LocalDateTime yesterday = today.minusDays(1);
    HistoryAggregator stats = HistoryAggregator.of(List.of(
        entryWithDate("gpt-4", 100, 60, 40, 1_000, today),
        entryWithDate("gpt-4",  50, 30, 20, 1_000, yesterday)));
    assertThat(stats.getTokensByDay()).hasSize(2);
    assertThat(stats.getTokensByDay()).containsKey(today.toLocalDate());
    assertThat(stats.getTokensByDay()).containsKey(yesterday.toLocalDate());
  }

  @Test
  void of_tokensByDay_outOfOrderDates_keysInChronologicalOrder() {
    LocalDateTime older = LocalDateTime.now().withHour(9).minusDays(2);
    LocalDateTime newer = LocalDateTime.now().withHour(9);
    HistoryAggregator stats = HistoryAggregator.of(List.of(
        entryWithDate("gpt-4", 100, 60, 40, 1_000, newer),
        entryWithDate("gpt-4",  50, 30, 20, 1_000, older)));
    List<LocalDate> keys = new ArrayList<>(stats.getTokensByDay().keySet());
    assertThat(keys.get(0)).isBefore(keys.get(1));
  }

  @Test
  void of_nullLastUpdated_excludedFromTokensByDay() {
    HistoryAggregator stats = HistoryAggregator.of(List.of(entry("gpt-4", 100, 60, 40, 1_000)));
    assertThat(stats.getTokensByDay()).isEmpty();
  }

  @ParameterizedTest(name = "{argumentSetNameOrArgumentsWithNames}")
  @MethodSource("processNameArgs")
  void of_processName_groupedCorrectly(String processName, String expectedKey) {
    var e = entry("gpt-4", 10, 5, 5, 1_000);
    e.setProcessName(processName);
    assertThat(HistoryAggregator.of(List.of(e)).getTokensByProcess()).containsKey(expectedKey);
  }

  @SuppressWarnings("unused")
  static Stream<Arguments> processNameArgs() {
    return Stream.of(
        Arguments.argumentSet("null_processName_groupedAsEmpty",  null,          ""),
        Arguments.argumentSet("blank_processName_groupedAsEmpty", " ",           ""),
        Arguments.argumentSet("valid_processName_preserved",      "OrderProcess", "OrderProcess")
    );
  }

  @ParameterizedTest(name = "{argumentSetNameOrArgumentsWithNames}")
  @MethodSource("bucketArgs")
  void of_responseTimeBuckets_classifiedCorrectly(long durationMs, int expectedBucket) {
    long[] buckets = HistoryAggregator.of(List.of(entry("gpt-4", 10, 5, 5, durationMs)))
        .getResponseTimeBuckets();
    assertThat(buckets[expectedBucket]).isEqualTo(1L);
    for (int i = 0; i < buckets.length; i++) {
      if (i != expectedBucket) {
        assertThat(buckets[i]).as("bucket[%d] should be 0", i).isZero();
      }
    }
  }

  @SuppressWarnings("unused")
  static Stream<Arguments> bucketArgs() {
    return Stream.of(
        Arguments.argumentSet("under_5s_bucket0",     4_999L, 0),
        Arguments.argumentSet("exactly_5s_bucket1",   5_000L, 1),
        Arguments.argumentSet("exactly_10s_bucket2", 10_000L, 2),
        Arguments.argumentSet("exactly_15s_bucket3", 15_000L, 3),
        Arguments.argumentSet("over_15s_bucket3",    20_000L, 3)
    );
  }

  static AgentConversationEntry entry(String model, long total, long input, long output, long durationMs) {
    var e = new AgentConversationEntry();
    e.setTokenUsageJson(tokenJson(model, total, input, output, durationMs));
    return e;
  }

  private static AgentConversationEntry entryWithDate(String model, long total, long input, long output,
      long durationMs, LocalDateTime date) {
    var e = entry(model, total, input, output, durationMs);
    e.setLastUpdated(date.toString());
    return e;
  }

  private static String tokenJson(String model, long total, long input, long output, long durationMs) {
    return "[{\"modelName\":\"" + model + "\",\"totalTokens\":" + total
        + ",\"inputTokens\":" + input + ",\"outputTokens\":" + output
        + ",\"durationMs\":" + durationMs + "}]";
  }
}
