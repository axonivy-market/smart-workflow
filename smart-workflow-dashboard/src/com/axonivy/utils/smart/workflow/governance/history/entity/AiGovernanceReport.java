package com.axonivy.utils.smart.workflow.governance.history.entity;

import java.util.List;

public class AiGovernanceReport {

  private List<EfficiencyFinding> efficiencyOpportunities;
  private ReliabilityConcerns reliabilityConcerns;
  private ToolUsagePatterns toolUsagePatterns;
  private RiskAssessment riskAssessment;
  private List<Recommendation> recommendations;
  private String summary;

  public AiGovernanceReport() {}

  public List<EfficiencyFinding> getEfficiencyOpportunities() { return efficiencyOpportunities; }
  public void setEfficiencyOpportunities(List<EfficiencyFinding> efficiencyOpportunities) { this.efficiencyOpportunities = efficiencyOpportunities; }

  public ReliabilityConcerns getReliabilityConcerns() { return reliabilityConcerns; }
  public void setReliabilityConcerns(ReliabilityConcerns reliabilityConcerns) { this.reliabilityConcerns = reliabilityConcerns; }

  public ToolUsagePatterns getToolUsagePatterns() { return toolUsagePatterns; }
  public void setToolUsagePatterns(ToolUsagePatterns toolUsagePatterns) { this.toolUsagePatterns = toolUsagePatterns; }

  public RiskAssessment getRiskAssessment() { return riskAssessment; }
  public void setRiskAssessment(RiskAssessment riskAssessment) { this.riskAssessment = riskAssessment; }

  public List<Recommendation> getRecommendations() { return recommendations; }
  public void setRecommendations(List<Recommendation> recommendations) { this.recommendations = recommendations; }

  public String getSummary() { return summary; }
  public void setSummary(String summary) { this.summary = summary; }

  public record EfficiencyFinding(String agentRef, List<String> observations, List<String> suggestions) {
    public String getAgentRef() { return agentRef; }
    public List<String> getObservations() { return observations; }
    public List<String> getSuggestions() { return suggestions; }
  }

  public record ReliabilityConcerns(List<String> anomalies, List<String> errorsAndGuardrails, String conclusion) {
    public List<String> getAnomalies() { return anomalies; }
    public List<String> getErrorsAndGuardrails() { return errorsAndGuardrails; }
    public String getConclusion() { return conclusion; }
  }

  public record ToolUsagePatterns(List<String> toolsUsed, int totalCalls, List<String> observations, String insight) {
    public List<String> getToolsUsed() { return toolsUsed; }
    public int getTotalCalls() { return totalCalls; }
    public List<String> getObservations() { return observations; }
    public String getInsight() { return insight; }
  }

  public record RiskAssessment(RiskEntry operational, RiskEntry compliance, RiskEntry cost, RiskEntry reliability) {
    public RiskEntry getOperational() { return operational; }
    public RiskEntry getCompliance() { return compliance; }
    public RiskEntry getCost() { return cost; }
    public RiskEntry getReliability() { return reliability; }
  }

  public record RiskEntry(String level, String detail) {
    public String getLevel() { return level; }
    public String getDetail() { return detail; }
  }

  public record Recommendation(String title, List<String> actions) {
    public String getTitle() { return title; }
    public List<String> getActions() { return actions; }
  }
}
