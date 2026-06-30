package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.ClarificationProblemType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskKind;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestClarificationProblemTypeBuilder {

  @Test
  void resolve_documentTypeKeyOrMissingDocKind_returnsDocument() {
    assertThat(ClarificationProblemTypeBuilder.resolve(
        "CERTIFICATION:ISO_9001", null, null, null))
        .isEqualTo(ClarificationProblemType.DOCUMENT);
    assertThat(ClarificationProblemTypeBuilder.resolve(
        null, RiskKind.MISSING_DOC, null, null))
        .isEqualTo(ClarificationProblemType.DOCUMENT);
  }

  @Test
  void resolve_duplicateKeyword_inSourceOrMessage_returnsDuplicate() {
    assertThat(ClarificationProblemTypeBuilder.resolve(
        null, RiskKind.AI_VALIDATION, "ERP Duplicate Check", null))
        .isEqualTo(ClarificationProblemType.DUPLICATE);
    assertThat(ClarificationProblemTypeBuilder.resolve(
        null, RiskKind.AI_VALIDATION, null, "Possible ERP duplicate found"))
        .isEqualTo(ClarificationProblemType.DUPLICATE);
    assertThat(ClarificationProblemTypeBuilder.resolve(
        null, RiskKind.AI_VALIDATION, "DUPLICATE ENTRY", null))
        .isEqualTo(ClarificationProblemType.DUPLICATE);
  }

  @Test
  void resolve_noMatch_returnsOther() {
    assertThat(ClarificationProblemTypeBuilder.resolve(
        null, RiskKind.AI_VALIDATION, "VAT Validation", "VAT format invalid"))
        .isEqualTo(ClarificationProblemType.OTHER);
    assertThat(ClarificationProblemTypeBuilder.resolve(null, null, null, null))
        .isEqualTo(ClarificationProblemType.OTHER);
  }

  @Test
  void resolve_fromFinding_delegatesToFields() {
    ValidationFinding withDocKey = new ValidationFinding();
    withDocKey.setDocumentTypeKey("CERTIFICATION:ISO_9001");
    withDocKey.setRiskKind(RiskKind.AI_VALIDATION);
    assertThat(ClarificationProblemTypeBuilder.resolve(withDocKey))
        .isEqualTo(ClarificationProblemType.DOCUMENT);

    ValidationFinding missingDoc = new ValidationFinding();
    missingDoc.setRiskKind(RiskKind.MISSING_DOC);
    assertThat(ClarificationProblemTypeBuilder.resolve(missingDoc))
        .isEqualTo(ClarificationProblemType.DOCUMENT);
  }
}
