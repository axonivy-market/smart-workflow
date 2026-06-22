package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierCertification;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestCertificationHelper {

  @Test
  void init_whenNoExistingCertification_initializeDefaultCertificationState() {
    CertificationHelper manager = new CertificationHelper();

    manager.init(null);

    for (LegalDocumentType type : LegalDocumentType.certificationValues()) {
      assertFalse(manager.getCertChecked().get(type));

      SupplierCertification cert = manager.getCertDetails().get(type);
      assertNotNull(cert);
      assertEquals(type, cert.getType());
      assertFalse(Boolean.TRUE.equals(cert.getUploaded()));
    }
  }

  @Test
  void init_whenExistingCertificationExists_overrideDefaultCertificationState() {
    CertificationHelper manager = new CertificationHelper();

    LegalDocumentType type = LegalDocumentType.certificationValues()[0];

    SupplierCertification existing = new SupplierCertification();
    existing.setType(type);
    existing.setUploaded(Boolean.TRUE);

    manager.init(List.of(existing));

    assertTrue(manager.getCertChecked().get(type));
    assertSame(existing, manager.getCertDetails().get(type));
  }

  @Test
  void buildCertificationList_whenDocumentExists_setUploadedToTrue() {
    CertificationHelper manager = new CertificationHelper();
    manager.init(null);

    LegalDocumentType targetType = LegalDocumentType.certificationValues()[0];

    List<SupplierCertification> certifications = manager.buildCertificationList(type -> {
      if (type == targetType) {
        return new LegalDocument();
      }
      return null;
    });

    SupplierCertification cert = certifications.stream()
        .filter(c -> c.getType() == targetType)
        .findFirst()
        .orElseThrow();

    assertTrue(Boolean.TRUE.equals(cert.getUploaded()));
  }

  @Test
  void buildCertificationList_whenCertificationChecked_setUploadedToTrue() {
    CertificationHelper manager = new CertificationHelper();
    manager.init(null);

    LegalDocumentType targetType = LegalDocumentType.certificationValues()[0];
    manager.getCertChecked().put(targetType, Boolean.TRUE);

    List<SupplierCertification> certifications =
        manager.buildCertificationList(type -> null);

    SupplierCertification cert = certifications.stream()
        .filter(c -> c.getType() == targetType)
        .findFirst()
        .orElseThrow();

    assertTrue(Boolean.TRUE.equals(cert.getUploaded()));
  }

  @Test
  void buildCertificationList_whenNoDocumentAndNotChecked_setUploadedToFalse() {
    CertificationHelper manager = new CertificationHelper();
    manager.init(null);

    LegalDocumentType targetType = LegalDocumentType.certificationValues()[0];

    List<SupplierCertification> certifications =
        manager.buildCertificationList(type -> null);

    SupplierCertification cert = certifications.stream()
        .filter(c -> c.getType() == targetType)
        .findFirst()
        .orElseThrow();

    assertFalse(Boolean.TRUE.equals(cert.getUploaded()));
  }

  @Test
  void buildCertificationList_afterInitialization_returnAllCertificationTypes() {
    CertificationHelper manager = new CertificationHelper();
    manager.init(null);

    List<SupplierCertification> certifications =
        manager.buildCertificationList(type -> null);

    assertEquals(
        LegalDocumentType.certificationValues().length,
        certifications.size());
  }
}