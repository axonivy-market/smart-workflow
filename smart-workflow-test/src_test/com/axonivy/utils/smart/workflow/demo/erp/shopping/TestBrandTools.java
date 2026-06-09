package com.axonivy.utils.smart.workflow.demo.erp.shopping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.DemoTestProcessData;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.test.RestResourceTest;

@RestResourceTest
class TestBrandTools {

  private static final BpmProcess TEST_PROCESS = BpmProcess.name("DemoTestProcess");

  @Test
  void findProductBrand(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testFindProductBrand")).execute();
    DemoTestProcessData data = res.data().last();
    assertThat(data.getBrandToolResult()).isEqualTo("Brand not found");
  }

  @Test
  void createProductBrand(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testCreateProductBrand")).execute();
    DemoTestProcessData data = res.data().last();
    assertThat(data.getBrandToolResult()).isEqualTo("Product brand is created successfully");
  }
}
