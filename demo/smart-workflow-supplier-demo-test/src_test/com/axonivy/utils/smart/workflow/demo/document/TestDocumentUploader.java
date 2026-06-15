package com.axonivy.utils.smart.workflow.demo.document;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestDocumentUploader {

  @ParameterizedTest
  @MethodSource
  void isSameSlot_sameType_returnsExpected(LegalDocumentType type, boolean expected) {
    assertThat(DocumentUploader.isSameSlot(docOf(type), docOf(type))).isEqualTo(expected);
  }

  @SuppressWarnings("unused")
  static Stream<Arguments> isSameSlot_sameType_returnsExpected() {
    return Stream.of(
        Arguments.of(LegalDocumentType.COMMERCIAL_REGISTER, true),
        Arguments.of(LegalDocumentType.SELF_DECLARATION, true),
        Arguments.of(LegalDocumentType.ANNUAL_REPORT, true),
        Arguments.of(LegalDocumentType.ISO_9001, true),
        Arguments.of(LegalDocumentType.BANKING_CONFIRMATION, false),
        Arguments.of(LegalDocumentType.CONTRACT, false)
    );
  }

  @Test
  void isSameSlot_differentTypes_returnsFalse() {
    assertThat(DocumentUploader.isSameSlot(docOf(LegalDocumentType.ISO_9001),
        docOf(LegalDocumentType.ISO_14001))).isFalse();
    assertThat(DocumentUploader.isSameSlot(docOf(LegalDocumentType.CONTRACT),
        docOf(LegalDocumentType.ANNUAL_REPORT))).isFalse();
  }

  @Test
  void isSameSlot_certificationBase_matchesByDescription() {
    LegalDocument brcFood = docOfCert("BRC Food");
    assertThat(DocumentUploader.isSameSlot(brcFood, docOfCert("BRC Food"))).isTrue();
    assertThat(DocumentUploader.isSameSlot(brcFood, docOfCert("OHSAS 18001"))).isFalse();
  }

  private static LegalDocument docOf(LegalDocumentType type) {
    LegalDocument doc = new LegalDocument();
    doc.setDocumentType(type);
    return doc;
  }

  private static LegalDocument docOfCert(String description) {
    LegalDocument doc = docOf(LegalDocumentType.CERTIFICATION);
    doc.setDescription(description);
    return doc;
  }
}
