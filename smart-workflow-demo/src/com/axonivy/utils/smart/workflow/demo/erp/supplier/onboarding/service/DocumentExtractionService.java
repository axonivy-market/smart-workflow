package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentRepository;
import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.DocumentExtractionResult;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.builder.LogLineBuilder;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.AgentStepStatus;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.LogLineSeverity;

/**
 * Helpers for the {@code supplierDocumentExtractAgent} callable sub-process.
 *
 * <p>All methods are stateless and static so that IvyScript in process Script
 * nodes can call them with a single import line.</p>
 */
public class DocumentExtractionService {

  private static final Logger LOG = Logger.getLogger(DocumentExtractionService.class.getName());

  private DocumentExtractionService() {
  }

  // ── Document loading ─────────────────────────────────────────────────────

  /**
   * Loads all legal documents for the given supplier from the repository.
   * Returns an empty list when none are found (never null).
   */
  public static List<LegalDocument> loadDocuments(String supplierId) {
    List<LegalDocument> docs = LegalDocumentRepository.getInstance().findByObjectId(supplierId);
    return docs != null ? docs : new ArrayList<>();
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

  // ── Context building ─────────────────────────────────────────────────────

  /**
   * Builds a single-document context string for one {@link LegalDocument}.
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
   * Loads all legal documents for the given supplier and returns a formatted
   * document context string for the AI extraction query.
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

  // ── Result merging ───────────────────────────────────────────────────────

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

  // ── Step lifecycle ───────────────────────────────────────────────────────

  /**
   * Creates and returns a new {@link AgentProcessingStep} for document
   * extraction, already in RUNNING state.
   */
  public static AgentProcessingStep startExtractionStep() {
    AgentProcessingStep step = new AgentProcessingStep();
    step.setName(ValidationUtils.stepName("StepDocumentExtraction"));
    step.setStatus(AgentStepStatus.RUNNING);
    step.setStartedAt(Instant.now());
    return step;
  }

  /**
   * Marks the extraction step COMPLETED, attaches log lines for each
   * extracted document, and links it to the result.
   */
  public static void finalizeExtractionStep(AgentProcessingStep step,
      DocumentExtractionResult result) {
    step.setStatus(AgentStepStatus.COMPLETED);
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

  /**
   * Marks the extraction step FAILED, attaches it to the result, logs the
   * error, and returns a plain-text error summary.
   */
  public static String failExtractionStep(AgentProcessingStep step,
      DocumentExtractionResult result, Throwable error) {
    step.setName(ValidationUtils.stepName("StepDocumentExtraction"));
    step.setStatus(AgentStepStatus.FAILED);
    step.setCompletedAt(Instant.now());
    if (step.getStartedAt() != null) {
      step.setDurationMs(step.getCompletedAt().toEpochMilli() - step.getStartedAt().toEpochMilli());
    }
    if (step.getLogLines() == null) {
      step.setLogLines(new ArrayList<>());
    }
    String msg = error != null ? error.getMessage() : "Unknown extraction error";
    LOG.log(Level.SEVERE, "Document extraction failed: " + msg, error);
    step.getLogLines().add(LogLineBuilder.of(LogLineSeverity.ERROR,
        "Extraction failed: " + msg));
    result.setProcessingStep(step);
    return "Document extraction failed: " + msg;
  }

  private static void buildLogForDocumentType(AgentProcessingStep step,
      DocumentExtractionResult.ExtractedDoc doc) {
    String header = (doc.getFileName() != null && !doc.getFileName().isEmpty())
        ? doc.getFileName() + " [" + (doc.getDocumentType() != null ? doc.getDocumentType().getLabel() : LegalDocumentType.OTHER.getLabel()) + "]"
        : (doc.getDocumentType() != null ? doc.getDocumentType().getLabel() : LegalDocumentType.OTHER.getLabel());

    if (step.getLogLines() == null) {
      step.setLogLines(new ArrayList<>());
    }
    step.getLogLines().add(LogLineBuilder.of(LogLineSeverity.OK, header));

    String detail = (doc.getExtractedFieldsSummary() != null && !doc.getExtractedFieldsSummary().isEmpty())
        ? doc.getExtractedFieldsSummary()
        : (doc.getNote() != null && !doc.getNote().isEmpty() ? doc.getNote() : null);
    if (detail != null) {
      step.getLogLines().add(LogLineBuilder.of(LogLineSeverity.OK, detail, true));
    }
  }
}
