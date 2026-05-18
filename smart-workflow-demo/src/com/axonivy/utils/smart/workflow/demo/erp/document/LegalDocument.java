package com.axonivy.utils.smart.workflow.demo.erp.document;

import com.axonivy.utils.smart.workflow.utils.IdGenerationUtils;

public class LegalDocument {

  private String documentId;
  private String objectId;
  private LegalDocumentObjectType objectType;
  private LegalDocumentType documentType;
  private String fileName;
  private String contentType;
  private byte[] fileContent;
  private long fileSize;
  private String uploadedAt;
  private String uploadedBy;
  private String description;

  public LegalDocument() {
    this.documentId = IdGenerationUtils.generateRandomId();
  }

  public String getDocumentId() {
    return documentId;
  }

  public void setDocumentId(String documentId) {
    this.documentId = documentId;
  }

  public String getObjectId() {
    return objectId;
  }

  public void setObjectId(String objectId) {
    this.objectId = objectId;
  }

  public LegalDocumentObjectType getObjectType() {
    return objectType;
  }

  public void setObjectType(LegalDocumentObjectType objectType) {
    this.objectType = objectType;
  }

  public LegalDocumentType getDocumentType() {
    return documentType;
  }

  public void setDocumentType(LegalDocumentType documentType) {
    this.documentType = documentType;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public byte[] getFileContent() {
    return fileContent;
  }

  public void setFileContent(byte[] fileContent) {
    this.fileContent = fileContent;
  }

  public long getFileSize() {
    return fileSize;
  }

  public void setFileSize(long fileSize) {
    this.fileSize = fileSize;
  }

  public String getUploadedAt() {
    return uploadedAt;
  }

  public void setUploadedAt(String uploadedAt) {
    this.uploadedAt = uploadedAt;
  }

  public String getUploadedBy() {
    return uploadedBy;
  }

  public void setUploadedBy(String uploadedBy) {
    this.uploadedBy = uploadedBy;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private final LegalDocument doc = new LegalDocument();

    public Builder objectId(String v) { doc.setObjectId(v); return this; }
    public Builder objectType(LegalDocumentObjectType v) { doc.setObjectType(v); return this; }
    public Builder documentType(LegalDocumentType v) { doc.setDocumentType(v); return this; }
    public Builder fileName(String v) { doc.setFileName(v); return this; }
    public Builder contentType(String v) { doc.setContentType(v); return this; }
    public Builder fileContent(byte[] v) { doc.setFileContent(v); return this; }
    public Builder fileSize(long v) { doc.setFileSize(v); return this; }
    public Builder description(String v) { doc.setDescription(v); return this; }
    public Builder uploadedNow() {
      doc.setUploadedAt(java.time.LocalDateTime.now()
          .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
      return this;
    }
    public LegalDocument build() { return doc; }
  }
}
