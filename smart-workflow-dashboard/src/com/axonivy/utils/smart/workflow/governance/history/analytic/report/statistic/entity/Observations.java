package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity;

import java.util.List;

public record Observations(
    AgentObservation slowestAgent,
    int slowestAgentTimeSharePct,
    AgentObservation highestTokenAgent,
    int highestTokenSharePct,
    List<String> models,
    long unexpectedFinishCount,
    List<AgentObservation> longConversationAgents) {

  public Observations {
    models = List.copyOf(models);
    longConversationAgents = List.copyOf(longConversationAgents);
  }

  public AgentObservation getSlowestAgent() {
    return slowestAgent;
  }
  public int getSlowestAgentTimeSharePct() {
    return slowestAgentTimeSharePct;
  }
  public AgentObservation getHighestTokenAgent() {
    return highestTokenAgent;
  }
  public int getHighestTokenSharePct() {
    return highestTokenSharePct;
  }
  public List<String> getModels() {
    return models;
  }
  public long getUnexpectedFinishCount() {
    return unexpectedFinishCount;
  }
  public List<AgentObservation> getLongConversationAgents() {
    return longConversationAgents;
  }

  public record AgentObservation(
      int index,
      String displayName,
      int messageCount,
      int totalTokens,
      long durationMs) {

    public int getIndex() {
      return index;
    }
    public String getDisplayName() {
      return displayName;
    }
    public int getMessageCount() {
      return messageCount;
    }
    public int getTotalTokens() {
      return totalTokens;
    }
    public long getDurationMs() {
      return durationMs;
    }
  }
}
