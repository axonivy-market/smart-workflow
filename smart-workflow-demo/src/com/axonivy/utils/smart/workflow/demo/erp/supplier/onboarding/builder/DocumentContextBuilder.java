package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.builder;

import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentType;

public class DocumentContextBuilder {

  private static final int DEFAULT_CONTENT_LIMIT = 3_000; // keep in sync with ValidationUtils.CONTENT_TRUNCATION_LIMIT
  private static final double MAX_NON_PRINTABLE_RATIO = 0.05;

  private final LegalDocument doc;
  private boolean showCertificationSubtype = false;
  private int contentLimit = DEFAULT_CONTENT_LIMIT;

  private DocumentContextBuilder(LegalDocument doc) {
    this.doc = doc;
  }

  public static DocumentContextBuilder of(LegalDocument doc) {
    return new DocumentContextBuilder(doc);
  }

  public DocumentContextBuilder withCertificationType() {
    this.showCertificationSubtype = true;
    return this;
  }

  public DocumentContextBuilder withContentLimit(int limit) {
    this.contentLimit = limit;
    return this;
  }

  public String build() {
    if (doc == null) {
      return "No document provided.";
    }
    StringJoiner fields = new StringJoiner(", ");
    fields.add("File: " + doc.getFileName());
    fields.add("Type: " + doc.getDocumentType());

    String docDescription = doc.getDescription() != null ? doc.getDescription() : "n/a";
    String docDescriptionPrefix = showCertificationSubtype && LegalDocumentType.CERTIFICATION.equals(doc.getDocumentType())
        ? "CertificationType: " : "Description: ";

    fields.add(docDescriptionPrefix + docDescription);
    String content = readableContent();
    if (content != null) {
      fields.add("Content: " + content);
    }
    return fields.toString();
  }

  // Returns the document text for AI context, or null if the content is binary (PDF, image, etc.)
  private String readableContent() {
    if (doc.getFileContent() == null || doc.getFileContent().length == 0) {
      return null;
    }
    String text = new String(doc.getFileContent(), StandardCharsets.UTF_8);
    long nonPrintable = text.chars()
        .filter(c -> (char) c < 32 && c != '\n' && c != '\r' && c != '\t').count();
    if (nonPrintable > text.length() * MAX_NON_PRINTABLE_RATIO) {
      return null;
    }
    int limit = Math.min(text.length(), contentLimit);
    return text.length() > limit ? text.substring(0, limit) + "...[truncated]" : text;
  }
}
