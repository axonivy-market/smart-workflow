package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RuleType;
import com.axonivy.utils.smart.workflow.demo.mock.MockRules;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierPolicyRule;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.PolicyValidationResult;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.RiskScoreResult;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.SupplierRiskScore;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.agent.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder.LogLineBuilder;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder.SupplierRiskScoreBuilder;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AgentStepStatus;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.FindingSeverity;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.LogLineSeverity;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskLevel;

public class RiskAssessmentService {

  private static final String STEP_NAME_KEY = "StepRiskScoreCalculation";
  private static final String UNKNOWN_ERROR_MSG = "Unknown risk scoring error";
  private static final String RISK_FAIL_PREFIX = "Risk scoring failed: ";
  private static final String ROUTING_APPROVAL = "APPROVAL";
  private static final String ROUTING_CLARIFICATION = "CLARIFICATION";
  private static final String ROUTING_DECLINE = "DECLINE";
  private static final String FINANCIAL_SCORE_FORMAT = "Financial stability score: %d/100";
  private static final String COMPLIANCE_SCORE_FORMAT = "Compliance score: %d/100";
  private static final String CERT_SCORE_FORMAT = "Cert validity score: %d/100";
  private static final String AGGREGATE_SCORE_FORMAT = "Aggregate risk score: %d/100 - %s";
  private static final String ROUTING_FORMAT = "Routing decision: -> %s path";
  private static final Pattern CERT_EXPIRY_DATE_PATTERN = Pattern.compile(
      "valid[- ]until[:\\s]+([0-9]{4}-[0-9]{2}-[0-9]{2})", Pattern.CASE_INSENSITIVE);

  private RiskAssessmentService() {
  }

  public static AgentProcessingStep startRiskStep() {
    AgentProcessingStep step = new AgentProcessingStep();
    step.setName(ValidationUtils.stepName(STEP_NAME_KEY));
    step.setStatus(AgentStepStatus.RUNNING);
    step.setStartedAt(Instant.now());
    return step;
  }

  public static void finalizeRiskStep(AgentProcessingStep step, RiskScoreResult riskScoreResult) {
    step.setStatus(AgentStepStatus.COMPLETED);
    step.setCompletedAt(Instant.now());
    if (step.getStartedAt() != null) {
      step.setDurationMs(step.getCompletedAt().toEpochMilli() - step.getStartedAt().toEpochMilli());
    }
    if (step.getLogLines() == null) {
      step.setLogLines(new ArrayList<>());
    }
    if (riskScoreResult == null || riskScoreResult.getRiskScore() == null) {
      return;
    }
    SupplierRiskScore score = riskScoreResult.getRiskScore();

    step.getLogLines().add(LogLineBuilder.of(
        LogLineSeverity.OK, String.format(FINANCIAL_SCORE_FORMAT, score.getFinancialStability())));

    LogLineSeverity policySev = score.getPolicyCompliance() < 100 ? LogLineSeverity.WARNING : LogLineSeverity.OK;
    step.getLogLines().add(LogLineBuilder.of(
        policySev, String.format(COMPLIANCE_SCORE_FORMAT, score.getPolicyCompliance())));

    LogLineSeverity certSev = score.getCertValidity() < 100 ? LogLineSeverity.WARNING : LogLineSeverity.OK;
    step.getLogLines().add(LogLineBuilder.of(
        certSev, String.format(CERT_SCORE_FORMAT, score.getCertValidity())));

    LogLineSeverity aggSev = LogLineSeverity.OK;
    if (score.getLevel() == RiskLevel.RED) {
      aggSev = LogLineSeverity.ERROR;
    } else if (score.getLevel() == RiskLevel.YELLOW) {
      aggSev = LogLineSeverity.WARNING;
    }
    step.getLogLines().add(LogLineBuilder.of(
        aggSev, String.format(AGGREGATE_SCORE_FORMAT, score.getAggregate(), score.getLevel().name())));
    step.getLogLines().add(LogLineBuilder.of(
        aggSev, String.format(ROUTING_FORMAT, riskScoreResult.getRoutingDecision())));

    riskScoreResult.setProcessingStep(step);
  }

  public static RiskScoreResult failRiskStep(AgentProcessingStep step, Throwable error) {
    AgentProcessingStep resolvedStep = step != null ? step : new AgentProcessingStep();
    resolvedStep.setName(ValidationUtils.stepName(STEP_NAME_KEY));
    resolvedStep.setStatus(AgentStepStatus.FAILED);
    resolvedStep.setCompletedAt(Instant.now());
    if (resolvedStep.getStartedAt() != null) {
      resolvedStep.setDurationMs(
          resolvedStep.getCompletedAt().toEpochMilli() - resolvedStep.getStartedAt().toEpochMilli());
    }
    if (resolvedStep.getLogLines() == null) {
      resolvedStep.setLogLines(new ArrayList<>());
    }
    String errorMsg = error != null ? error.getMessage() : UNKNOWN_ERROR_MSG;
    resolvedStep.getLogLines().add(LogLineBuilder.of(
        LogLineSeverity.ERROR, RISK_FAIL_PREFIX + errorMsg));

    RiskScoreResult result = new RiskScoreResult();
    result.setRiskScore(SupplierRiskScoreBuilder.of(0, 0, 0));
    result.setRoutingDecision(ROUTING_DECLINE);
    result.setProcessingStep(resolvedStep);
    return result;
  }

  public static RiskScoreResult computeRiskScore(
      Integer annualVolumeEur,
      PolicyValidationResult policyResult,
      PolicyValidationResult financialResult,
      String caseUuid) {
    int financial = FinancialValidationService.computeFinancialStabilityScore(financialResult, caseUuid);
    int policyCompliance = PolicyValidationService.computePolicyComplianceScore(policyResult, caseUuid);
    int certValidity = computeCertValidityScore(policyResult, annualVolumeEur, caseUuid);

    SupplierRiskScore score = SupplierRiskScoreBuilder.of(financial, policyCompliance, certValidity);
    RiskScoreResult result = new RiskScoreResult();
    result.setRiskScore(score);
    switch (score.getLevel()) {
      case GREEN -> result.setRoutingDecision(ROUTING_APPROVAL);
      case YELLOW -> result.setRoutingDecision(ROUTING_CLARIFICATION);
      default -> result.setRoutingDecision(ROUTING_DECLINE);
    }
    return result;
  }

  private static int computeCertValidityScore(PolicyValidationResult policyResult,
      Integer annualVolumeEur, String caseUuid) {
    List<ValidationFinding> findings = policyResult != null && policyResult.getFindings() != null
        ? policyResult.getFindings() : Collections.emptyList();
    Map<String, Integer> certRules = loadCertValidityRuleScores(caseUuid);

    int score = 100;
    if (hasFailureFindingForKey(findings, LegalDocumentType.COMMERCIAL_REGISTER)) {
      score -= certRules.getOrDefault(MockRules.CERT_01_COMMERCIAL_REGISTER.name(), 0);
    }
    if (hasFailureFindingForKey(findings, LegalDocumentType.SELF_DECLARATION)) {
      score -= certRules.getOrDefault(MockRules.CERT_02_SELF_DECLARATION.name(), 0);
    }
    if (hasFailureFindingForKey(findings, LegalDocumentType.ANNUAL_REPORT)) {
      score -= certRules.getOrDefault(MockRules.CERT_03_ANNUAL_REPORT.name(), 0);
    }
    if (annualVolumeEur != null && annualVolumeEur > 50_000
        && hasFailureFindingForKey(findings, LegalDocumentType.CERTIFICATION)) {
      score -= certRules.getOrDefault(MockRules.CERT_04_CERTIFICATION_REQUIRED.name(), 0);
    }
    score -= computeCertExpiryDeduction(findings,
        certRules.getOrDefault(MockRules.CERT_05_CERT_EXPIRED.name(), 0),
        certRules.getOrDefault(MockRules.CERT_06_CERT_EXPIRING_SOON.name(), 0));
    return Math.max(0, Math.min(100, score));
  }

  private static Map<String, Integer> loadCertValidityRuleScores(String caseUuid) {
    Map<String, Integer> scores = new HashMap<>();
    for (SupplierPolicyRule rule : ValidationUtils.loadRulesByType(RuleType.CERT_VALIDITY, caseUuid)) {
      scores.put(rule.getTarget(), rule.getRiskScore());
    }
    return scores;
  }

  private static boolean hasFailureFindingForKey(List<ValidationFinding> findings, LegalDocumentType key) {
    return findings.stream().anyMatch(f ->
        f.getSeverity() == FindingSeverity.FAILURE && (
            (f.getSource() != null && f.getSource().toUpperCase().contains(key.name())) ||
            (f.getDocumentTypeKey() != null && f.getDocumentTypeKey().toUpperCase().contains(key.name()))
        )
    );
  }

  private static int computeCertExpiryDeduction(List<ValidationFinding> findings,
      int expiredDeduction, int soonDeduction) {
    int deduction = 0;
    LocalDate today = LocalDate.now();
    LocalDate soonThreshold = today.plusMonths(6);
    for (ValidationFinding f : findings) {
      if (f.getMessage() == null) {
        continue;
      }
      Matcher m = CERT_EXPIRY_DATE_PATTERN.matcher(f.getMessage());
      while (m.find()) {
        try {
          LocalDate validUntil = LocalDate.parse(m.group(1));
          if (today.isAfter(validUntil)) {
            deduction += expiredDeduction;
          } else if (!validUntil.isAfter(soonThreshold)) {
            deduction += soonDeduction;
          }
        } catch (Exception ignored) {
        }
      }
    }
    return deduction;
  }
}
