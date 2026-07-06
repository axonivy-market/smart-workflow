package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.StringJoiner;

import com.axonivy.utils.smart.workflow.demo.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;

public class DocumentContextBuilder {

  private static final int DEFAULT_CONTENT_LIMIT = 3_000;
  private static final double MAX_NON_PRINTABLE_RATIO = 0.05;

  private static final String NO_DOCUMENT_MSG           = "No document provided.";
  private static final String FILE_FORMAT               = "File: %s";
  private static final String TYPE_FORMAT               = "Type: %s";
  private static final String CERTIFICATION_TYPE_FORMAT = "CertificationType: %s";
  private static final String DESCRIPTION_FORMAT        = "Description: %s";
  private static final String CONTENT_FORMAT            = "Content: %s";
  private static final String N_A                       = "n/a";
  private static final String TRUNCATION_SUFFIX         = "...[truncated]";

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
      return NO_DOCUMENT_MSG;
    }
    StringJoiner fields = new StringJoiner(", ");
    fields.add(String.format(FILE_FORMAT, doc.getFileName()));
    fields.add(String.format(TYPE_FORMAT, doc.getDocumentType()));

    String docDescription = doc.getDescription() != null ? doc.getDescription() : N_A;
    fields.add(String.format(getDescriptionFormat(), docDescription));

    readableContent().ifPresent(content -> fields.add(String.format(CONTENT_FORMAT, content)));
    return fields.toString();
  }

  private String getDescriptionFormat() {
    if (showCertificationSubtype && LegalDocumentType.CERTIFICATION.equals(doc.getDocumentType())) {
      return CERTIFICATION_TYPE_FORMAT;
    }
    return DESCRIPTION_FORMAT;
  }

  private Optional<String> readableContent() {
    if (doc.getFileContent() == null || doc.getFileContent().length == 0) {
      return Optional.empty();
    }
    String text = new String(doc.getFileContent(), StandardCharsets.UTF_8);
    long nonPrintable = text.chars()
        .filter(charCode -> (char) charCode < 32 && charCode != '\n' && charCode != '\r' && charCode != '\t')
        .count();
    if (nonPrintable > text.length() * MAX_NON_PRINTABLE_RATIO) {
      return Optional.empty();
    }
    int limit = Math.min(text.length(), contentLimit);
    return Optional.of(text.length() > limit ? text.substring(0, limit) + TRUNCATION_SUFFIX : text);
  }
}
