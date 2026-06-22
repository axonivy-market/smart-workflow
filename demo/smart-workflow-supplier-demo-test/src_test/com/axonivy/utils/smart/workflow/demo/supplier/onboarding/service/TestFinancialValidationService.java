package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.supplier.agent.PolicyValidationResult;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.agent.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AgentStepStatus;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.FindingSeverity;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskKind;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskType;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestFinancialValidationService {

  @Test
  void mergeFinancialRuleFindings_setsRiskTypeAndKind() {
    List<ValidationFinding> accumulated = new ArrayList<>();
    PolicyValidationResult ruleResult = new PolicyValidationResult();
    ValidationFinding finding = new ValidationFinding();
    finding.setMessage("Low revenue");
    finding.setSeverity(FindingSeverity.WARNING);
    ruleResult.setFindings(List.of(finding));

    FinancialValidationService.mergeFinancialRuleFindings(accumulated, ruleResult);

    assertThat(accumulated).hasSize(1);
    assertThat(accumulated.get(0).getRiskType()).isEqualTo(RiskType.FINANCIAL_STABILITY);
    assertThat(accumulated.get(0).getRiskKind()).isEqualTo(RiskKind.AI_VALIDATION);
  }

  @Test
  void startFinancialStep_statusIsRunning() {
    AgentProcessingStep step = FinancialValidationService.startFinancialStep();

    assertThat(step.getStatus()).isEqualTo(AgentStepStatus.RUNNING);
    assertThat(step.getStartedAt()).isNotNull();
  }

  @Test
  void finalizeFinancialStep_summarizesFindings() {
    AgentProcessingStep step1 = FinancialValidationService.startFinancialStep();
    assertThat(FinancialValidationService.finalizeFinancialStep(step1, null))
        .isEqualTo("All financial checks passed.");

    AgentProcessingStep step2 = FinancialValidationService.startFinancialStep();
    PolicyValidationResult result = new PolicyValidationResult();
    ValidationFinding finding = new ValidationFinding();
    finding.setSeverity(FindingSeverity.FAILURE);
    finding.setMessage("Debt ratio too high");
    result.setFindings(List.of(finding));
    assertThat(FinancialValidationService.finalizeFinancialStep(step2, result))
        .contains("Debt ratio too high");
    assertThat(result.getProcessingStep()).isNotNull();
  }

  @Test
  void finalizeFinancialValidation_wrapsAndComputes() {
    AgentProcessingStep step = FinancialValidationService.startFinancialStep();
    ValidationFinding finding = new ValidationFinding();
    finding.setSeverity(FindingSeverity.FAILURE);
    finding.setSource("Financial Check");
    finding.setScore(10);
    finding.setMessage("Revenue below threshold");

    PolicyValidationResult result = FinancialValidationService.finalizeFinancialValidation(
        List.of(finding), step, Ivy.wfCase().uuid());

    assertThat(result.getComplianceScore()).isEqualTo(90);
    assertThat(result.getProcessingStep()).isNotNull();
  }
}
