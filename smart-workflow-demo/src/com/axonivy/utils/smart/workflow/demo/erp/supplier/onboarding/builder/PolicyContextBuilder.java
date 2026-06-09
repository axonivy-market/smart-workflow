package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.builder;

import java.time.LocalDate;
import java.util.List;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.DocumentExtractionResult;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.SupplierPolicyRule;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingRequest;

public class PolicyContextBuilder {

  private static final int CONTENT_TRUNCATION_LIMIT = 3_000;

  private PolicyContextBuilder() {
  }

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
        if (content.length() > CONTENT_TRUNCATION_LIMIT) {
          content = content.substring(0, CONTENT_TRUNCATION_LIMIT) + "\n... [truncated]";
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

  public static boolean hasRuleDocument(SupplierPolicyRule rule,
      DocumentExtractionResult extractionResult) {
    if (rule.getCertificationType() == null && rule.getLegalDocumentType() == null) {
      return true;
    }
    return findMatchingDoc(rule, extractionResult) != null;
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
}
