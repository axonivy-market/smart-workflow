package com.axonivy.utils.smart.workflow.demo.erp.supplier.model;

import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentType;
import dev.langchain4j.model.output.structured.Description;

public class SupplierCertification {

  @Description("Type of certification")
  private LegalDocumentType type;

  @Description("Certificate number")
  private String certNumber;

  @Description("Expiry date in ISO format yyyy-MM-dd")
  private String expiryDate;

  @Description("Reference to uploaded document filename")
  private String documentReference;

  @Description("Whether the certificate document has been uploaded")
  private boolean uploaded;

  public SupplierCertification() {
  }

  public SupplierCertification(LegalDocumentType type, String certNumber, String expiryDate,
      String documentReference, boolean uploaded) {
    this.type = type;
    this.certNumber = certNumber;
    this.expiryDate = expiryDate;
    this.documentReference = documentReference;
    this.uploaded = uploaded;
  }

  public LegalDocumentType getType() {
    return type;
  }

  public void setType(LegalDocumentType type) {
    this.type = type;
  }

  public String getCertNumber() {
    return certNumber;
  }

  public void setCertNumber(String certNumber) {
    this.certNumber = certNumber;
  }

  public String getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(String expiryDate) {
    this.expiryDate = expiryDate;
  }

  public String getDocumentReference() {
    return documentReference;
  }

  public void setDocumentReference(String documentReference) {
    this.documentReference = documentReference;
  }

  public boolean isUploaded() {
    return uploaded;
  }

  public void setUploaded(boolean uploaded) {
    this.uploaded = uploaded;
  }
}
