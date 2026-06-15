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
    assertThat(DocumentUploader.isSameSlot(LegalDocumentBuilder.of(type), LegalDocumentBuilder.of(type))).isEqualTo(expected);
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
    assertThat(DocumentUploader.isSameSlot(LegalDocumentBuilder.of(LegalDocumentType.ISO_9001),
        LegalDocumentBuilder.of(LegalDocumentType.ISO_14001))).isFalse();
    assertThat(DocumentUploader.isSameSlot(LegalDocumentBuilder.of(LegalDocumentType.CONTRACT),
        LegalDocumentBuilder.of(LegalDocumentType.ANNUAL_REPORT))).isFalse();
  }

  @Test
  void isSameSlot_certificationBase_matchesByDescription() {
    LegalDocument brcFood = LegalDocumentBuilder.ofCert("BRC Food");
    assertThat(DocumentUploader.isSameSlot(brcFood, LegalDocumentBuilder.ofCert("BRC Food"))).isTrue();
    assertThat(DocumentUploader.isSameSlot(brcFood, LegalDocumentBuilder.ofCert("OHSAS 18001"))).isFalse();
  }

}
