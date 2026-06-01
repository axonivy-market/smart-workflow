package com.axonivy.utils.smart.workflow.governance.history.entity.summary;

import java.util.List;

public record CaseAnalysisResult(List<AgentSummary> summaries, String aiReport) {
}
