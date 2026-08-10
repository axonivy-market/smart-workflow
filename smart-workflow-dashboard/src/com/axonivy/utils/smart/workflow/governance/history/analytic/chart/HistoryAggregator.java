package com.axonivy.utils.smart.workflow.governance.history.analytic.chart;

import static com.axonivy.utils.smart.workflow.governance.history.analytic.chart.model.DashboardKpi.NO_DATA;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.axonivy.utils.smart.workflow.governance.history.ChatHistoryJsonParser;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;

import ch.ivyteam.ivy.environment.Ivy;

public class HistoryAggregator {

  public record TokenPair(long input, long output) {
    TokenPair add(TokenPair other) {
      return new TokenPair(input + other.input, output + other.output);
    }
  }

  private interface BucketMs {
    long FAST   = 5_000;
    long MEDIUM = 10_000;
    long SLOW   = 15_000;
  }

  private interface BucketIndex {
    int FAST   = 0;
    int MEDIUM = 1;
    int SLOW   = 2;
    int OVER   = 3;
  }

  private final int totalSessions;
  private final long totalTokens;
  private final long avgResponseMs;
  private final String topModel;
  private final Map<LocalDate, TokenPair> tokensByDay;
  private final Map<String, Long> countByModel;
  private final Map<String, Long> tokensByProcess;
  private final long[] responseTimeBuckets;

  private HistoryAggregator(int totalSessions, long totalTokens, long avgResponseMs, String topModel,
      Map<LocalDate, TokenPair> tokensByDay, Map<String, Long> countByModel,
      Map<String, Long> tokensByProcess, long[] responseTimeBuckets) {
    this.totalSessions = totalSessions;
    this.totalTokens = totalTokens;
    this.avgResponseMs = avgResponseMs;
    this.topModel = topModel;
    this.tokensByDay = tokensByDay;
    this.countByModel = countByModel;
    this.tokensByProcess = tokensByProcess;
    this.responseTimeBuckets = responseTimeBuckets;
  }

  public static HistoryAggregator of(List<AgentConversationEntry> entries) {
    if (entries.isEmpty()) {
      return new HistoryAggregator(0, 0L, 0L, NO_DATA,
          new TreeMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new long[BucketIndex.OVER + 1]);
    }

    long totalTokens = 0L;
    long durationSum = 0L;
    Map<LocalDate, TokenPair> tokensByDay = new TreeMap<>();
    Map<String, Long> countByModel = new LinkedHashMap<>();
    Map<String, Long> tokensByProcess = new LinkedHashMap<>();
    long[] responseTimeBuckets = new long[BucketIndex.OVER + 1];

    for (AgentConversationEntry e : entries) {
      ChatHistoryJsonParser.TokenUsage usage = ChatHistoryJsonParser.parseTokenUsage(e);
      totalTokens += usage.totalTokens();
      durationSum += usage.avgDurationMs();

      countByModel.merge(usage.modelName(), 1L, Long::sum);
      tokensByProcess.merge(resolveProcessName(e), usage.totalTokens(), Long::sum);

      responseTimeBuckets[bucketIndex(usage.avgDurationMs())]++;
      mergeTokensByDay(tokensByDay, e, usage);
    }

    return new HistoryAggregator(entries.size(), totalTokens, durationSum / entries.size(),
        resolveTopModel(countByModel), tokensByDay, countByModel, tokensByProcess, responseTimeBuckets);
  }

  private static String resolveProcessName(AgentConversationEntry entry) {
    String name = entry.getProcessName();
    return name != null && !name.isBlank() ? name : "";
  }

  private static void mergeTokensByDay(Map<LocalDate, TokenPair> map, AgentConversationEntry e,
      ChatHistoryJsonParser.TokenUsage usage) {
    if (e.getLastUpdated() == null) {
      return;
    }
    try {
      LocalDate date = LocalDateTime.parse(e.getLastUpdated()).toLocalDate();
      map.merge(date, new TokenPair(usage.inputTokens(), usage.outputTokens()), TokenPair::add);
    } catch (DateTimeParseException ex) {
      Ivy.log().warn("HistoryAggregator: unparseable date ''{0}'': {1}",
          e.getLastUpdated(), ex.getMessage());
    }
  }

  private static String resolveTopModel(Map<String, Long> countByModel) {
    return countByModel.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse(NO_DATA);
  }

  private static int bucketIndex(long ms) {
    return ms < BucketMs.FAST   ? BucketIndex.FAST
         : ms < BucketMs.MEDIUM ? BucketIndex.MEDIUM
         : ms < BucketMs.SLOW   ? BucketIndex.SLOW
         : BucketIndex.OVER;
  }

  public int getTotalSessions()               { return totalSessions; }
  public long getTotalTokens()                { return totalTokens; }
  public long getAvgResponseMs()              { return avgResponseMs; }
  public String getTopModel()                 { return topModel; }
  public Map<LocalDate, TokenPair> getTokensByDay() { return Collections.unmodifiableMap(tokensByDay); }
  public Map<String, Long> getCountByModel()      { return Collections.unmodifiableMap(countByModel); }
  public Map<String, Long> getTokensByProcess()   { return Collections.unmodifiableMap(tokensByProcess); }
  public long[] getResponseTimeBuckets()          { return responseTimeBuckets.clone(); }
}
