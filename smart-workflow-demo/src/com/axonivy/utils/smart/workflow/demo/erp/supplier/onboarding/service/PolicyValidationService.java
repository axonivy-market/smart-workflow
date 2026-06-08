package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.DocumentExtractionResult;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.PolicyValidationResult;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.RiskKind;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.RiskType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.RuleType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.SupplierPolicyRule;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.builder.LogLineBuilder;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.builder.ValidationFindingBuilder;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.AgentStepStatus;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.FindingSeverity;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.LogLineSeverity;

import ch.ivyteam.ivy.environment.Ivy;

/**
 * Helpers for the {@code validateAgainstPolicy} callable sub-process.
 *
 * <p>Also owns the shared per-rule helpers ({@link #buildRuleDocContext},
 * {@link #buildSingleRuleSystemPrompt}, {@link #hasRuleDocument}) used by the
 * financial validation loop, and the public
 * {@link #computePolicyComplianceScore} called by
 * {@link com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.service.RiskAssessmentService}.</p>
 *
 * <p>All methods are stateless and static so that IvyScript in process Script
 * nodes can call them with a single import line.</p>
 */
public class PolicyValidationService {

  private static final Logger LOG = Logger.getLogger(PolicyValidationService.class.getName());

  private PolicyValidationService() {
  }

  // ── Rule loading ─────────────────────────────────────────────────────────

  /**
   * Loads policy rules from repository as detached copies so runtime mutation
   * of {@code isPassed} does not persist globally.
   */
  public static List<SupplierPolicyRule> loadPolicyRules() {
    return ValidationUtils.loadRulesByType(RuleType.POLICY);
  }

  // ── Document presence check ──────────────────────────────────────────────

  /**
   * Checks each required document type against the supplier's uploaded documents
   * and returns a pre-created ValidationFinding per required type: PASSED if present,
   * FAILURE if missing.
   */
  public static List<ValidationFinding> checkRequiredDocuments(List<LegalDocument> docs) {
    List<ValidationFinding> findings = new ArrayList<>();
    for (LegalDocumentType docType : LegalDocumentType.values()) {
      if (!docType.isRequired()) {
        continue;
      }
      boolean present = docs != null && docs.stream().anyMatch(d -> docType.equals(d.getDocumentType()));
      RiskKind messageKind = present ? RiskKind.AI_VALIDATION : RiskKind.MISSING_DOC;
      ValidationFinding f = ValidationFindingBuilder.of(
          present ? FindingSeverity.PASSED : FindingSeverity.FAILURE,
          Ivy.cms().co(messageKind.getCmsUri(), Arrays.asList(docType.getLabel())),
          docType.name(), RiskType.CERTIFICATION_VALIDITY);
      f.setDocumentTypeKey(docType.getDocumentTypeKey());
      f.setRiskKind(RiskKind.MISSING_DOC);
      findings.add(f);
    }
    return findings;
  }

  /**
   * Prepends pre-created presence findings to the AI-generated findings in the result.
   */
  public static void mergePresenceFindings(PolicyValidationResult result,
      List<ValidationFinding> presenceFindings) {
    if (result == null || presenceFindings == null || presenceFindings.isEmpty()) {
      return;
    }
    List<ValidationFinding> merged = new ArrayList<>(presenceFindings);
    if (result.getFindings() != null) {
      merged.addAll(result.getFindings());
    }
    result.setFindings(merged);
  }

  // ── Context building ─────────────────────────────────────────────────────

  /**
   * Builds the enriched document context for policy validation: extracted
   * document summaries + today's date + per-certification-type status.
   */
  public static String buildPolicyDocumentContext(OnboardingRequest request,
      DocumentExtractionResult extractionResult) {
    StringBuilder sb = new StringBuilder();

    if (extractionResult != null && extractionResult.getDocumentSummaries() != null
        && !extractionResult.getDocumentSummaries().isEmpty()) {
      for (DocumentExtractionResult.ExtractedDoc doc : extractionResult.getDocumentSummaries()) {
        sb.append("Document Type: ").append(doc.getDocumentType()).append("\n");
        sb.append("File Name: ").append(doc.getFileName()).append("\n");
        sb.append("Content:\n");
        if (doc.getContent() != null && !doc.getContent().isEmpty()) {
          sb.append(doc.getContent());
        } else {
          sb.append("(no content extracted)");
        }
        sb.append("\n\n");
      }
    } else {
      sb.append("No documents were extracted.");
    }

    sb.append("\nTODAY'S DATE: ").append(LocalDate.now());

    return sb.toString();
  }

  // ── Per-rule loop helpers (shared with financial validation loop) ─────────

  /**
   * Builds a focused context string for evaluating ONE policy rule.
   *
   * <p>Finds the {@link DocumentExtractionResult.ExtractedDoc} that matches
   * the rule's {@code certificationType} or {@code legalDocumentType}, then
   * formats: supplier context + matching document content. If no document is
   * matched, the supplier context alone is returned so the AI can still
   * evaluate non-document rules (e.g. geography).</p>
   *
   * <p>Also used by the financial validation loop in
   * {@code SupplierValidationAgent.p.json}.</p>
   */
  public static String buildRuleDocContext(SupplierPolicyRule rule,
      DocumentExtractionResult extractionResult, String supplierContext) {
    StringBuilder sb = new StringBuilder();
    sb.append("SUPPLIER CONTEXT:\n").append(supplierContext != null ? supplierContext : "N/A").append("\n\n");

    DocumentExtractionResult.ExtractedDoc matchingDoc = findMatchingDoc(rule, extractionResult);

    if (matchingDoc != null) {
      sb.append("DOCUMENT UNDER EVALUATION:\n");
      sb.append("File: ").append(matchingDoc.getFileName()).append("\n");
      sb.append("Type: ").append(matchingDoc.getDocumentType()).append("\n");
      if (matchingDoc.getExtractedFieldsSummary() != null && !matchingDoc.getExtractedFieldsSummary().isBlank()) {
        sb.append("Extracted Fields: ").append(matchingDoc.getExtractedFieldsSummary()).append("\n");
      }
      if (matchingDoc.getContent() != null && !matchingDoc.getContent().isBlank()) {
        String content = matchingDoc.getContent();
        if (content.length() > 3000) {
          content = content.substring(0, 3000) + "\n... [truncated]";
        }
        sb.append("Content:\n").append(content).append("\n");
      }
    } else {
      sb.append("DOCUMENT: No specific document found for this rule.\n");
      sb.append("Evaluate based on supplier context and available signals only.\n");
    }

    sb.append("\nTODAY'S DATE: ").append(LocalDate.now());
    return sb.toString();
  }

  /**
   * Builds a focused system prompt for evaluating a single policy rule.
   *
   * <p>Also used by the financial validation loop in
   * {@code SupplierValidationAgent.p.json}.</p>
   */
  public static String buildSingleRuleSystemPrompt(SupplierPolicyRule rule) {
    int fullDeduction = rule.getRiskScore();
    int halfDeduction = Math.round(fullDeduction / 2.0f);
    return """
        You are a supplier compliance auditor evaluating a single policy rule for supplier onboarding.

        RULE UNDER EVALUATION:
        Target: %s
        Policy: %s
        Risk Score Deduction: %d points if violated

        Based on the supplier context and document content provided, evaluate whether this rule is satisfied.
        Produce a PolicyValidationResult with one or more ValidationFinding entries:
        - severity: PASSED (rule met), WARNING (partial/advisory), or FAILURE (rule violated)
        - message: specific explanation relevant to this rule
        - source: use "%s" as the source
        - documentTypeKey: CERTIFICATION:<NAME> or DOCUMENT:<NAME> if document-relevant, otherwise null
        - score: points deducted from the compliance score — 0 for PASSED, %d for WARNING, %d for FAILURE

        Focus ONLY on this specific rule. Do not evaluate or invent findings for other rules."""
        .formatted(rule.getTarget(), rule.getRule(), fullDeduction, rule.getTarget(), halfDeduction, fullDeduction);
  }

  /**
   * Returns true if the rule should be evaluated by the AI.
   *
   * <ul>
   *   <li>Rules with no document type (e.g. geography rules) → always evaluate.</li>
   *   <li>Rules tied to a document type → evaluate only when a matching document
   *       is present in the extraction result.</li>
   * </ul>
   *
   * <p>Also used by the financial validation loop in
   * {@code SupplierValidationAgent.p.json}.</p>
   */
  public static boolean hasRuleDocument(SupplierPolicyRule rule,
      DocumentExtractionResult extractionResult) {
    if (rule.getCertificationType() == null && rule.getLegalDocumentType() == null) {
      return true;
    }
    return findMatchingDoc(rule, extractionResult) != null;
  }

  // ── Findings management ──────────────────────────────────────────────────

  /**
   * Filters an existing list of policy findings for re-use on a re-run.
   *
   * <p>Removes findings where {@code resolved == true} AND
   * {@code riskKind == MISSING_DOC}. All other findings are kept so they
   * pre-populate {@code accumulatedFindings} and lock in already-evaluated
   * rules.</p>
   */
  public static List<ValidationFinding> filterExistingFindings(
      List<ValidationFinding> existingFindings) {
    if (existingFindings == null || existingFindings.isEmpty()) {
      return new ArrayList<>();
    }
    List<ValidationFinding> kept = new ArrayList<>();
    for (ValidationFinding f : existingFindings) {
      if (Boolean.TRUE.equals(f.getResolved()) && RiskKind.MISSING_DOC.equals(f.getRiskKind())) {
        continue;
      }
      kept.add(f);
    }
    return kept;
  }

  /**
   * Returns {@code true} when the filtered existing findings already contain
   * at least one entry whose {@code source} matches {@code rule.getTarget()}
   * (case-insensitive, trimmed). Used to skip the AI call for already-evaluated
   * rules during a re-run.
   */
  public static boolean isRuleAlreadyEvaluated(SupplierPolicyRule rule,
      List<ValidationFinding> filteredExistingFindings) {
    if (rule == null || filteredExistingFindings == null || filteredExistingFindings.isEmpty()) {
      return false;
    }
    String ruleTarget = ValidationUtils.normalizeKey(rule.getTarget());
    for (ValidationFinding f : filteredExistingFindings) {
      if (ruleTarget.equals(ValidationUtils.normalizeKey(f.getSource()))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns a mutable copy of {@code filteredExistingFindings} to use as the
   * initial value for {@code accumulatedFindings} in the policy rule loop.
   */
  public static List<ValidationFinding> initAccumulatedFromExisting(
      List<ValidationFinding> filteredExistingFindings) {
    if (filteredExistingFindings == null || filteredExistingFindings.isEmpty()) {
      return new ArrayList<>();
    }
    return new ArrayList<>(filteredExistingFindings);
  }

  /**
   * Adds all findings from {@code ruleResult} into the {@code accumulated}
   * list, skipping duplicates by {@code documentTypeKey} or {@code source}.
   */
  public static void mergeRuleFindings(List<ValidationFinding> accumulated,
      PolicyValidationResult ruleResult) {
    if (ruleResult == null || ruleResult.getFindings() == null || accumulated == null) {
      return;
    }
    for (ValidationFinding f : ruleResult.getFindings()) {
      f.setRiskType(RiskType.POLICY_COMPLIANCE);
      f.setRiskKind(RiskKind.AI_VALIDATION);
      if (!isDuplicate(accumulated, f)) {
        accumulated.add(f);
      }
    }
  }

  /**
   * Wraps a list of findings into a new {@link PolicyValidationResult},
   * deduplicating by message content (preserves first occurrence).
   */
  public static PolicyValidationResult wrapFindings(List<ValidationFinding> findings) {
    PolicyValidationResult result = new PolicyValidationResult();
    findings = findings != null ? findings : new ArrayList<>();
    List<ValidationFinding> distinct = new ArrayList<>();
    Set<String> seenMessages = new LinkedHashSet<>();
    for (ValidationFinding f : findings) {
      if (seenMessages.add(f.getMessage())) {
        f.setUserExplanation(null);
        distinct.add(f);
      }
    }
    result.setFindings(distinct);
    return result;
  }

  // ── Rule evaluation and scoring ──────────────────────────────────────────

  /**
   * Evaluates configured policy rules against policy findings and sets
   * {@code isPassed} per rule in returned detached rule copies.
   */
  public static List<SupplierPolicyRule> evaluatePolicyRules(PolicyValidationResult result) {
    List<SupplierPolicyRule> rules = loadPolicyRules();
    Map<String, Integer> highestSeverityByTarget = ValidationUtils.resolveHighestSeverityByTarget(result, rules);
    for (SupplierPolicyRule rule : rules) {
      int severityRank = highestSeverityByTarget.getOrDefault(ValidationUtils.normalizeKey(rule.getTarget()), 0);
      rule.setPassed(severityRank == 0);
    }
    return rules;
  }

  /**
   * Computes deterministic policy compliance score from policy findings and
   * per-rule risk deductions.
   *
   * <p>FAILURE =&gt; full deduction, WARNING =&gt; half deduction, PASSED =&gt; no
   * deduction. Score is clamped to [0,100].</p>
   *
   * <p>Also called by
   * {@link com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.service.RiskAssessmentService}.</p>
   */
  public static int computePolicyComplianceScore(PolicyValidationResult result) {
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

    List<SupplierPolicyRule> rules = loadPolicyRules();
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
   * Creates and returns a new {@link AgentProcessingStep} for policy
   * validation, already in RUNNING state.
   */
  public static AgentProcessingStep startPolicyStep() {
    AgentProcessingStep step = new AgentProcessingStep();
    step.setName(ValidationUtils.stepName("StepPolicyValidation"));
    step.setStatus(AgentStepStatus.RUNNING);
    step.setStartedAt(Instant.now());
    return step;
  }

  /**
   * Marks the policy step COMPLETED, attaches it to the result, adds
   * per-finding log lines, and returns the plain-text findings summary.
   */
  public static String finalizePolicyStep(AgentProcessingStep step,
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
        if (sev == FindingSeverity.PASSED) {
          continue;
        }
        LogLineSeverity logSev = sev == FindingSeverity.FAILURE ? LogLineSeverity.ERROR
            : sev == FindingSeverity.WARNING ? LogLineSeverity.WARNING : LogLineSeverity.OK;
        step.getLogLines().add(LogLineBuilder.of(logSev, finding.getMessage()));
        summary.append("[").append(sev).append("] ")
               .append(finding.getMessage()).append("\n");
      }
    }
    if (step.getLogLines().isEmpty()) {
      int count = loadPolicyRules().size();
      String summaryMsg = count > 0
          ? "All " + count + " policy rules passed."
          : "All policy checks passed.";
      step.getLogLines().add(LogLineBuilder.of(LogLineSeverity.OK, summaryMsg));
    }
    result.setProcessingStep(step);
    return summary.length() > 0 ? summary.toString() : "All policy checks passed.";
  }

  /**
   * Marks the policy step FAILED, attaches it to the result, and returns a
   * plain-text error summary.
   */
  public static String failPolicyStep(AgentProcessingStep step,
      PolicyValidationResult result, Throwable error) {
    step.setName(ValidationUtils.stepName("StepPolicyValidation"));
    step.setStatus(AgentStepStatus.FAILED);
    step.setCompletedAt(Instant.now());
    if (step.getStartedAt() != null) {
      step.setDurationMs(step.getCompletedAt().toEpochMilli() - step.getStartedAt().toEpochMilli());
    }
    if (step.getLogLines() == null) {
      step.setLogLines(new ArrayList<>());
    }
    String msg = error != null ? error.getMessage() : "Unknown policy validation error";
    LOG.log(Level.SEVERE, "Policy validation failed: " + msg, error);
    step.getLogLines().add(LogLineBuilder.of(LogLineSeverity.ERROR,
        "Policy validation failed: " + msg));
    ValidationFinding errorFinding = ValidationFindingBuilder.of(
        FindingSeverity.FAILURE, "Policy validation could not complete: " + msg, "system", RiskType.POLICY_COMPLIANCE);
    errorFinding.setRiskKind(RiskKind.AI_VALIDATION);
    result.getFindings().add(errorFinding);
    result.setProcessingStep(step);
    return "Policy validation failed: " + msg;
  }

  // ── Full finalization ────────────────────────────────────────────────────

  /**
   * Convenience wrapper that performs the full policy finalization sequence:
   * wraps findings, merges presence findings, finalizes the step, evaluates
   * rules, computes the compliance score, and updates the onboarding request.
   */
  public static PolicyValidationResult finalizePolicyValidation(
      List<ValidationFinding> accumulatedFindings,
      List<ValidationFinding> presenceFindings,
      AgentProcessingStep processingStep,
      OnboardingRequest onboardingRequest) {
    PolicyValidationResult result = wrapFindings(accumulatedFindings);
    mergePresenceFindings(result, presenceFindings);
    finalizePolicyStep(processingStep, result);
    result.setRuleEvaluations(evaluatePolicyRules(result));
    result.setComplianceScore(computePolicyComplianceScore(result));
    if (onboardingRequest != null) {
      onboardingRequest.setPolicyValidationFindings(result.getFindings());
    }
    return result;
  }

  // ── Private helpers ──────────────────────────────────────────────────────

  private static DocumentExtractionResult.ExtractedDoc findMatchingDoc(
      SupplierPolicyRule rule, DocumentExtractionResult extractionResult) {
    if (extractionResult == null || extractionResult.getDocumentSummaries() == null
        || extractionResult.getDocumentSummaries().isEmpty()) {
      return null;
    }
    List<DocumentExtractionResult.ExtractedDoc> docs = extractionResult.getDocumentSummaries();

    if (rule.getCertificationType() != null) {
      return docs.stream()
          .filter(d -> rule.getCertificationType().equals(d.getDocumentType()))
          .findFirst().orElse(null);
    }
    if (rule.getLegalDocumentType() != null) {
      return docs.stream()
          .filter(d -> d.getDocumentType() == rule.getLegalDocumentType())
          .findFirst().orElse(null);
    }
    return null;
  }

  private static boolean isDuplicate(List<ValidationFinding> accumulated, ValidationFinding incoming) {
    String docKey = incoming.getDocumentTypeKey();
    String source = incoming.getSource();
    for (ValidationFinding existing : accumulated) {
      if (docKey != null) {
        if (docKey.equalsIgnoreCase(existing.getDocumentTypeKey())) {
          return true;
        }
      } else if (source != null && source.equalsIgnoreCase(existing.getSource())
          && existing.getDocumentTypeKey() == null) {
        return true;
      }
    }
    return false;
  }
}
