package com.axonivy.utils.smart.workflow.rag.opensearch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.rag.tool.OpenSearchRagTool;
import com.axonivy.utils.smart.workflow.rag.tool.OpenSearchRagToolProvider;

public class TestOpenSearchRagToolProvider {

  private final OpenSearchRagToolProvider provider = new OpenSearchRagToolProvider();

  @Test
  void getToolsReturnsOneOpenSearchRagTool() {
    var tools = provider.getTools();
    assertThat(tools).hasSize(1);
    assertThat(tools.get(0)).isInstanceOf(OpenSearchRagTool.class);
  }
}
