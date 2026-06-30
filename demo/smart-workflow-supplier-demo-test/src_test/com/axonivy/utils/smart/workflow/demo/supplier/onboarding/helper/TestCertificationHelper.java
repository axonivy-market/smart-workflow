package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.axonivy.utils.smart.workflow.demo.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierCertification;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestCertificationHelper {

  @ParameterizedTest(name = "{0}")
  @CsvSource({
      "init_ISO9001NoExisting_defaultState, ISO_9001",
      "init_ISO14001NoExisting_defaultState, ISO_14001",
      "init_ISO27001NoExisting_defaultState, ISO_27001",
      "init_GdprDpaNoExisting_defaultState, GDPR_DPA"
  })
  void init_whenNoExistingCertification_initializeDefaultCertificationState(
      String testName, LegalDocumentType type) {
    CertificationHelper manager = new CertificationHelper();

    manager.init(null);

    assertFalse(manager.getCertChecked().get(type));

    SupplierCertification cert = manager.getCertDetails().get(type);
    assertNotNull(cert);
    assertEquals(type, cert.getType());
    assertFalse(Boolean.TRUE.equals(cert.getUploaded()));
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
      "init_ISO9001Existing_overridden, ISO_9001",
      "init_ISO14001Existing_overridden, ISO_14001",
      "init_ISO27001Existing_overridden, ISO_27001",
      "init_GdprDpaExisting_overridden, GDPR_DPA"
  })
  void init_whenExistingCertificationExists_overrideDefaultCertificationState(
      String testName, LegalDocumentType type) {
    CertificationHelper manager = new CertificationHelper();

    SupplierCertification existing = new SupplierCertification();
    existing.setType(type);
    existing.setUploaded(Boolean.TRUE);

    manager.init(List.of(existing));

    assertTrue(manager.getCertChecked().get(type));
    assertSame(existing, manager.getCertDetails().get(type));
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
      "buildList_ISO9001DocExists_uploaded, ISO_9001",
      "buildList_ISO14001DocExists_uploaded, ISO_14001",
      "buildList_ISO27001DocExists_uploaded, ISO_27001",
      "buildList_GdprDpaDocExists_uploaded, GDPR_DPA"
  })
  void buildCertificationList_whenDocumentExists_setUploadedToTrue(
      String testName, LegalDocumentType type) {
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

  @ParameterizedTest(name = "{0}")
  @CsvSource({
      "buildList_ISO9001Checked_uploaded, ISO_9001",
      "buildList_ISO14001Checked_uploaded, ISO_14001",
      "buildList_ISO27001Checked_uploaded, ISO_27001",
      "buildList_GdprDpaChecked_uploaded, GDPR_DPA"
  })
  void buildCertificationList_whenCertificationChecked_setUploadedToTrue(
      String testName, LegalDocumentType type) {
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

  @ParameterizedTest(name = "{0}")
  @CsvSource({
      "buildList_ISO9001NoneChecked_notUploaded, ISO_9001",
      "buildList_ISO14001NoneChecked_notUploaded, ISO_14001",
      "buildList_ISO27001NoneChecked_notUploaded, ISO_27001",
      "buildList_GdprDpaNoneChecked_notUploaded, GDPR_DPA"
  })
  void buildCertificationList_whenNoDocumentAndNotChecked_setUploadedToFalse(
      String testName, LegalDocumentType type) {
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
