package com.axonivy.utils.smart.workflow.demo.document;

import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;

public interface CertificationUploader extends DocumentUploader {

  default LegalDocument getDocumentForCert(LegalDocumentType certType) {
    return getSupplierDocuments().stream()
        .filter(d -> certType.equals(d.getDocumentType()))
        .findFirst().orElse(null);
  }
}
