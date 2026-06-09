package com.axonivy.utils.smart.workflow.demo.erp.shopping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.DemoTestProcessData;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.test.RestResourceTest;

@RestResourceTest
class TestProductTools {

  private static final BpmProcess TEST_PROCESS = BpmProcess.name("DemoTestProcess");

  @Test
  void findProduct(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testFindProduct")).execute();
    DemoTestProcessData data = res.data().last();
    assertThat(data.getProductToolResult()).isEqualTo("Product with SKU   is not existing in the system");
  }
}
