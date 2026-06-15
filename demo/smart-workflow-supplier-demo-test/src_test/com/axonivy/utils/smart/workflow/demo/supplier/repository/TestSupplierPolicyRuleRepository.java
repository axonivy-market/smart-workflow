package com.axonivy.utils.smart.workflow.demo.supplier.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.SupplierDemoTestProcessData;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RuleType;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;

@IvyProcessTest
class TestSupplierPolicyRuleRepository {

  private static final BpmProcess TEST_PROCESS = BpmProcess.name("SupplierDemoTestProcess");

  @Test
  void findAll_returnsAllSeededRules(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testPolicyRuleRepo_findAll")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getPolicyRuleCount()).isEqualTo(18);
  }

  @Test
  void findByTarget_existingTarget_returnsRule(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testPolicyRuleRepo_findByTarget")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getFoundPolicyRule()).isNotNull();
    assertThat(data.getFoundPolicyRule().getRuleType()).isEqualTo(RuleType.POLICY);
  }
}
