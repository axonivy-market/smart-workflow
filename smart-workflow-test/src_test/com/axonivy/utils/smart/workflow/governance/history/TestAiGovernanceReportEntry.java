package com.axonivy.utils.smart.workflow.governance.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.entity.AiGovernanceReport;
import com.axonivy.utils.smart.workflow.governance.history.entity.AiGovernanceReport.EfficiencyFinding;
import com.axonivy.utils.smart.workflow.governance.history.entity.AiGovernanceReport.RiskAssessment;
import com.axonivy.utils.smart.workflow.governance.history.entity.AiGovernanceReport.RiskEntry;
import com.axonivy.utils.smart.workflow.governance.history.entity.AiGovernanceReportEntry;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestAiGovernanceReportEntry {

  @Test
  void setReport_validReport_reportJsonPopulated() {
    var entry = new AiGovernanceReportEntry();
    entry.setCaseUuid("case-1");
    entry.setReport(reportWithSummary("All good."));
    assertThat(entry.getReportJson()).isNotBlank();
    assertThat(entry.getReportJson()).contains("All good.");
  }

  @Test
  void getReport_afterSetReport_roundtripsCorrectly() {
    var entry = new AiGovernanceReportEntry();
    entry.setCaseUuid("case-1");
    entry.setReport(reportWithSummary("Round-trip summary"));
    var restored = entry.getReport();
    assertThat(restored).isNotNull();
    assertThat(restored.getSummary()).isEqualTo("Round-trip summary");
  }

  @Test
  void getReport_withNestedObjects_roundtripsCorrectly() {
    var report = new AiGovernanceReport();
    report.setSummary("Nested test");
    report.setRiskAssessment(new RiskAssessment(
        new RiskEntry("High", "Token overuse"),
        new RiskEntry("Low",  "No compliance issues"),
        new RiskEntry("Moderate", "Cost elevated"),
        new RiskEntry("Low", "Reliable")));
    report.setEfficiencyOpportunities(List.of(
        new EfficiencyFinding("agent-1", List.of("slow"), List.of("cache results"))));

    var entry = new AiGovernanceReportEntry();
    entry.setCaseUuid("case-2");
    entry.setReport(report);
    var restored = entry.getReport();

    assertThat(restored).isNotNull();
    assertThat(restored.getRiskAssessment().getOperational().getLevel()).isEqualTo("High");
    assertThat(restored.getRiskAssessment().getCompliance().getLevel()).isEqualTo("Low");
    assertThat(restored.getEfficiencyOpportunities()).hasSize(1);
    assertThat(restored.getEfficiencyOpportunities().get(0).getAgentRef()).isEqualTo("agent-1");
  }

  @Test
  void getReport_invalidJson_returnsNull() {
    var entry = new AiGovernanceReportEntry();
    entry.setCaseUuid("case-bad");
    entry.setReportJson("not-valid-json{{");
    assertThat(entry.getReport()).isNull();
  }

  @Test
  void getReport_nullJson_returnsNull() {
    var entry = new AiGovernanceReportEntry();
    entry.setCaseUuid("case-null");
    entry.setReportJson(null);
    assertThat(entry.getReport()).isNull();
  }

  @Test
  void setReport_nullReport_setsNullJson() {
    var entry = new AiGovernanceReportEntry();
    entry.setCaseUuid("case-1");
    entry.setReport(null);
    // null report serializes to JSON "null" which is valid — reportJson is not blank
    // but getReport() should return null when deserialized
    var restored = entry.getReport();
    assertThat(restored).isNull();
  }

  @Test
  void fields_setAndGetCorrectly() {
    var entry = new AiGovernanceReportEntry();
    entry.setCaseUuid("case-42");
    entry.setGeneratedAt("2025-01-15T10:30:00.123456789");
    entry.setReportJson("{\"summary\":\"manual\"}");

    assertThat(entry.getCaseUuid()).isEqualTo("case-42");
    assertThat(entry.getGeneratedAt()).isEqualTo("2025-01-15T10:30:00.123456789");
    assertThat(entry.getReportJson()).contains("manual");
  }

  private static AiGovernanceReport reportWithSummary(String summary) {
    var report = new AiGovernanceReport();
    report.setSummary(summary);
    return report;
  }
}
