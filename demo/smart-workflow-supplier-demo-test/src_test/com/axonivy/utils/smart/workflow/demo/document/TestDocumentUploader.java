package com.axonivy.utils.smart.workflow.demo.document;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestDocumentUploader {

  @Test
  void isSameSlot_differentTypes_returnsFalse() {
    assertThat(DocumentUploader.isSameSlot(docOf(LegalDocumentType.CONTRACT),
        docOf(LegalDocumentType.ANNUAL_REPORT))).isFalse();
  }

  @Test
  void isSameSlot_certificationSubtype_sameOrDifferent() {
    assertThat(DocumentUploader.isSameSlot(docOf(LegalDocumentType.ISO_9001),
        docOf(LegalDocumentType.ISO_9001))).isTrue();
    assertThat(DocumentUploader.isSameSlot(docOf(LegalDocumentType.ISO_9001),
        docOf(LegalDocumentType.ISO_14001))).isFalse();
  }

  @Test
  void isSameSlot_certificationBase_sameOrDifferentDescription() {
    LegalDocument existing = docOf(LegalDocumentType.CERTIFICATION);
    existing.setDescription("BRC Food");
    LegalDocument sameDesc = docOf(LegalDocumentType.CERTIFICATION);
    sameDesc.setDescription("BRC Food");
    assertThat(DocumentUploader.isSameSlot(existing, sameDesc)).isTrue();

    LegalDocument diffDesc = docOf(LegalDocumentType.CERTIFICATION);
    diffDesc.setDescription("OHSAS 18001");
    assertThat(DocumentUploader.isSameSlot(existing, diffDesc)).isFalse();
  }

  @Test
  void isSameSlot_singleInstanceTypes_returnsTrue() {
    assertThat(DocumentUploader.isSameSlot(docOf(LegalDocumentType.COMMERCIAL_REGISTER),
        docOf(LegalDocumentType.COMMERCIAL_REGISTER))).isTrue();
    assertThat(DocumentUploader.isSameSlot(docOf(LegalDocumentType.SELF_DECLARATION),
        docOf(LegalDocumentType.SELF_DECLARATION))).isTrue();
    assertThat(DocumentUploader.isSameSlot(docOf(LegalDocumentType.ANNUAL_REPORT),
        docOf(LegalDocumentType.ANNUAL_REPORT))).isTrue();
  }

  @Test
  void isSameSlot_multiInstanceTypes_returnsFalse() {
    assertThat(DocumentUploader.isSameSlot(docOf(LegalDocumentType.BANKING_CONFIRMATION),
        docOf(LegalDocumentType.BANKING_CONFIRMATION))).isFalse();
    assertThat(DocumentUploader.isSameSlot(docOf(LegalDocumentType.CONTRACT),
        docOf(LegalDocumentType.CONTRACT))).isFalse();
  }

  private static LegalDocument docOf(LegalDocumentType type) {
    LegalDocument doc = new LegalDocument();
    doc.setDocumentType(type);
    return doc;
  }
}
