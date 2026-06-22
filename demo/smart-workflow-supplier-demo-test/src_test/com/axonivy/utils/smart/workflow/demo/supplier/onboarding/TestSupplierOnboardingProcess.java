package com.axonivy.utils.smart.workflow.demo.supplier.onboarding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.SupplierDemoTestProcessData;
import com.axonivy.utils.smart.workflow.demo.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmElement;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;

@IvyProcessTest
class TestSupplierOnboardingProcess {

  private static final BpmProcess TEST_PROCESS = BpmProcess.name("SupplierDemoTestProcess");

  @Test
  void installDemoData_allRepositoriesSeededWithExpectedCounts(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testInstallDemoData")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getPolicyRuleCount()).isGreaterThan(0);
    assertThat(data.getEmployeeCount()).isGreaterThan(0);
    assertThat(data.getDepartmentCount()).isGreaterThan(0);
    assertThat(data.getSupplierCount()).isGreaterThan(0);
  }

  @Test
  void duplicateCheck_capturesAuditEntry(BpmClient client) {
    Supplier matchedSupplier = new Supplier();
    matchedSupplier.setBusinessName("ACME Corp");

    SupplierAgentResponse mockResponse = new SupplierAgentResponse();
    mockResponse.setSuppliers(List.of(matchedSupplier));
    mockResponse.setIsSupplierExisting(true);

    client.mock()
      .element(BpmElement.process(TEST_PROCESS).name("AI Agent: Check duplicate supplier"))
      .with((params, results) -> {
        try {
          results.set("agentResponse", mockResponse);
        } catch (NoSuchFieldException e) {
          throw new RuntimeException(e);
        }
      });

    var res = client.start()
      .process(TEST_PROCESS.elementName("testDuplicateCheckAuditCapture"))
      .execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getAuditEntryCount()).isEqualTo(1);
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
