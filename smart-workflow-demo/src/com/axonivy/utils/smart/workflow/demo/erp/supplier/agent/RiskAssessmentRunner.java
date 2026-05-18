package com.axonivy.utils.smart.workflow.demo.erp.supplier.agent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep.LogLineSeverity;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep.StepStatus;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.RiskLevel;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.SupplierRiskScore;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ValidationFinding;

public class RiskAssessmentRunner {

  private RiskAssessmentRunner() {
  }

  // ── f2: Init Risk Step ─────────────────────────────────────────────────────

  /** Creates and starts a new processing step for risk score calculation. */
  public static AgentProcessingStep initRiskStep() {
    AgentProcessingStep step = new AgentProcessingStep();
    step.setName("Risk Score Calculation");
    step.setStatus(StepStatus.RUNNING);
    step.setStartedAt(Instant.now());
    return step;
  }

  /**
   * Builds the document context string passed as query context to the AI risk
   * scoring model. Combines annual volume, document summaries, policy findings,
   * and cross-reference findings.
   */
  public static String buildRiskContext(Integer annualVolumeEur, String documentContext,
      PolicyValidationResult policyResult, PolicyValidationResult financialResult,
      List<LegalDocument> documents) {
    StringBuilder ctx = new StringBuilder();
    ctx.append("TODAY'S DATE: ").append(LocalDate.now()).append("\n");
    ctx.append("ANNUAL VOLUME (EUR): ")
       .append(annualVolumeEur != null ? annualVolumeEur : "not specified").append("\n");
    ctx.append("\nEXTRACTED DOCUMENT SUMMARIES:\n")
       .append(documentContext != null ? documentContext : "none");

    if (policyResult != null && policyResult.getFindings() != null) {
      ctx.append("\n\nPOLICY VALIDATION FINDINGS:\n");
      for (ValidationFinding f : policyResult.getFindings()) {
        ctx.append("- [").append(f.getSeverity()).append("] ").append(f.getMessage());
        if (f.getScore() > 0) {
          ctx.append(" (deduction: ").append(f.getScore()).append(" pts)");
        }
        ctx.append("\n");
      }
    }
    if (financialResult != null && financialResult.getFindings() != null) {
      ctx.append("\n\nFINANCIAL VALIDATION FINDINGS:\n");
      for (ValidationFinding f : financialResult.getFindings()) {
        ctx.append("- [").append(f.getSeverity()).append("] ").append(f.getMessage()).append("\n");
      }
    }

    ctx.append("\n\nREQUIRED DOCUMENTS STATUS:\n");
    ctx.append(buildDocumentStatusSection(documents));

    int fixedPolicyCompliance = ValidationRunner.computePolicyComplianceScore(policyResult);
    ctx.append("\n\nPOLICY_COMPLIANCE_SCORE_FIXED: ").append(fixedPolicyCompliance);
    ctx.append("\nUse this value as riskScore.policyCompliance in your response.");

    return ctx.toString();
  }

  /**
   * Builds the structured "REQUIRED DOCUMENTS STATUS" block injected into the
   * risk scoring context so the AI can reliably apply STEP 1 deductions for
   * missing required documents and certifications.
   */
  private static String buildDocumentStatusSection(List<LegalDocument> documents) {
    if (documents == null) {
      documents = java.util.Collections.emptyList();
    }
    boolean hasCommercialRegister = documents.stream()
        .anyMatch(d -> LegalDocumentType.COMMERCIAL_REGISTER.equals(d.getDocumentType()));
    boolean hasSelfDeclaration = documents.stream()
        .anyMatch(d -> LegalDocumentType.SELF_DECLARATION.equals(d.getDocumentType()));
    boolean hasAnnualReport = documents.stream()
        .anyMatch(d -> LegalDocumentType.ANNUAL_REPORT.equals(d.getDocumentType()));

    StringBuilder sb = new StringBuilder();
    sb.append("COMMERCIAL_REGISTER: ").append(hasCommercialRegister ? "PRESENT" : "MISSING").append("\n");
    sb.append("SELF_DECLARATION: ").append(hasSelfDeclaration ? "PRESENT" : "MISSING").append("\n");
    sb.append("ANNUAL_REPORT: ").append(hasAnnualReport ? "PRESENT" : "MISSING").append("\n");

    // Emit a single CERTIFICATION line (PRESENT/MISSING) so the AI applies the
    // -30 deduction exactly once. Individual cert details appear in EXTRACTED
    // DOCUMENT SUMMARIES and are used by STEP 2/3 for expiry checking.
    List<LegalDocument> certDocs = documents.stream()
        .filter(d -> LegalDocumentType.CERTIFICATION.equals(d.getDocumentType()))
        .collect(Collectors.toList());

    if (certDocs.isEmpty()) {
      sb.append("CERTIFICATION: MISSING\n");
    } else {
      String certNames = certDocs.stream()
          .map(LegalDocument::getFileName)
          .collect(Collectors.joining(", "));
      sb.append("CERTIFICATION: PRESENT (files: ").append(certNames).append(")\n");
    }
    return sb.toString();
  }

  /**
   * Overrides policy compliance in AI risk output with deterministic score
   * computed from policy findings and configured policy rules.
   */
  public static void applyFixedPolicyCompliance(RiskScoreResult riskScoreResult,
      PolicyValidationResult policyResult) {
    if (riskScoreResult == null || riskScoreResult.getRiskScore() == null) {
      return;
    }

    SupplierRiskScore score = riskScoreResult.getRiskScore();
    int fixedPolicyCompliance = ValidationRunner.computePolicyComplianceScore(policyResult);
    score.setPolicyCompliance(fixedPolicyCompliance);

    // Clamp AI-generated components to valid [0, 100] range — the LLM may return
    // values outside the described range (e.g. -30 for expired certs).
    score.setFinancialStability(Math.max(0, Math.min(100, score.getFinancialStability())));
    score.setCertValidity(Math.max(0, Math.min(100, score.getCertValidity())));

    int aggregate = (score.getFinancialStability() + score.getPolicyCompliance()
        + score.getCertValidity()) / 3;
    score.setAggregate(aggregate);
    score.setLevel(RiskLevel.fromScore(aggregate));

    switch (score.getLevel()) {
      case GREEN -> riskScoreResult.setRoutingDecision("APPROVAL");
      case YELLOW -> riskScoreResult.setRoutingDecision("CLARIFICATION");
      default -> riskScoreResult.setRoutingDecision("DECLINE");
    }
  }

  // ── Deterministic risk scoring (no AI) ──────────────────────────────────────

  /**
   * Computes a deterministic 4-component risk score without calling an AI model.
   * Implements the same scoring rules described in the CMS RiskScoringSystemPrompt.
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
    int financial = ValidationRunner.computeFinancialStabilityScore(financialResult);

    // COMPONENT 2 — policyCompliance (deterministic, derived from finding scores)
    int policyCompliance = ValidationRunner.computePolicyComplianceScore(policyResult);

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

    SupplierRiskScore score = new SupplierRiskScore(financial, policyCompliance, certValidity);
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
        "FAILURE".equalsIgnoreCase(f.getSeverity()) && (
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

  // ── f4: Finalize Risk Step ─────────────────────────────────────────────────

  /**
   * Completes the processing step with timing and per-component log lines,
   * then attaches it to the result. Called after the AI agent returns.
   */
  public static void finalizeRiskStep(AgentProcessingStep step, RiskScoreResult riskScoreResult) {
    step.setStatus(StepStatus.COMPLETED);
    step.setCompletedAt(Instant.now());
    if (step.getStartedAt() != null) {
      step.setDurationMs(step.getCompletedAt().toEpochMilli() - step.getStartedAt().toEpochMilli());
    }
    if (riskScoreResult == null || riskScoreResult.getRiskScore() == null) {
      return;
    }
    SupplierRiskScore score = riskScoreResult.getRiskScore();

    step.getLogLines().add(new AgentProcessingStep.LogLine(
        LogLineSeverity.OK, "Financial stability score: " + score.getFinancialStability() + "/100"));

    LogLineSeverity policySev = score.getPolicyCompliance() < 100 ? LogLineSeverity.WARNING : LogLineSeverity.OK;
    step.getLogLines().add(new AgentProcessingStep.LogLine(
        policySev, "Compliance score: " + score.getPolicyCompliance() + "/100"));

    LogLineSeverity certSev = score.getCertValidity() < 100 ? LogLineSeverity.WARNING : LogLineSeverity.OK;
    step.getLogLines().add(new AgentProcessingStep.LogLine(
        certSev, "Cert validity score: " + score.getCertValidity() + "/100"));

    LogLineSeverity aggSev = LogLineSeverity.OK;
    if (score.getLevel() == RiskLevel.RED) {
      aggSev = LogLineSeverity.ERROR;
    } else if (score.getLevel() == RiskLevel.YELLOW) {
      aggSev = LogLineSeverity.WARNING;
    }
    step.getLogLines().add(new AgentProcessingStep.LogLine(
        aggSev, "Aggregate risk score: " + score.getAggregate() + "/100 \u2014 " + score.getLevel().name()));
    step.getLogLines().add(new AgentProcessingStep.LogLine(
        aggSev, "Routing decision: -> " + riskScoreResult.getRoutingDecision() + " path"));

    riskScoreResult.setProcessingStep(step);
  }

  // ── f3err: Handle Risk Score Error ────────────────────────────────────────

  /**
   * Builds a fallback {@link RiskScoreResult} for the error boundary handler.
   * Sets DECLINE routing with zero scores and attaches the failed processing step.
   */
  public static RiskScoreResult buildErrorResult(AgentProcessingStep step, Throwable error) {
    AgentProcessingStep resolvedStep = step != null ? step : new AgentProcessingStep();
    resolvedStep.setName("Risk Score Calculation");
    resolvedStep.setStatus(StepStatus.FAILED);
    resolvedStep.setCompletedAt(Instant.now());
    if (resolvedStep.getStartedAt() != null) {
      resolvedStep.setDurationMs(
          resolvedStep.getCompletedAt().toEpochMilli() - resolvedStep.getStartedAt().toEpochMilli());
    }
    String errorMsg = error != null ? error.getMessage() : "Unknown risk scoring error";
    resolvedStep.getLogLines().add(new AgentProcessingStep.LogLine(
        LogLineSeverity.ERROR, "AI risk scoring failed: " + errorMsg));

    RiskScoreResult result = new RiskScoreResult();
    result.setRiskScore(new SupplierRiskScore(0, 0, 0));
    result.setRoutingDecision("DECLINE");
    result.setProcessingStep(resolvedStep);
    return result;
  }
}
