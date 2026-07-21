package com.axonivy.utils.smart.workflow.governance.history.entity;

import com.axonivy.utils.smart.workflow.utils.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ivyteam.ivy.environment.Ivy;

public class AiGovernanceReportEntry {

  private static final String WARN_SERIALIZE   = "AiGovernanceReportEntry: failed to serialize report for caseUuid=%s: %s";
  private static final String WARN_DESERIALIZE = "AiGovernanceReportEntry: failed to deserialize reportJson for caseUuid=%s: %s";

  private String caseUuid;
  private String generatedAt;
  private String reportJson;

  public String getCaseUuid() { return caseUuid; }
  public void setCaseUuid(String caseUuid) { this.caseUuid = caseUuid; }

  public String getGeneratedAt() { return generatedAt; }
  public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }

  public String getReportJson() { return reportJson; }
  public void setReportJson(String reportJson) { this.reportJson = reportJson; }

  public AiGovernanceReport getReport() {
    if (reportJson == null) {
      return null;
    }
    try {
      return JsonUtils.getObjectMapper().readValue(reportJson, AiGovernanceReport.class);
    } catch (JsonProcessingException e) {
      Ivy.log().warn(WARN_DESERIALIZE.formatted(caseUuid, e.getMessage()));
      return null;
    }
  }

  public void setReport(AiGovernanceReport report) {
    try {
      reportJson = JsonUtils.getObjectMapper().writeValueAsString(report);
    } catch (JsonProcessingException e) {
      Ivy.log().warn(WARN_SERIALIZE.formatted(caseUuid, e.getMessage()));
      reportJson = null;
    }
  }
}
