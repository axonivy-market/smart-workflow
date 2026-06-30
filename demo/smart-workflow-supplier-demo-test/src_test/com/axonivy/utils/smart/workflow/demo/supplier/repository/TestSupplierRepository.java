package com.axonivy.utils.smart.workflow.demo.supplier.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.SupplierDemoTestProcessData;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;

@IvyProcessTest
class TestSupplierRepository {

  private static final BpmProcess TEST_PROCESS = BpmProcess.name("SupplierDemoTestProcess");

  @Test
  void findAll_returnsAllSeededSuppliers(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testSupplierRepo_findAll")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getSupplierCount()).isEqualTo(11);
  }

  @Test
  void findById_existingId_returnsSupplier(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testSupplierRepo_findById")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getFoundSupplier()).isNotNull();
    assertThat(data.getFoundSupplier().getBusinessName()).isEqualTo("Holzmann & Partner GmbH");
    assertThat(data.getFoundSupplier().getVatId()).isEqualTo("DE198765432");
  }
}
