package com.axonivy.utils.smart.workflow.demo.supplier.onboarding;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.SupplierDemoTestProcessData;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;

@IvyProcessTest
class TestSupplierOnboardingProcess {

  private static final BpmProcess TEST_PROCESS = BpmProcess.name("SupplierDemoTestProcess");

  @Test
  void installDemoData_allRepositoriesSeededWithExpectedCounts(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testInstallDemoData")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getPolicyRuleCount()).isEqualTo(18);
    assertThat(data.getEmployeeCount()).isEqualTo(10);
    assertThat(data.getDepartmentCount()).isEqualTo(5);
    assertThat(data.getSupplierCount()).isEqualTo(11);
  }

  @Test
  void clearDemoData_afterInstall_allRepositoriesAreEmpty(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testClearDemoData")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getPolicyRuleCount()).isZero();
    assertThat(data.getEmployeeCount()).isZero();
    assertThat(data.getDepartmentCount()).isZero();
    assertThat(data.getSupplierCount()).isZero();
  }
}
