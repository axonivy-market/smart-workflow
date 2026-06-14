package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.agent.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AgentStepStatus;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.FindingSeverity;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestCrossReferenceService {

  @Test
  void validateCompanyRegister_validFormats_returnsValid() {
    assertThat(CrossReferenceService.validateCompanyRegister("HRB12345", "DE").getSeverity())
        .isEqualTo(FindingSeverity.PASSED);
    assertThat(CrossReferenceService.validateCompanyRegister("REG123", "CH").getSeverity())
        .isEqualTo(FindingSeverity.PASSED);
  }

  @Test
  void validateVatId_validDe_returnsConfirmed() {
    assertThat(CrossReferenceService.validateVatId("DE123456789", "DE").getSeverity())
        .isEqualTo(FindingSeverity.PASSED);
    assertThat(CrossReferenceService.validateVatId("DE123456789", "DE").getMessage())
        .contains("confirmed");
  }

  @Test
  void checkErpDuplicate_matchOutcome_dependsOnSupplierId() {
    Supplier other = new Supplier();
    other.setSupplierId("OTHER1");
    assertThat(CrossReferenceService.checkErpDuplicate("S001", java.util.List.of(other)).getSeverity())
        .isEqualTo(FindingSeverity.WARNING);

    Supplier same = new Supplier();
    same.setSupplierId("S001");
    assertThat(CrossReferenceService.checkErpDuplicate("S001", java.util.List.of(same)).getSeverity())
        .isEqualTo(FindingSeverity.PASSED);
  }

  @Test
  void startCrossReferenceStep_statusIsRunning() {
    AgentProcessingStep step = CrossReferenceService.startCrossReferenceStep();

    assertThat(step.getStatus()).isEqualTo(AgentStepStatus.RUNNING);
    assertThat(step.getStartedAt()).isNotNull();
  }
}
