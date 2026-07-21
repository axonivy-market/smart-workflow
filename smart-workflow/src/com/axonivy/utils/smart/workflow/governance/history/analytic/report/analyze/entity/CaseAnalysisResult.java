package com.axonivy.utils.smart.workflow.governance.history.analytic.report.analyze.entity;

import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.AgentSummary;
import com.axonivy.utils.smart.workflow.governance.history.entity.AiGovernanceReport;

public record CaseAnalysisResult(List<AgentSummary> summaries, AiGovernanceReport aiReport) {
}
