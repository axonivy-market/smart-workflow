package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity;

import java.util.List;

public class CaseStatistics {

  public String caseId;
  public String caseName;
  public String processName;
  public List<AgentSummary> summaries;

  public int agentCount;
  public int totalTokens;
  public long totalDurationMs;
  public int totalToolCalls;
  public int totalMessages;
  public long anomalyAgents;
  public int totalAnomalies;
  public int totalErrors;
  public int totalNullResults;
  public long lengthCount;
  public long distinctModels;

  public double avgTokensPerMsg;
  public double avgDurationPerAgent;

  public TokenUsageSummary tokenUsage;
  public double outputInputRatio;
  public double estInputCost;
  public double estOutputCost;

  public int toolSuccesses;
  public int overallToolSuccessRate;
  public List<ToolStats> toolStats;

  public List<AgentStats> agentStats;

  public AgentStats slowestAgent;
  public int slowestAgentPct;
  public AgentStats highestTokenAgent;
  public int highestTokenPct;
  public String modelList;
  public long unexpectedFinishCount;
  public int toolErrorRate;
  public List<AgentSummary> longConversationAgents;

  public CaseStatistics() {}

  public String getCaseId() { return caseId; }
  public String getCaseName() { return caseName; }
  public String getProcessName() { return processName; }
  public List<AgentSummary> getSummaries() { return summaries; }
  public int getAgentCount() { return agentCount; }
  public int getTotalTokens() { return totalTokens; }
  public long getTotalDurationMs() { return totalDurationMs; }
  public int getTotalToolCalls() { return totalToolCalls; }
  public int getTotalMessages() { return totalMessages; }
  public long getAnomalyAgents() { return anomalyAgents; }
  public int getTotalAnomalies() { return totalAnomalies; }
  public int getTotalErrors() { return totalErrors; }
  public int getTotalNullResults() { return totalNullResults; }
  public long getLengthCount() { return lengthCount; }
  public long getDistinctModels() { return distinctModels; }
  public double getAvgTokensPerMsg() { return avgTokensPerMsg; }
  public double getAvgDurationPerAgent() { return avgDurationPerAgent; }
  public TokenUsageSummary getTokenUsage() { return tokenUsage; }
  public double getOutputInputRatio() { return outputInputRatio; }
  public double getEstInputCost() { return estInputCost; }
  public double getEstOutputCost() { return estOutputCost; }
  public int getToolSuccesses() { return toolSuccesses; }
  public int getOverallToolSuccessRate() { return overallToolSuccessRate; }
  public List<ToolStats> getToolStats() { return toolStats; }
  public List<AgentStats> getAgentStats() { return agentStats; }
  public AgentStats getSlowestAgent() { return slowestAgent; }
  public int getSlowestAgentPct() { return slowestAgentPct; }
  public AgentStats getHighestTokenAgent() { return highestTokenAgent; }
  public int getHighestTokenPct() { return highestTokenPct; }
  public String getModelList() { return modelList; }
  public long getUnexpectedFinishCount() { return unexpectedFinishCount; }
  public int getToolErrorRate() { return toolErrorRate; }
  public List<AgentSummary> getLongConversationAgents() { return longConversationAgents; }

  public static final class ToolStats {
    private final String name;
    private final int calls;
    private final int successes;
    private final int nulls;
    private final int errors;
    private final int successRate;
    private final String grade;

    public ToolStats(String name, int calls, int successes, int nulls, int errors,
        int successRate, String grade) {
      this.name = name;
      this.calls = calls;
      this.successes = successes;
      this.nulls = nulls;
      this.errors = errors;
      this.successRate = successRate;
      this.grade = grade;
    }

    public String getName() { return name; }
    public int getCalls() { return calls; }
    public int getSuccesses() { return successes; }
    public int getNulls() { return nulls; }
    public int getErrors() { return errors; }
    public int getSuccessRate() { return successRate; }
    public String getGrade() { return grade; }
  }

  public static final class AgentStats {
    private final int index;
    private final String displayName;
    private final AgentSummary summary;
    private final double tokensPerMsg;
    private final double throughputTokensPerSec;
    private final int tokenSharePct;
    private final int timeSharePct;
    private final String grade;

    public AgentStats(int index, String displayName, AgentSummary summary,
        double tokensPerMsg, double throughputTokensPerSec,
        int tokenSharePct, int timeSharePct, String grade) {
      this.index = index;
      this.displayName = displayName;
      this.summary = summary;
      this.tokensPerMsg = tokensPerMsg;
      this.throughputTokensPerSec = throughputTokensPerSec;
      this.tokenSharePct = tokenSharePct;
      this.timeSharePct = timeSharePct;
      this.grade = grade;
    }

    public int getIndex() { return index; }
    public String getDisplayName() { return displayName; }
    public AgentSummary getSummary() { return summary; }
    public double getTokensPerMsg() { return tokensPerMsg; }
    public double getThroughputTokensPerSec() { return throughputTokensPerSec; }
    public int getTokenSharePct() { return tokenSharePct; }
    public int getTimeSharePct() { return timeSharePct; }
    public String getGrade() { return grade; }
  }
}
