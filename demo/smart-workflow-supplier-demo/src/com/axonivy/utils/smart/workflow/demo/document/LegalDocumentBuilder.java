package com.axonivy.utils.smart.workflow.demo.document;

import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentObjectType;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;

public final class LegalDocumentBuilder {

  private final LegalDocument doc = new LegalDocument();

  public static LegalDocumentBuilder builder() {
    return new LegalDocumentBuilder();
  }

  public static LegalDocument of(LegalDocumentType type) {
    return builder().documentType(type).build();
  }

  public static LegalDocument of(LegalDocumentType type, String fileName) {
    return builder().documentType(type).fileName(fileName).build();
  }

  public static LegalDocument of(LegalDocumentType type, String fileName, String description) {
    return builder().documentType(type).fileName(fileName).description(description).build();
  }

  public static LegalDocument of(LegalDocumentType type, String fileName, byte[] content) {
    return builder().documentType(type).fileName(fileName).fileContent(content).build();
  }

  public static LegalDocument ofCert(String description) {
    return of(LegalDocumentType.CERTIFICATION, null, description);
  }

  public LegalDocumentBuilder objectId(String v) { doc.setObjectId(v); return this; }
  public LegalDocumentBuilder objectType(LegalDocumentObjectType v) { doc.setObjectType(v); return this; }
  public LegalDocumentBuilder documentType(LegalDocumentType v) { doc.setDocumentType(v); return this; }
  public LegalDocumentBuilder fileName(String v) { doc.setFileName(v); return this; }
  public LegalDocumentBuilder fileContent(byte[] v) { doc.setFileContent(v); return this; }
  public LegalDocumentBuilder description(String v) { doc.setDescription(v); return this; }
  public LegalDocumentBuilder uploadedNow() {
    doc.setUploadedAt(java.time.LocalDateTime.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
    return this;
  }
  public LegalDocument build() { return doc; }
}
