package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.axonivy.utils.smart.workflow.demo.supplier.agent.PolicyValidationResult;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskKind;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RuleType;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierPolicyRule;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.agent.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder.LogLineBuilder;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AgentStepStatus;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.FindingSeverity;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.LogLineSeverity;

public class FinancialValidationService {

  private static final String STEP_NAME_KEY = "StepFinancialValidation";
  private static final String ALL_CHECKS_PASSED = "All financial checks passed.";
  private static final String FINDING_SUMMARY_FORMAT = "[%s] %s\n";

  private FinancialValidationService() {
  }

  public static List<SupplierPolicyRule> loadFinancialRules(String caseUuid) {
    return ValidationUtils.loadRulesByType(RuleType.FINANCIAL, caseUuid);
  }

  public static void mergeFinancialRuleFindings(List<ValidationFinding> accumulated,
      PolicyValidationResult ruleResult) {
    if (ruleResult == null || ruleResult.getFindings() == null || accumulated == null) {
      return;
    }
    for (ValidationFinding f : ruleResult.getFindings()) {
      f.setRiskType(RiskType.FINANCIAL_STABILITY);
      f.setRiskKind(RiskKind.AI_VALIDATION);
      accumulated.add(f);
    }
  }

  public static int computeFinancialStabilityScore(PolicyValidationResult result, String caseUuid) {
    return ValidationUtils.computeComplianceScore(result, RuleType.FINANCIAL, caseUuid);
  }

  public static AgentProcessingStep startFinancialStep() {
    AgentProcessingStep step = new AgentProcessingStep();
    step.setName(ValidationUtils.stepName(STEP_NAME_KEY));
    step.setStatus(AgentStepStatus.RUNNING);
    step.setStartedAt(Instant.now().toEpochMilli());
    return step;
  }

  public static String finalizeFinancialStep(AgentProcessingStep step,
      PolicyValidationResult result) {
    step.setStatus(AgentStepStatus.COMPLETED);
    step.setCompletedAt(Instant.now().toEpochMilli());
    if (step.getStartedAt() != null) {
      step.setDurationMs(step.getCompletedAt() - step.getStartedAt());
    }
    if (step.getLogLines() == null) {
      step.setLogLines(new ArrayList<>());
    }

    if (result == null) {
      result = new PolicyValidationResult();
    }

    StringBuilder summary = new StringBuilder();
    if (result.getFindings() != null) {
      for (ValidationFinding finding : result.getFindings()) {
        FindingSeverity severity = finding.getSeverity();
        LogLineSeverity logSeverity = severity == FindingSeverity.FAILURE ? LogLineSeverity.ERROR
            : severity == FindingSeverity.WARNING ? LogLineSeverity.WARNING : LogLineSeverity.OK;
        if (severity == FindingSeverity.FAILURE || severity == FindingSeverity.WARNING) {
          summary.append(String.format(FINDING_SUMMARY_FORMAT, severity, finding.getMessage()));
        }
        step.getLogLines().add(LogLineBuilder.of(logSeverity, finding.getMessage()));
      }
    }
    if (step.getLogLines().isEmpty()) {
      step.getLogLines().add(LogLineBuilder.of(LogLineSeverity.OK, ALL_CHECKS_PASSED));
    }
    result.setProcessingStep(step);
    return summary.length() > 0 ? summary.toString() : ALL_CHECKS_PASSED;
  }

  public static PolicyValidationResult finalizeFinancialValidation(
      List<ValidationFinding> accumulatedFindings,
      AgentProcessingStep processingStep,
      String caseUuid) {
    PolicyValidationResult result = PolicyValidationService.wrapFindings(accumulatedFindings);
    finalizeFinancialStep(processingStep, result);
    result.setComplianceScore(computeFinancialStabilityScore(result, caseUuid));
    return result;
  }
}
