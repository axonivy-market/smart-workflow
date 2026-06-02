package com.axonivy.utils.smart.workflow.demo.erp.supplier.agent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep;
import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.langchain4j.model.output.structured.Description;

public class DocumentExtractionResult implements Serializable {

  private static final long serialVersionUID = 1L;

  @Description("List of extracted documents produced by the AI extraction step")
  private List<ExtractedDoc> documentSummaries;

  @JsonIgnore
  private AgentProcessingStep processingStep;

  public DocumentExtractionResult() {
    this.documentSummaries = new ArrayList<>();
  }

  public List<ExtractedDoc> getDocumentSummaries() {
    return documentSummaries;
  }

  public void setDocumentSummaries(List<ExtractedDoc> documentSummaries) {
    this.documentSummaries = documentSummaries;
  }

  public AgentProcessingStep getProcessingStep() {
    return processingStep;
  }

  public void setProcessingStep(AgentProcessingStep processingStep) {
    this.processingStep = processingStep;
  }

  // ---------------------------------------------------------------------------
  // Inner class
  // ---------------------------------------------------------------------------

  public static class ExtractedDoc implements Serializable {

    private static final long serialVersionUID = 1L;

    @Description("Exact filename of the document, e.g. 'iso9001_cert.pdf'")
    private String fileName;

    @Description("Document type matching LegalDocumentType enum. Use specific certification sub-types directly: "
        + "'ISO_9001', 'ISO_14001', 'ISO_27001', 'GDPR_DPA'. "
        + "Other values: 'TAX_CERTIFICATE', 'COMMERCIAL_REGISTER', 'BANKING_CONFIRMATION', 'CONTRACT', "
        + "'SELF_DECLARATION', 'ANNUAL_REPORT', 'CERTIFICATION' (generic cert), 'OTHER'.")
    private LegalDocumentType documentType;

    @Description("Comma-separated summary of key extracted fields, e.g. 'cert no: DE-2023-00412, valid until: 2026-03-15'")
    private String extractedFieldsSummary;

    @Description("Short human-readable log note for the UI, e.g. 'iso9001_cert.pdf — extracted: cert no. DE-2023-00412, valid until 2026-03-15'")
    private String note;

    @Description("Full extracted text content of the document. For demo documents without real file bytes, generate realistic synthetic "
        + "full-text content matching the document type (e.g. full certificate text with header, body, signature block).")
    private String content;

    public ExtractedDoc() {
    }

    public String getFileName() {
      return fileName;
    }

    public void setFileName(String fileName) {
      this.fileName = fileName;
    }

    public LegalDocumentType getDocumentType() {
      return documentType;
    }

    public void setDocumentType(LegalDocumentType documentType) {
      this.documentType = documentType;
    }

    public String getExtractedFieldsSummary() {
      return extractedFieldsSummary;
    }

    public void setExtractedFieldsSummary(String extractedFieldsSummary) {
      this.extractedFieldsSummary = extractedFieldsSummary;
    }

    public String getNote() {
      return note;
    }

    public void setNote(String note) {
      this.note = note;
    }

    public String getContent() {
      return content;
    }

    public void setContent(String content) {
      this.content = content;
    }
  }
}
