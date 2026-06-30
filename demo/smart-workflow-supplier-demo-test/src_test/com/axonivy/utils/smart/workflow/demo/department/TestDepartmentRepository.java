package com.axonivy.utils.smart.workflow.demo.department;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.SupplierDemoTestProcessData;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;

@IvyProcessTest
class TestDepartmentRepository {

  private static final BpmProcess TEST_PROCESS = BpmProcess.name("SupplierDemoTestProcess");

  @Test
  void findAll_returnsAllSeededDepartments(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testDepartmentRepo_findAll")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getDepartmentCount()).isEqualTo(5);
  }

  @Test
  void findById_existingId_returnsDepartment(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testDepartmentRepo_findById")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getFoundDepartment()).isNotNull();
    assertThat(data.getFoundDepartment().getName()).isEqualTo("Lumber & Building Materials");
    assertThat(data.getFoundDepartment().getFirstLevelManager()).isEqualTo("robert.hayes");
    assertThat(data.getFoundDepartment().getSecondLevelManager()).isEqualTo("sandra.collins");
  }
}
