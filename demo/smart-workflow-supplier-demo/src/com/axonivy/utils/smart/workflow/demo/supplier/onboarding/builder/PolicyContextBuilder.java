package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.axonivy.utils.smart.workflow.demo.supplier.agent.DocumentExtractionResult;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.ExtractedDoc;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierPolicyRule;

public class PolicyContextBuilder {

  private static final int CONTENT_TRUNCATION_LIMIT = 3_000;

  private static final String PARA_BREAK              = "\n\n";
  private static final String DOC_ENTRY_HEADER_FORMAT   = "Document Type: %s\nFile Name: %s\nContent:\n";
  private static final String NO_CONTENT_TEXT           = "(no content extracted)";
  private static final String NO_DOCUMENTS_TEXT         = "No documents were extracted.";
  private static final String TODAY_DATE_FORMAT         = "\nTODAY'S DATE: %s";
  private static final String SUPPLIER_CONTEXT_FORMAT   = "SUPPLIER CONTEXT:\n%s\n\n";
  private static final String DOC_UNDER_EVALUATION      = "DOCUMENT UNDER EVALUATION:\n";
  private static final String FILE_TYPE_FORMAT          = "File: %s\nType: %s\n";
  private static final String EXTRACTED_FIELDS_FORMAT   = "Extracted Fields: %s\n";
  private static final String TRUNCATION_SUFFIX         = "\n... [truncated]";
  private static final String CONTENT_FORMAT            = "Content:\n%s\n";
  private static final String NO_DOC_FOUND_TEXT         = "DOCUMENT: No specific document found for this rule.\n";
  private static final String EVALUATE_WITHOUT_DOC_TEXT = "Evaluate based on supplier context and available signals only.\n";
  private static final String SINGLE_RULE_PROMPT_FORMAT = """
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
      - score: points deducted from the compliance score - 0 for PASSED, %d for WARNING, %d for FAILURE

      Focus ONLY on this specific rule. Do not evaluate or invent findings for other rules.""";

  private PolicyContextBuilder() {
  }

  public static String buildPolicyDocumentContext(DocumentExtractionResult extractionResult) {
    StringBuilder sb = new StringBuilder();

    if (extractionResult != null && extractionResult.getDocumentSummaries() != null
        && !extractionResult.getDocumentSummaries().isEmpty()) {
      for (ExtractedDoc doc : extractionResult.getDocumentSummaries()) {
        sb.append(String.format(DOC_ENTRY_HEADER_FORMAT, doc.getDocumentType(), doc.getFileName()));
        if (doc.getContent() != null && !doc.getContent().isEmpty()) {
          sb.append(doc.getContent());
        } else {
          sb.append(NO_CONTENT_TEXT);
        }
        sb.append(PARA_BREAK);
      }
    } else {
      sb.append(NO_DOCUMENTS_TEXT);
    }

    sb.append(String.format(TODAY_DATE_FORMAT, LocalDate.now()));
    return sb.toString();
  }

  public static String buildRuleDocContext(SupplierPolicyRule rule,
      DocumentExtractionResult extractionResult, String supplierContext) {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format(SUPPLIER_CONTEXT_FORMAT, supplierContext != null ? supplierContext : "N/A"));

    Optional<ExtractedDoc> matchingDoc = findMatchingDoc(rule, extractionResult);

    if (matchingDoc.isPresent()) {
      ExtractedDoc doc = matchingDoc.get();
      sb.append(DOC_UNDER_EVALUATION);
      sb.append(String.format(FILE_TYPE_FORMAT, doc.getFileName(), doc.getDocumentType()));
      if (doc.getExtractedFieldsSummary() != null && !doc.getExtractedFieldsSummary().isBlank()) {
        sb.append(String.format(EXTRACTED_FIELDS_FORMAT, doc.getExtractedFieldsSummary()));
      }
      if (doc.getContent() != null && !doc.getContent().isBlank()) {
        String content = doc.getContent();
        if (content.length() > CONTENT_TRUNCATION_LIMIT) {
          content = content.substring(0, CONTENT_TRUNCATION_LIMIT) + TRUNCATION_SUFFIX;
        }
        sb.append(String.format(CONTENT_FORMAT, content));
      }
    } else {
      sb.append(NO_DOC_FOUND_TEXT);
      sb.append(EVALUATE_WITHOUT_DOC_TEXT);
    }

    sb.append(String.format(TODAY_DATE_FORMAT, LocalDate.now()));
    return sb.toString();
  }

  public static String buildSingleRuleSystemPrompt(SupplierPolicyRule rule) {
    int fullDeduction = rule.getRiskScore();
    int halfDeduction = Math.round(fullDeduction / 2.0f);
    return String.format(SINGLE_RULE_PROMPT_FORMAT,
        rule.getTarget(), rule.getRule(), fullDeduction, rule.getTarget(), halfDeduction, fullDeduction);
  }

  public static boolean hasRuleDocument(SupplierPolicyRule rule,
      DocumentExtractionResult extractionResult) {
    if (rule.getCertificationType() == null && rule.getLegalDocumentType() == null) {
      return true;
    }
    return findMatchingDoc(rule, extractionResult).isPresent();
  }

  private static Optional<ExtractedDoc> findMatchingDoc(
      SupplierPolicyRule rule, DocumentExtractionResult extractionResult) {
    if (extractionResult == null || extractionResult.getDocumentSummaries() == null
        || extractionResult.getDocumentSummaries().isEmpty()) {
      return Optional.empty();
    }
    List<ExtractedDoc> docs = extractionResult.getDocumentSummaries();

    if (rule.getCertificationType() != null) {
      return docs.stream()
          .filter(doc -> rule.getCertificationType().equals(doc.getDocumentType()))
          .findFirst();
    }
    if (rule.getLegalDocumentType() != null) {
      return docs.stream()
          .filter(doc -> doc.getDocumentType() == rule.getLegalDocumentType())
          .findFirst();
    }
    return Optional.empty();
  }
}
