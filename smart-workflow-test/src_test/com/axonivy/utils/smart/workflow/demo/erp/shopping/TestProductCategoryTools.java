package com.axonivy.utils.smart.workflow.demo.erp.shopping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.DemoTestProcessData;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.test.RestResourceTest;

@RestResourceTest
class TestProductCategoryTools {

  private static final BpmProcess TEST_PROCESS = BpmProcess.name("DemoTestProcess");

  @Test
  void findProductCategory(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testFindProductCategory")).execute();
    DemoTestProcessData data = res.data().last();
    assertThat(data.getCategoryToolResult()).isEqualTo("Product category not found");
  }

  @Test
  void createProductCategory(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testCreateProductCategory")).execute();
    DemoTestProcessData data = res.data().last();
    assertThat(data.getCategoryToolResult()).isEqualTo("Product category is created successfully");
  }
}
