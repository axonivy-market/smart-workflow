package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.service;

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
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.builder.DocumentContextBuilder;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.builder.LogLineBuilder;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.AgentStepStatus;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.LogLineSeverity;

public class DocumentExtractionService {

  private static final Logger LOG = Logger.getLogger(DocumentExtractionService.class.getName());

  private DocumentExtractionService() {
  }

  public static List<LegalDocument> loadDocuments(String supplierId) {
    List<LegalDocument> docs = LegalDocumentRepository.getInstance().findByObjectId(supplierId);
    return docs != null ? docs : new ArrayList<>();
  }

  public static List<String> loadRequiredDocumentTypes() {
    List<String> required = new ArrayList<>();
    for (LegalDocumentType docType : LegalDocumentType.values()) {
      if (docType.isRequired()) {
        required.add(docType.getLabel());
      }
    }
    return required;
  }

  public static String buildSingleDocumentContext(LegalDocument doc) {
    return DocumentContextBuilder.of(doc).withCertificationType().build();
  }

  public static String buildDocumentContext(String supplierId) {
    List<LegalDocument> docs = LegalDocumentRepository.getInstance().findByObjectId(supplierId);
    if (docs == null || docs.isEmpty()) {
      return "No documents found for supplier: " + supplierId;
    }
    StringBuilder sb = new StringBuilder();
    for (LegalDocument doc : docs) {
      sb.append(DocumentContextBuilder.of(doc).build()).append("\n");
    }
    return sb.toString();
  }

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

  public static AgentProcessingStep startExtractionStep() {
    AgentProcessingStep step = new AgentProcessingStep();
    step.setName(ValidationUtils.stepName("StepDocumentExtraction"));
    step.setStatus(AgentStepStatus.RUNNING);
    step.setStartedAt(Instant.now());
    return step;
  }

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
