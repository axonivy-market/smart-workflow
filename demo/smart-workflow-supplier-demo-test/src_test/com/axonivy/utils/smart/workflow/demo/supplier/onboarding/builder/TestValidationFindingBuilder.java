package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.FindingSeverity;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.ValidationFindingType;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestValidationFindingBuilder {

  @Test
  void of_explicitFields_setsAllProperties() {
    ValidationFinding finding = ValidationFindingBuilder.of(
        FindingSeverity.FAILURE, "VAT ID invalid", "VAT Validation", RiskType.POLICY_COMPLIANCE);

    assertThat(finding.getSeverity()).isEqualTo(FindingSeverity.FAILURE);
    assertThat(finding.getMessage()).isEqualTo("VAT ID invalid");
    assertThat(finding.getSource()).isEqualTo("VAT Validation");
    assertThat(finding.getRiskType()).isEqualTo(RiskType.POLICY_COMPLIANCE);
  }

  @Test
  void of_fromType_noArgs_usesTemplateDirectly() {
    ValidationFinding finding = ValidationFindingBuilder.of(ValidationFindingType.VAT_ID_MISSING);

    assertThat(finding.getSeverity()).isEqualTo(FindingSeverity.WARNING);
    assertThat(finding.getMessage()).isEqualTo(
        "No VAT ID provided — may be exempt for sole traders or certain legal forms");
    assertThat(finding.getSource()).isEqualTo("VAT Validation");
    assertThat(finding.getRiskType()).isEqualTo(RiskType.POLICY_COMPLIANCE);
  }

  @Test
  void of_fromType_withArgs_formatsMessage() {
    ValidationFinding finding = ValidationFindingBuilder.of(
        ValidationFindingType.VAT_ID_INVALID, "DE123", "DE", "DE + 9 digits");

    assertThat(finding.getSeverity()).isEqualTo(FindingSeverity.FAILURE);
    assertThat(finding.getMessage()).contains("DE123").contains("DE");
  }

  @Test
  void of_fromType_severityVariants_passedAndWarning() {
    assertThat(ValidationFindingBuilder.of(ValidationFindingType.ERP_NO_SIMILAR).getSeverity())
        .isEqualTo(FindingSeverity.PASSED);

    ValidationFinding warning = ValidationFindingBuilder.of(ValidationFindingType.ERP_DUPLICATE_FOUND, 3);
    assertThat(warning.getSeverity()).isEqualTo(FindingSeverity.WARNING);
    assertThat(warning.getMessage()).contains("3");
  }
}
