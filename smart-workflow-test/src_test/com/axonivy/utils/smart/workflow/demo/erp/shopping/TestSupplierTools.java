package com.axonivy.utils.smart.workflow.demo.erp.shopping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.DemoTestProcessData;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.test.RestResourceTest;

@RestResourceTest
class TestSupplierTools {

  private static final BpmProcess TEST_PROCESS = BpmProcess.name("DemoTestProcess");

  @Test
  void findSupplier(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testFindSupplier")).execute();
    DemoTestProcessData data = res.data().last();
    assertThat(data.getSupplierToolResult()).isEqualTo("Supplier not found");
  }

  @Test
  void createSupplier(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testCreateSupplier")).execute();
    DemoTestProcessData data = res.data().last();
    assertThat(data.getSupplierToolResult()).isEqualTo("Supplier is created successfully");
  }
}
