package com.axonivy.utils.smart.workflow.demo.employee;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.SupplierDemoTestProcessData;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;

@IvyProcessTest
class TestEmployeeRepository {

  private static final BpmProcess TEST_PROCESS = BpmProcess.name("SupplierDemoTestProcess");

  @Test
  void findAll_returnsAllSeededEmployees(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testEmployeeRepo_findAll")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getEmployeeCount()).isEqualTo(10);
  }

  @Test
  void findByUsername_existingUser_returnsEmployee(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testEmployeeRepo_findByUsername")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getFoundEmployee()).isNotNull();
    assertThat(data.getFoundEmployee().getFirstName()).isEqualTo("Sandra");
    assertThat(data.getFoundEmployee().getLastName()).isEqualTo("Collins");
    assertThat(data.getFoundEmployee().getRole()).isEqualTo("ProcurementDirector");
  }

  @Test
  void create_addsEmployeeToList(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testEmployeeRepo_create")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getEmployeeCount()).isEqualTo(11);
  }
}
