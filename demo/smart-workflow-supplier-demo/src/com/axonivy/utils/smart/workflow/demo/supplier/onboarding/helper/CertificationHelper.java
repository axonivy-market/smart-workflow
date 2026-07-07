package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.axonivy.utils.smart.workflow.demo.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierCertification;

public class CertificationHelper implements Serializable {

  private static final long serialVersionUID = 1L;

  private Map<LegalDocumentType, Boolean> certChecked;
  private Map<LegalDocumentType, SupplierCertification> certDetails;

  public void init(List<SupplierCertification> existing) {
    certChecked = new EnumMap<>(LegalDocumentType.class);
    certDetails = new EnumMap<>(LegalDocumentType.class);

    for (LegalDocumentType type : LegalDocumentType.certificationValues()) {
      certChecked.put(type, Boolean.FALSE);
      SupplierCertification cert = new SupplierCertification();
      cert.setType(type);
      cert.setUploaded(Boolean.FALSE);
      certDetails.put(type, cert);
    }

    if (existing != null) {
      for (SupplierCertification cert : existing) {
        if (cert.getType() != null) {
          certChecked.put(cert.getType(), Boolean.TRUE.equals(cert.getUploaded()));
          certDetails.put(cert.getType(), cert);
        }
      }
    }
  }

  public List<SupplierCertification> buildCertificationList(
      Function<LegalDocumentType, LegalDocument> docFinder) {
    List<SupplierCertification> result = new ArrayList<>();
    for (LegalDocumentType type : LegalDocumentType.certificationValues()) {
      boolean hasDoc = docFinder.apply(type) != null;
      boolean isChecked = Boolean.TRUE.equals(certChecked.get(type));
      SupplierCertification cert = certDetails.get(type);
      if (cert == null) {
        cert = new SupplierCertification();
        cert.setType(type);
      }
      cert.setUploaded(hasDoc || isChecked);
      result.add(cert);
    }
    return result;
  }

  public Map<LegalDocumentType, Boolean> getCertChecked() {
    return certChecked;
  }

  public Map<LegalDocumentType, SupplierCertification> getCertDetails() {
    return certDetails;
  }
}