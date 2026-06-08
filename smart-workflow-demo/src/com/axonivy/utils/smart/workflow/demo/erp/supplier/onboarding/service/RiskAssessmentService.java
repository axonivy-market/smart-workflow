package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.PolicyValidationResult;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.RiskScoreResult;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.builder.LogLineBuilder;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.builder.SupplierRiskScoreBuilder;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.AgentStepStatus;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.FindingSeverity;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.LogLineSeverity;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.RiskLevel;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.SupplierRiskScore;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ValidationFinding;

/**
 * Stateless helpers for the risk score calculation step of supplier onboarding.
 *
 * <p>Computes a deterministic 3-component risk score (financial stability,
 * policy compliance, certificate validity) without calling an AI model,
 * then derives the aggregate score and routing decision.</p>
 *
 * <p>All methods are stateless and static so that IvyScript in process Script
 * nodes can call them with a single import line.</p>
 */
public class RiskAssessmentService {

  private RiskAssessmentService() {
  }

  // ── Step lifecycle ─────────────────────────────────────────────────────────

  /** Creates and returns a new {@link AgentProcessingStep} for risk score
   * calculation, already in RUNNING state. */
  public static AgentProcessingStep startRiskStep() {
    AgentProcessingStep step = new AgentProcessingStep();
    step.setName(ValidationUtils.stepName("StepRiskScoreCalculation"));
    step.setStatus(AgentStepStatus.RUNNING);
    step.setStartedAt(Instant.now());
    return step;
  }

  /**
   * Completes the processing step with timing and per-component log lines,
   * then attaches it to the result.
   */
  public static void finalizeRiskStep(AgentProcessingStep step, RiskScoreResult riskScoreResult) {
    step.setStatus(AgentStepStatus.COMPLETED);
    step.setCompletedAt(Instant.now());
    if (step.getStartedAt() != null) {
      step.setDurationMs(step.getCompletedAt().toEpochMilli() - step.getStartedAt().toEpochMilli());
    }
    if (step.getLogLines() == null) {
      step.setLogLines(new java.util.ArrayList<>());
    }
    if (riskScoreResult == null || riskScoreResult.getRiskScore() == null) {
      return;
    }
    SupplierRiskScore score = riskScoreResult.getRiskScore();

    step.getLogLines().add(LogLineBuilder.of(
        LogLineSeverity.OK, "Financial stability score: " + score.getFinancialStability() + "/100"));

    LogLineSeverity policySev = score.getPolicyCompliance() < 100 ? LogLineSeverity.WARNING : LogLineSeverity.OK;
    step.getLogLines().add(LogLineBuilder.of(
        policySev, "Compliance score: " + score.getPolicyCompliance() + "/100"));

    LogLineSeverity certSev = score.getCertValidity() < 100 ? LogLineSeverity.WARNING : LogLineSeverity.OK;
    step.getLogLines().add(LogLineBuilder.of(
        certSev, "Cert validity score: " + score.getCertValidity() + "/100"));

    LogLineSeverity aggSev = LogLineSeverity.OK;
    if (score.getLevel() == RiskLevel.RED) {
      aggSev = LogLineSeverity.ERROR;
    } else if (score.getLevel() == RiskLevel.YELLOW) {
      aggSev = LogLineSeverity.WARNING;
    }
    step.getLogLines().add(LogLineBuilder.of(
        aggSev, "Aggregate risk score: " + score.getAggregate() + "/100 \u2014 " + score.getLevel().name()));
    step.getLogLines().add(LogLineBuilder.of(
        aggSev, "Routing decision: -> " + riskScoreResult.getRoutingDecision() + " path"));

    riskScoreResult.setProcessingStep(step);
  }

  /**
   * Builds a fallback {@link RiskScoreResult} for the error boundary handler.
   * Sets DECLINE routing with zero scores and attaches the failed processing step.
   */
  public static RiskScoreResult failRiskStep(AgentProcessingStep step, Throwable error) {
    AgentProcessingStep resolvedStep = step != null ? step : new AgentProcessingStep();
    resolvedStep.setName(ValidationUtils.stepName("StepRiskScoreCalculation"));
    resolvedStep.setStatus(AgentStepStatus.FAILED);
    resolvedStep.setCompletedAt(Instant.now());
    if (resolvedStep.getStartedAt() != null) {
      resolvedStep.setDurationMs(
          resolvedStep.getCompletedAt().toEpochMilli() - resolvedStep.getStartedAt().toEpochMilli());
    }
    if (resolvedStep.getLogLines() == null) {
      resolvedStep.setLogLines(new java.util.ArrayList<>());
    }
    String errorMsg = error != null ? error.getMessage() : "Unknown risk scoring error";
    resolvedStep.getLogLines().add(LogLineBuilder.of(
        LogLineSeverity.ERROR, "Risk scoring failed: " + errorMsg));

    RiskScoreResult result = new RiskScoreResult();
    result.setRiskScore(SupplierRiskScoreBuilder.of(0, 0, 0));
    result.setRoutingDecision("DECLINE");
    result.setProcessingStep(resolvedStep);
    return result;
  }

  // ── Deterministic risk scoring ─────────────────────────────────────────────

  /**
   * Computes a deterministic 3-component risk score without calling an AI model.
   * Implements the same scoring rules described in the CMS RiskScoringSystemPrompt.
   *
   * <p>Also called by
   * {@link com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.service.PolicyValidationService}
   * and
   * {@link com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.service.FinancialValidationService}.</p>
   */
  public static RiskScoreResult computeRiskScore(
      Integer annualVolumeEur,
      PolicyValidationResult policyResult,
      PolicyValidationResult financialResult) {

    List<ValidationFinding> policyFindings = policyResult != null && policyResult.getFindings() != null
        ? policyResult.getFindings() : java.util.Collections.emptyList();

    // Derive document presence from policy findings (populated by checkRequiredDocuments)
    boolean hasAnnualReport = !hasFailureFindingForKey(policyFindings, "ANNUAL_REPORT");
    boolean hasCommercialRegister = !hasFailureFindingForKey(policyFindings, "COMMERCIAL_REGISTER");
    boolean hasSelfDeclaration = !hasFailureFindingForKey(policyFindings, "SELF_DECLARATION");
    boolean hasCertification = !hasFailureFindingForKey(policyFindings, "CERTIFICATION");

    // COMPONENT 1 — financialStability (deterministic from financial rule findings)
    int financial = FinancialValidationService.computeFinancialStabilityScore(financialResult);

    // COMPONENT 2 — policyCompliance (deterministic, derived from finding scores)
    int policyCompliance = PolicyValidationService.computePolicyComplianceScore(policyResult);

    // COMPONENT 3 — certValidity (start 100)
    int certValidity = 100;
    if (!hasCommercialRegister) {
      certValidity -= 30;
    }
    if (!hasSelfDeclaration) {
      certValidity -= 15;
    }
    if (!hasAnnualReport) {
      certValidity -= 15;
    }
    boolean certRequired = annualVolumeEur != null && annualVolumeEur > 50_000;
    if (certRequired && !hasCertification) {
      certValidity -= 30;
    }
    certValidity -= computeCertExpiryDeduction(policyFindings);
    certValidity = Math.max(0, Math.min(100, certValidity));

    SupplierRiskScore score = SupplierRiskScoreBuilder.of(financial, policyCompliance, certValidity);
    RiskScoreResult result = new RiskScoreResult();
    result.setRiskScore(score);
    switch (score.getLevel()) {
      case GREEN -> result.setRoutingDecision("APPROVAL");
      case YELLOW -> result.setRoutingDecision("CLARIFICATION");
      default -> result.setRoutingDecision("DECLINE");
    }
    return result;
  }

  private static boolean hasFailureFindingForKey(List<ValidationFinding> findings, String key) {
    String normalizedKey = key.toUpperCase();
    return findings.stream().anyMatch(f ->
        f.getSeverity() == FindingSeverity.FAILURE && (
            (f.getSource() != null && f.getSource().toUpperCase().contains(normalizedKey)) ||
            (f.getDocumentTypeKey() != null && f.getDocumentTypeKey().toUpperCase().contains(normalizedKey))
        )
    );
  }

  private static int computeCertExpiryDeduction(List<ValidationFinding> findings) {
    int deduction = 0;
    LocalDate today = LocalDate.now();
    LocalDate soonThreshold = today.plusMonths(6);
    Pattern datePattern = Pattern.compile(
        "valid[- ]until[:\\s]+([0-9]{4}-[0-9]{2}-[0-9]{2})",
        Pattern.CASE_INSENSITIVE);
    for (ValidationFinding f : findings) {
      if (f.getMessage() == null) {
        continue;
      }
      Matcher m = datePattern.matcher(f.getMessage());
      while (m.find()) {
        try {
          LocalDate validUntil = LocalDate.parse(m.group(1));
          if (today.isAfter(validUntil)) {
            deduction += 40;
          } else if (!validUntil.isAfter(soonThreshold)) {
            deduction += 10;
          }
        } catch (Exception ignored) {
        }
      }
    }
    return deduction;
  }
}
