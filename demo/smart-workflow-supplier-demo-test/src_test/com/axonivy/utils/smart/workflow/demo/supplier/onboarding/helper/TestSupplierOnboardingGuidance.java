package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.assistant.AgentGuidance;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestSupplierOnboardingGuidance {

  @Test
  void allGuidanceLists_haveExpectedSize() {
    assertThat(SupplierOnboardingGuidance.forRequest()).hasSize(4);
    assertThat(SupplierOnboardingGuidance.forRegistration()).hasSize(6);
    assertThat(SupplierOnboardingGuidance.forAgentProcessing()).hasSize(4);
    assertThat(SupplierOnboardingGuidance.forDuplicateCheck()).hasSize(4);
  }

  @Test
  void allEntries_haveNonBlankQuestionAndInstruction() {
    List<List<AgentGuidance>> allLists = List.of(
        SupplierOnboardingGuidance.forRequest(),
        SupplierOnboardingGuidance.forRegistration(),
        SupplierOnboardingGuidance.forAgentProcessing(),
        SupplierOnboardingGuidance.forDuplicateCheck()
    );
    for (List<AgentGuidance> list : allLists) {
      for (AgentGuidance entry : list) {
        assertThat(entry.getQuestionPattern()).isNotBlank();
        assertThat(entry.getInstruction()).isNotBlank();
      }
    }
  }
}
