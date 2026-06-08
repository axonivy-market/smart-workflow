package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.PolicyValidationResult;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.RiskKind;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.RiskType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.RuleType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.SupplierPolicyRule;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.builder.LogLineBuilder;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.AgentStepStatus;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.FindingSeverity;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.LogLineSeverity;

/**
 * Helpers for the {@code validateFinancialPolicy} callable sub-process.
 *
 * <p>Note: the per-rule context/prompt helpers ({@link PolicyValidationService#buildRuleDocContext},
 * {@link PolicyValidationService#buildSingleRuleSystemPrompt},
 * {@link PolicyValidationService#hasRuleDocument}) are shared from
 * {@link PolicyValidationService} because they are rule-evaluation utilities,
 * not domain-specific to either policy or financial validation.</p>
 *
 * <p>All methods are stateless and static so that IvyScript in process Script
 * nodes can call them with a single import line.</p>
 */
public class FinancialValidationService {

  private FinancialValidationService() {
  }

  // ── Rule loading ─────────────────────────────────────────────────────────

  /**
   * Loads financial rules from repository as detached copies so runtime
   * mutation of {@code isPassed} does not persist globally.
   */
  public static List<SupplierPolicyRule> loadFinancialRules() {
    return ValidationUtils.loadRulesByType(RuleType.FINANCIAL);
  }

  // ── Findings management ──────────────────────────────────────────────────

  /**
   * Adds all findings from {@code ruleResult} into the {@code accumulated}
   * list, tagging each with {@link RiskType#FINANCIAL_STABILITY}.
   */
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

  // ── Scoring ──────────────────────────────────────────────────────────────

  /**
   * Computes deterministic financial stability score from financial findings
   * and per-rule risk deductions. Score is clamped to [0,100].
   *
   * <p>Also called by
   * {@link com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.service.RiskAssessmentService}.</p>
   */
  public static int computeFinancialStabilityScore(PolicyValidationResult result) {
    if (result == null || result.getFindings() == null || result.getFindings().isEmpty()) {
      return 100;
    }

    boolean hasExplicitScores = result.getFindings().stream().anyMatch(f -> f.getScore() != null && f.getScore() > 0);
    if (hasExplicitScores) {
      Map<String, Integer> maxScoreBySource = new HashMap<>();
      for (ValidationFinding f : result.getFindings()) {
        if (f.getScore() != null && f.getScore() > 0 && f.getSource() != null) {
          String key = ValidationUtils.normalizeKey(f.getSource());
          maxScoreBySource.merge(key, f.getScore(), Math::max);
        }
      }
      int totalDeduction = maxScoreBySource.values().stream().mapToInt(Integer::intValue).sum();
      return Math.max(0, Math.min(100, 100 - totalDeduction));
    }

    List<SupplierPolicyRule> rules = ValidationUtils.loadRulesByType(RuleType.FINANCIAL);
    if (rules.isEmpty()) {
      return 100;
    }
    Map<String, Integer> highestSeverityByTarget = ValidationUtils.resolveHighestSeverityByTarget(result, rules);
    int score = 100;
    for (SupplierPolicyRule rule : rules) {
      int severityRank = highestSeverityByTarget.getOrDefault(ValidationUtils.normalizeKey(rule.getTarget()), 0);
      if (severityRank >= 2) {
        score -= rule.getRiskScore();
      } else if (severityRank == 1) {
        score -= Math.round(rule.getRiskScore() / 2.0f);
      }
    }
    return Math.max(0, Math.min(100, score));
  }

  // ── Step lifecycle ───────────────────────────────────────────────────────

  /**
   * Creates and returns a new {@link AgentProcessingStep} for financial
   * validation, already in RUNNING state.
   */
  public static AgentProcessingStep startFinancialStep() {
    AgentProcessingStep step = new AgentProcessingStep();
    step.setName(ValidationUtils.stepName("StepFinancialValidation"));
    step.setStatus(AgentStepStatus.RUNNING);
    step.setStartedAt(Instant.now());
    return step;
  }

  /**
   * Marks the financial step COMPLETED, attaches it to the result, adds
   * per-finding log lines, and returns the plain-text findings summary.
   */
  public static String finalizeFinancialStep(AgentProcessingStep step,
      PolicyValidationResult result) {
    step.setStatus(AgentStepStatus.COMPLETED);
    step.setCompletedAt(Instant.now());
    if (step.getStartedAt() != null) {
      step.setDurationMs(step.getCompletedAt().toEpochMilli() - step.getStartedAt().toEpochMilli());
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
        FindingSeverity sev = finding.getSeverity();
        LogLineSeverity logSev = sev == FindingSeverity.FAILURE ? LogLineSeverity.ERROR
            : sev == FindingSeverity.WARNING ? LogLineSeverity.WARNING : LogLineSeverity.OK;
        if (sev == FindingSeverity.FAILURE || sev == FindingSeverity.WARNING) {
          summary.append("[").append(sev).append("] ")
                 .append(finding.getMessage()).append("\n");
        }
        step.getLogLines().add(LogLineBuilder.of(logSev, finding.getMessage()));
      }
    }
    if (step.getLogLines().isEmpty()) {
      step.getLogLines().add(LogLineBuilder.of(LogLineSeverity.OK, "All financial checks passed."));
    }
    result.setProcessingStep(step);
    return summary.length() > 0 ? summary.toString() : "All financial checks passed.";
  }

  // ── Full finalization ────────────────────────────────────────────────────

  /**
   * Convenience wrapper that performs the full financial finalization sequence:
   * wraps findings, finalizes the step, and computes the financial stability
   * score. Returns the fully-populated {@link PolicyValidationResult}.
   */
  public static PolicyValidationResult finalizeFinancialValidation(
      List<ValidationFinding> accumulatedFindings,
      AgentProcessingStep processingStep) {
    PolicyValidationResult result = PolicyValidationService.wrapFindings(accumulatedFindings);
    finalizeFinancialStep(processingStep, result);
    result.setComplianceScore(computeFinancialStabilityScore(result));
    return result;
  }
}
