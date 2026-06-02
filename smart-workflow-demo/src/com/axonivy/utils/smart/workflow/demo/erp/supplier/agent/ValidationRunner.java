package com.axonivy.utils.smart.workflow.demo.erp.supplier.agent;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentRepository;
import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.RiskKind;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.RiskType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.RuleType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.SupplierPolicyRule;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep.LogLineSeverity;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep.StepStatus;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.FindingSeverity;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.repository.SupplierPolicyRuleRepository;

import ch.ivyteam.ivy.environment.Ivy;

/**
 * Runner helper for document extraction and policy validation steps in the
 * supplier onboarding validation agent.
 *
 * <p>Mirrors the {@link CrossReferenceRunner} pattern: all IvyScript in the
 * process scripts is replaced with simple one-liner runner calls.</p>
 */
public class ValidationRunner {

  private static final Logger LOG = Logger.getLogger(ValidationRunner.class.getName());

  private ValidationRunner() {
  }

  // ── Document Extraction phase ────────────────────────────────────────────

  /**
   * Loads all legal documents for the given supplier from the repository.
   * Returns an empty list when none are found (never null).
   */
  public static List<LegalDocument> loadDocuments(String supplierId) {
    List<LegalDocument> docs = LegalDocumentRepository.getInstance().findByObjectId(supplierId);
    return docs != null ? docs : java.util.Collections.emptyList();
  }

  /**
   * Returns labels of all required documents for supplier onboarding —
   * filtered by isRequired() on LegalDocumentType (includes cert sub-types).
   */
  public static List<String> loadRequiredDocumentTypes() {
    List<String> required = new ArrayList<>();
    for (LegalDocumentType docType : LegalDocumentType.values()) {
      if (docType.isRequired()) {
        required.add(docType.getLabel());
      }
    }
    return required;
  }

  /**
   * Builds a single-document context string for one {@link LegalDocument},
   * using the same format as {@link #buildDocumentContext(String)}.
   */
  public static String buildSingleDocumentContext(LegalDocument doc) {
    if (doc == null) {
      return "No document provided.";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("File: ").append(doc.getFileName());
    sb.append(", Type: ").append(doc.getDocumentType());
    if (LegalDocumentType.CERTIFICATION.equals(doc.getDocumentType()) && doc.getDescription() != null) {
      sb.append(", CertificationType: ").append(doc.getDescription());
    }
    sb.append(", Description: ").append(doc.getDescription() != null ? doc.getDescription() : "n/a");
    if (doc.getFileContent() != null && doc.getFileContent().length > 0) {
      String text = new String(doc.getFileContent(), StandardCharsets.UTF_8);
      long nonPrintable = text.chars()
          .filter(c -> c < 32 && c != '\n' && c != '\r' && c != '\t').count();
      if (nonPrintable <= text.length() * 0.05) {
        int limit = Math.min(text.length(), 3000);
        sb.append(", Content: ").append(text, 0, limit);
        if (text.length() > limit) {
          sb.append("...[truncated]");
        }
      }
    }
    return sb.toString();
  }

  /**
   * Merges all {@link DocumentExtractionResult.ExtractedDoc} entries from
   * {@code single} into {@code aggregate}. Both parameters may be null.
   */
  public static void mergeExtractionResult(DocumentExtractionResult aggregate,
      DocumentExtractionResult single, LegalDocument original) {
    if (aggregate == null || single == null) {
      return;
    }
    if (single.getDocumentSummaries() != null) {
      single.getDocumentSummaries().forEach(doc -> {
          doc.setDocumentType(original.getDocumentType());
      });
      aggregate.getDocumentSummaries().addAll(single.getDocumentSummaries());
    }
  }

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
      ValidationFinding f = new ValidationFinding(
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

  /**
   * Loads all legal documents for the given supplier and returns a formatted
   * document context string to be injected into the AI extraction query.
   */
  public static String buildDocumentContext(String supplierId) {
    List<LegalDocument> docs = LegalDocumentRepository.getInstance().findByObjectId(supplierId);
    if (docs == null || docs.isEmpty()) {
      return "No documents found for supplier: " + supplierId;
    }
    StringBuilder sb = new StringBuilder();
    for (LegalDocument doc : docs) {
      sb.append("File: ").append(doc.getFileName());
      sb.append(", Type: ").append(doc.getDocumentType());
      sb.append(", Description: ").append(doc.getDescription() != null ? doc.getDescription() : "n/a");
      if (doc.getFileContent() != null && doc.getFileContent().length > 0) {
        String text = new String(doc.getFileContent(), StandardCharsets.UTF_8);
        long nonPrintable = text.chars()
            .filter(c -> c < 32 && c != '\n' && c != '\r' && c != '\t').count();
        if (nonPrintable <= text.length() * 0.05) {
          int limit = Math.min(text.length(), 3000);
          sb.append(", Content: ").append(text, 0, limit);
          if (text.length() > limit) {
            sb.append("...[truncated]");
          }
        }
      }
      sb.append("\n");
    }
    return sb.toString();
  }

  /**
   * Creates and returns a new {@link AgentProcessingStep} for document
   * extraction, already in RUNNING state.
   */
  public static AgentProcessingStep startExtractionStep() {
    AgentProcessingStep step = new AgentProcessingStep();
    step.setName("Document Extraction");
    step.setStatus(StepStatus.RUNNING);
    step.setStartedAt(Instant.now());
    return step;
  }

  /**
   * Marks the extraction step COMPLETED, attaches log lines for each
   * extracted document, and links it to the result.
   */
  public static void finalizeExtractionStep(AgentProcessingStep step,
      DocumentExtractionResult result) {
    step.setStatus(StepStatus.COMPLETED);
    step.setCompletedAt(Instant.now());
    if (step.getStartedAt() != null) {
      step.setDurationMs(step.getCompletedAt().toEpochMilli() - step.getStartedAt().toEpochMilli());
    }
    if (result != null && result.getDocumentSummaries() != null) {
      for (DocumentExtractionResult.ExtractedDoc doc : result.getDocumentSummaries()) {
        if (doc.getDocumentType() != null) {
          buildLogForDocumentType(step, doc);
        }
      }
    }
    if (result != null) {
      result.setProcessingStep(step);
    }
  }

  private static void buildLogForDocumentType(AgentProcessingStep step, DocumentExtractionResult.ExtractedDoc doc) {
    // Line 1: file identification
    String header = (doc.getFileName() != null && !doc.getFileName().isEmpty())
        ? doc.getFileName() + " [" + (doc.getDocumentType() != null ? doc.getDocumentType().getLabel() : LegalDocumentType.OTHER.getLabel()) + "]"
        : (doc.getDocumentType() != null ? doc.getDocumentType().getLabel() : LegalDocumentType.OTHER.getLabel());
    step.getLogLines().add(new AgentProcessingStep.LogLine(LogLineSeverity.OK, header));
    // Line 2: extracted fields (prefer extractedFieldsSummary, fall back to note)
    String detail = (doc.getExtractedFieldsSummary() != null && !doc.getExtractedFieldsSummary().isEmpty())
        ? doc.getExtractedFieldsSummary()
        : (doc.getNote() != null && !doc.getNote().isEmpty() ? doc.getNote() : null);
    if (detail != null) {
      step.getLogLines().add(new AgentProcessingStep.LogLine(LogLineSeverity.OK, detail, true));
    }
  }

  /**
   * Marks the extraction step FAILED, attaches it to the result, logs the
   * error, and returns a plain-text error summary.
   */
  public static String failExtractionStep(AgentProcessingStep step,
      DocumentExtractionResult result, Throwable error) {
    step.setName("Document Extraction");
    step.setStatus(StepStatus.FAILED);
    step.setCompletedAt(Instant.now());
    if (step.getStartedAt() != null) {
      step.setDurationMs(step.getCompletedAt().toEpochMilli() - step.getStartedAt().toEpochMilli());
    }
    String msg = error != null ? error.getMessage() : "Unknown extraction error";
    LOG.log(Level.SEVERE, "Document extraction failed: " + msg, error);
    step.getLogLines().add(new AgentProcessingStep.LogLine(LogLineSeverity.ERROR,
        "Extraction failed: " + msg));
    result.setProcessingStep(step);
    return "Document extraction failed: " + msg;
  }

  // ── Policy Validation phase ──────────────────────────────────────────────

  /**
   * Loads policy rules from repository as detached copies so runtime mutation
   * of {@code isPassed} does not persist globally.
   */
  public static List<SupplierPolicyRule> loadPolicyRules() {
    return loadRulesByType(RuleType.POLICY);
  }

  public static List<SupplierPolicyRule> loadFinancialRules() {
    return loadRulesByType(RuleType.FINANCIAL);
  }

  private static List<SupplierPolicyRule> loadRulesByType(RuleType type) {
    List<SupplierPolicyRule> stored = SupplierPolicyRuleRepository.getInstance().findAllOrdered();
    List<SupplierPolicyRule> detached = new ArrayList<>();
    for (SupplierPolicyRule rule : stored) {
      if (!type.equals(rule.getRuleType())) {
        continue;
      }
      SupplierPolicyRule copy = new SupplierPolicyRule(
          rule.getTarget(), rule.getRule(), rule.getRiskScore(), false, rule.getRuleType());
      copy.setLegalDocumentType(rule.getLegalDocumentType());
      copy.setCertificationType(rule.getCertificationType());
      detached.add(copy);
    }
    return detached;
  }

  /**
   * Builds the enriched document context for policy validation: extracted
   * document summaries + today's date + per-certification-type status.
   */
  public static String buildPolicyDocumentContext(OnboardingRequest request,
      DocumentExtractionResult extractionResult) {
    StringBuilder sb = new StringBuilder();

    // Extracted document type, name, and full content
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

  /**
   * Creates and returns a new {@link AgentProcessingStep} for policy
   * validation, already in RUNNING state.
   */
  public static AgentProcessingStep startPolicyStep() {
    AgentProcessingStep step = new AgentProcessingStep();
    step.setName("Policy Validation");
    step.setStatus(StepStatus.RUNNING);
    step.setStartedAt(Instant.now());
    return step;
  }

  /**
   * Marks the policy step COMPLETED, attaches it to the result, adds
   * per-finding log lines, and returns the plain-text findings summary.
   */
  public static String finalizePolicyStep(AgentProcessingStep step,
      PolicyValidationResult result) {
    step.setStatus(StepStatus.COMPLETED);
    step.setCompletedAt(Instant.now());
    if (step.getStartedAt() != null) {
      step.setDurationMs(step.getCompletedAt().toEpochMilli() - step.getStartedAt().toEpochMilli());
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
        step.getLogLines().add(new AgentProcessingStep.LogLine(logSev, finding.getMessage()));
        summary.append("[").append(sev).append("] ")
               .append(finding.getMessage()).append("\n");
      }
    }
    if (step.getLogLines().isEmpty()) {
      int count = loadPolicyRules().size();
      String summaryMsg = count > 0
          ? "All " + count + " policy rules passed."
          : "All policy checks passed.";
      step.getLogLines().add(new AgentProcessingStep.LogLine(LogLineSeverity.OK, summaryMsg));
    }
    result.setProcessingStep(step);
    return summary.length() > 0 ? summary.toString() : "All policy checks passed.";
  }

  /**
   * Creates and returns a new {@link AgentProcessingStep} for financial
   * validation, already in RUNNING state.
   */
  public static AgentProcessingStep startFinancialStep() {
    AgentProcessingStep step = new AgentProcessingStep();
    step.setName("Financial Validation");
    step.setStatus(StepStatus.RUNNING);
    step.setStartedAt(Instant.now());
    return step;
  }

  /**
   * Marks the financial step COMPLETED, attaches it to the result, adds
   * per-finding log lines, and returns the plain-text findings summary.
   */
  public static String finalizeFinancialStep(AgentProcessingStep step,
      PolicyValidationResult result) {
    step.setStatus(StepStatus.COMPLETED);
    step.setCompletedAt(Instant.now());
    if (step.getStartedAt() != null) {
      step.setDurationMs(step.getCompletedAt().toEpochMilli() - step.getStartedAt().toEpochMilli());
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
        step.getLogLines().add(new AgentProcessingStep.LogLine(logSev, finding.getMessage()));
      }
    }
    if (step.getLogLines().isEmpty()) {
      step.getLogLines().add(new AgentProcessingStep.LogLine(LogLineSeverity.OK, "All financial checks passed."));
    }
    result.setProcessingStep(step);
    return summary.length() > 0 ? summary.toString() : "All financial checks passed.";
  }

  /**
   * Computes deterministic financial stability score from financial findings
   * and per-rule risk deductions. Identical logic to
   * {@link #computePolicyComplianceScore} but scoped to financial rules.
   */
  public static int computeFinancialStabilityScore(PolicyValidationResult result) {
    if (result == null || result.getFindings() == null || result.getFindings().isEmpty()) {
      return 100;
    }

    boolean hasExplicitScores = result.getFindings().stream().anyMatch(f -> f.getScore() > 0);
    if (hasExplicitScores) {
      Map<String, Integer> maxScoreBySource = new HashMap<>();
      for (ValidationFinding f : result.getFindings()) {
        if (f.getScore() > 0 && f.getSource() != null) {
          String key = normalizeKey(f.getSource());
          maxScoreBySource.merge(key, f.getScore(), Math::max);
        }
      }
      int totalDeduction = maxScoreBySource.values().stream().mapToInt(Integer::intValue).sum();
      return Math.max(0, Math.min(100, 100 - totalDeduction));
    }

    List<SupplierPolicyRule> rules = loadFinancialRules();
    if (rules.isEmpty()) {
      return 100;
    }
    Map<String, Integer> highestSeverityByTarget = resolveHighestSeverityByTarget(result, rules);
    int score = 100;
    for (SupplierPolicyRule rule : rules) {
      int severityRank = highestSeverityByTarget.getOrDefault(normalizeKey(rule.getTarget()), 0);
      if (severityRank >= 2) {
        score -= rule.getRiskScore();
      } else if (severityRank == 1) {
        score -= Math.round(rule.getRiskScore() / 2.0f);
      }
    }
    return Math.max(0, Math.min(100, score));
  }

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

  /**
   * Evaluates configured policy rules against policy findings and sets
   * {@code isPassed} per rule in returned detached rule copies.
   */
  public static List<SupplierPolicyRule> evaluatePolicyRules(PolicyValidationResult result) {
    List<SupplierPolicyRule> rules = loadPolicyRules();
    Map<String, Integer> highestSeverityByTarget = resolveHighestSeverityByTarget(result, rules);
    for (SupplierPolicyRule rule : rules) {
      int severityRank = highestSeverityByTarget.getOrDefault(normalizeKey(rule.getTarget()), 0);
      rule.setPassed(severityRank == 0);
    }
    return rules;
  }

  /**
   * Computes deterministic policy compliance score from policy findings and
   * per-rule risk deductions.
   *
   * <p>FAILURE => full deduction, WARNING => half deduction, PASSED => no
   * deduction. Score is clamped to [0,100].</p>
   */
  public static int computePolicyComplianceScore(PolicyValidationResult result) {
    if (result == null || result.getFindings() == null || result.getFindings().isEmpty()) {
      return 100;
    }

    // When findings carry explicit scores (populated by the AI via buildSingleRuleSystemPrompt),
    // sum the maximum deduction per source to avoid double-counting multiple findings for the
    // same rule, then subtract from 100.
    boolean hasExplicitScores = result.getFindings().stream().anyMatch(f -> f.getScore() > 0);
    if (hasExplicitScores) {
      Map<String, Integer> maxScoreBySource = new HashMap<>();
      for (ValidationFinding f : result.getFindings()) {
        if (f.getScore() > 0 && f.getSource() != null) {
          String key = normalizeKey(f.getSource());
          maxScoreBySource.merge(key, f.getScore(), Math::max);
        }
      }
      int totalDeduction = maxScoreBySource.values().stream().mapToInt(Integer::intValue).sum();
      return Math.max(0, Math.min(100, 100 - totalDeduction));
    }

    // Legacy fallback: re-derive deductions from severity + rule config (used when findings
    // were created before the score field existed, e.g. presence/cross-reference findings).
    List<SupplierPolicyRule> rules = loadPolicyRules();
    if (rules.isEmpty()) {
      return 100;
    }
    Map<String, Integer> highestSeverityByTarget = resolveHighestSeverityByTarget(result, rules);
    int score = 100;
    for (SupplierPolicyRule rule : rules) {
      int severityRank = highestSeverityByTarget.getOrDefault(normalizeKey(rule.getTarget()), 0);
      if (severityRank >= 2) {
        score -= rule.getRiskScore();
      } else if (severityRank == 1) {
        score -= Math.round(rule.getRiskScore() / 2.0f);
      }
    }
    return Math.max(0, Math.min(100, score));
  }

  private static Map<String, Integer> resolveHighestSeverityByTarget(
      PolicyValidationResult result, List<SupplierPolicyRule> rules) {
    Map<String, Integer> highestByTarget = new HashMap<>();
    if (result == null || result.getFindings() == null || rules == null || rules.isEmpty()) {
      return highestByTarget;
    }

    for (ValidationFinding finding : result.getFindings()) {
      String target = resolveTargetFromFinding(finding, rules);
      if (target == null) {
        continue;
      }
      int rank = finding.getSeverity() != null ? finding.getSeverity().rank : 0;
      if (rank <= 0) {
        continue;
      }
      highestByTarget.merge(target, rank, Math::max);
    }
    return highestByTarget;
  }

  private static String resolveTargetFromFinding(ValidationFinding finding,
      List<SupplierPolicyRule> rules) {
    if (finding == null || finding.getSource() == null || rules == null) {
      return null;
    }
    String source = normalizeKey(finding.getSource());
    if (source.isEmpty()) {
      return null;
    }

    for (SupplierPolicyRule rule : rules) {
      String target = normalizeKey(rule.getTarget());
      if (source.equals(target) || source.contains(target)) {
        return target;
      }
    }
    return null;
  }

  private static String normalizeKey(String key) {
    return key != null ? key.trim().toUpperCase() : "";
  }

  // ── Per-rule policy loop helpers ─────────────────────────────────────────

  /**
   * Builds a focused context string for evaluating ONE policy rule.
   *
   * <p>Finds the {@link DocumentExtractionResult.ExtractedDoc} that matches
   * the rule's {@code certificationType} or {@code legalDocumentType} by
   * filename substring, then formats: supplier context + matching document
   * content. If no document is matched, the supplier context alone is
   * returned so the AI can still evaluate non-document rules (e.g.
   * geography).</p>
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

  /**
   * Returns true if the rule should be evaluated by the AI.
   *
   * <ul>
   *   <li>Rules with no document type (e.g. geography rules) → always evaluate.</li>
   *   <li>Rules tied to a {@code certificationType} or {@code legalDocumentType} →
   *       evaluate only when a matching document is present in the extraction result.</li>
   * </ul>
   */
  public static boolean hasRuleDocument(SupplierPolicyRule rule,
      DocumentExtractionResult extractionResult) {
    if (rule.getCertificationType() == null && rule.getLegalDocumentType() == null) {
      return true; // non-document rule — always run
    }
    return findMatchingDoc(rule, extractionResult) != null;
  }

  /**
   * Builds a focused system prompt for evaluating a single policy rule.
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
   * Filters an existing list of policy findings for re-use on a re-run.
   *
   * <p>Removes findings where {@code resolved == true} AND {@code riskKind == MISSING_DOC}.
   * All other findings (including unresolved MISSING_DOC and all AI_VALIDATION findings)
   * are kept so they pre-populate {@code accumulatedFindings} and lock in already-evaluated rules.</p>
   *
   * @return a mutable copy; never null.
   */
  public static List<ValidationFinding> filterExistingFindings(
      List<ValidationFinding> existingFindings) {
    if (existingFindings == null || existingFindings.isEmpty()) {
      return new ArrayList<>();
    }
    List<ValidationFinding> kept = new ArrayList<>();
    for (ValidationFinding f : existingFindings) {
      if (f.isResolved() && RiskKind.MISSING_DOC.equals(f.getRiskKind())) {
        continue;
      }
      kept.add(f);
    }
    return kept;
  }

  /**
   * Returns {@code true} when the filtered existing findings already contain at least one entry
   * whose {@code source} matches {@code rule.getTarget()} (case-insensitive, trimmed).
   * Used to skip the AI call for already-evaluated rules during a re-run.
   */
  public static boolean isRuleAlreadyEvaluated(SupplierPolicyRule rule,
      List<ValidationFinding> filteredExistingFindings) {
    if (rule == null || filteredExistingFindings == null || filteredExistingFindings.isEmpty()) {
      return false;
    }
    String ruleTarget = normalizeKey(rule.getTarget());
    for (ValidationFinding f : filteredExistingFindings) {
      if (ruleTarget.equals(normalizeKey(f.getSource()))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns a mutable copy of {@code filteredExistingFindings} to use as the initial value for
   * {@code accumulatedFindings} in the policy rule loop. Returns an empty list when null.
   */
  public static List<ValidationFinding> initAccumulatedFromExisting(
      List<ValidationFinding> filteredExistingFindings) {
    if (filteredExistingFindings == null || filteredExistingFindings.isEmpty()) {
      return new ArrayList<>();
    }
    return new ArrayList<>(filteredExistingFindings);
  }

  /**
   * Adds all findings from {@code ruleResult} into the {@code accumulated} list,
   * skipping any finding that is already represented:
   * <ul>
   *   <li>by {@code documentTypeKey} (case-insensitive) when non-null, or</li>
   *   <li>by {@code source} (case-insensitive) when {@code documentTypeKey} is null.</li>
   * </ul>
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

  /**
   * Wraps a list of findings into a new {@link PolicyValidationResult},
   * deduplicating by message content (preserves first occurrence).
   */
  public static PolicyValidationResult wrapFindings(List<ValidationFinding> findings) {
    PolicyValidationResult result = new PolicyValidationResult();
    findings = findings != null ? findings : new ArrayList<>();
    List<ValidationFinding> distinct = new ArrayList<>();
    java.util.Set<String> seenMessages = new java.util.LinkedHashSet<>();
    for (ValidationFinding f : findings) {
      if (seenMessages.add(f.getMessage())) {
        f.setUserExplanation(null);
        distinct.add(f);
      }
    }
    result.setFindings(distinct);
    return result;
  }

  /**
   * Marks the policy step FAILED, attaches it to the result, and returns a
   * plain-text error summary.
   */
  public static String failPolicyStep(AgentProcessingStep step,
      PolicyValidationResult result, Throwable error) {
    step.setName("Policy Validation");
    step.setStatus(StepStatus.FAILED);
    step.setCompletedAt(Instant.now());
    if (step.getStartedAt() != null) {
      step.setDurationMs(step.getCompletedAt().toEpochMilli() - step.getStartedAt().toEpochMilli());
    }
    String msg = error != null ? error.getMessage() : "Unknown policy validation error";
    LOG.log(Level.SEVERE, "Policy validation failed: " + msg, error);
    step.getLogLines().add(new AgentProcessingStep.LogLine(LogLineSeverity.ERROR,
        "Policy validation failed: " + msg));
    ValidationFinding errorFinding = new ValidationFinding(
        FindingSeverity.FAILURE, "Policy validation could not complete: " + msg, "system", RiskType.POLICY_COMPLIANCE);
    errorFinding.setRiskKind(RiskKind.AI_VALIDATION);
    result.getFindings().add(errorFinding);
    result.setProcessingStep(step);
    return "Policy validation failed: " + msg;
  }
}
