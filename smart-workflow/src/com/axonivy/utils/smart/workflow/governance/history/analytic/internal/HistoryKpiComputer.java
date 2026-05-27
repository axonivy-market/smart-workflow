package com.axonivy.utils.smart.workflow.governance.history.analytic.internal;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.ui.model.DashboardKpi;
import com.axonivy.utils.smart.workflow.governance.utils.ChatHistoryJsonParser;

public class HistoryKpiComputer {

  private static final String NO_MODEL_LABEL = "\u2014";

  public DashboardKpi computeKpi(List<AgentConversationEntry> current, List<AgentConversationEntry> previous) {
    if (current.isEmpty()) {
      return DashboardKpi.empty();
    }
    int sessions = current.size();
    long tokens = current.stream().mapToLong(ChatHistoryJsonParser::getTotalTokens).sum();
    long avgMs = (long) current.stream()
        .mapToLong(ChatHistoryJsonParser::getAvgDurationMs)
        .average().orElse(0);

    Map<String, Long> modelCounts = current.stream()
        .collect(Collectors.groupingBy(ChatHistoryJsonParser::getModelName, Collectors.counting()));
    Map.Entry<String, Long> top = modelCounts.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .orElse(Map.entry(NO_MODEL_LABEL, 0L));
    int topPct = (int) Math.round(100.0 * top.getValue() / sessions);

    long prevTokens = previous.stream().mapToLong(ChatHistoryJsonParser::getTotalTokens).sum();
    long prevMs = (long) previous.stream()
        .mapToLong(ChatHistoryJsonParser::getAvgDurationMs)
        .average().orElse(0);

    return new DashboardKpi(sessions, tokens, avgMs, top.getKey(), topPct,
        trendPct(previous.size(), sessions),
        trendPct(prevTokens, tokens),
        trendPct(prevMs, avgMs));
  }

  private int trendPct(long oldVal, long newVal) {
    if (oldVal == 0) {
      return 0;
    }
    return (int) Math.round(100.0 * (newVal - oldVal) / oldVal);
  }
}
