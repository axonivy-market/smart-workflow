package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.axonivy.utils.smart.workflow.demo.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierCertification;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestCertificationHelper {

  @SuppressWarnings("unused")
  static Stream<LegalDocumentType> certificationTypes() {
    return Arrays.stream(LegalDocumentType.certificationValues());
  }

  @ParameterizedTest
  @MethodSource("certificationTypes")
  void init_whenNoExistingCertification_initializeDefaultCertificationState(LegalDocumentType type) {
    CertificationHelper manager = new CertificationHelper();

    manager.init(null);

    assertFalse(manager.getCertChecked().get(type));

    SupplierCertification cert = manager.getCertDetails().get(type);
    assertNotNull(cert);
    assertEquals(type, cert.getType());
    assertFalse(Boolean.TRUE.equals(cert.getUploaded()));
  }

  @ParameterizedTest
  @MethodSource("certificationTypes")
  void init_whenExistingCertificationExists_overrideDefaultCertificationState(LegalDocumentType type) {
    CertificationHelper manager = new CertificationHelper();

    SupplierCertification existing = new SupplierCertification();
    existing.setType(type);
    existing.setUploaded(Boolean.TRUE);

    manager.init(List.of(existing));

    assertTrue(manager.getCertChecked().get(type));
    assertSame(existing, manager.getCertDetails().get(type));
  }

  @ParameterizedTest
  @MethodSource("certificationTypes")
  void buildCertificationList_whenDocumentExists_setUploadedToTrue(LegalDocumentType type) {
    CertificationHelper manager = new CertificationHelper();
    manager.init(null);

    List<SupplierCertification> certifications = manager.buildCertificationList(t -> {
      if (t == type) {
        return new LegalDocument();
      }
      return null;
    });

    SupplierCertification cert = certifications.stream()
        .filter(c -> c.getType() == type)
        .findFirst()
        .orElseThrow();

    assertTrue(Boolean.TRUE.equals(cert.getUploaded()));
  }

  @ParameterizedTest
  @MethodSource("certificationTypes")
  void buildCertificationList_whenCertificationChecked_setUploadedToTrue(LegalDocumentType type) {
    CertificationHelper manager = new CertificationHelper();
    manager.init(null);

    manager.getCertChecked().put(type, Boolean.TRUE);

    List<SupplierCertification> certifications =
        manager.buildCertificationList(t -> null);

    SupplierCertification cert = certifications.stream()
        .filter(c -> c.getType() == type)
        .findFirst()
        .orElseThrow();

    assertTrue(Boolean.TRUE.equals(cert.getUploaded()));
  }

  @ParameterizedTest
  @MethodSource("certificationTypes")
  void buildCertificationList_whenNoDocumentAndNotChecked_setUploadedToFalse(LegalDocumentType type) {
    CertificationHelper manager = new CertificationHelper();
    manager.init(null);

    List<SupplierCertification> certifications =
        manager.buildCertificationList(t -> null);

    SupplierCertification cert = certifications.stream()
        .filter(c -> c.getType() == type)
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
