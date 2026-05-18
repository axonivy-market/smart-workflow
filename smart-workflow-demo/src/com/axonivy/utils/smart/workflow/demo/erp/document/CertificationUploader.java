package com.axonivy.utils.smart.workflow.demo.erp.document;

/**
 * Contract for querying certification documents keyed by a specific {@link LegalDocumentType}
 * certification sub-type (ISO_9001, ISO_14001, ISO_27001, GDPR_DPA).
 *
 * <p>The default implementation filters {@link #getSupplierDocuments()} so
 * implementors do not need to repeat the lookup logic.
 */
public interface CertificationUploader extends DocumentUploader {

  default LegalDocument getDocumentForCert(LegalDocumentType certType) {
    return getSupplierDocuments().stream()
        .filter(d -> certType.equals(d.getDocumentType()))
        .findFirst().orElse(null);
  }
}
