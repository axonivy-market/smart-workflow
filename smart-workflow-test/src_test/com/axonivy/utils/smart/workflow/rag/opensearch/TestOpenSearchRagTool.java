package com.axonivy.utils.smart.workflow.rag.opensearch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.rag.tool.OpenSearchRagTool;

public class TestOpenSearchRagTool {

  private final OpenSearchRagTool tool = new OpenSearchRagTool();

  @Test
  void nameIsOpenSearchSearch() {
    assertThat(tool.name()).isEqualTo("openSearchSearch");
  }

  @Test
  void descriptionIsNotBlank() {
    assertThat(tool.description()).isNotBlank();
  }

  @Test
  void parametersHaveCorrectNamesAndTypes() {
    var params = tool.parameters();
    assertThat(params).hasSize(4);
    assertThat(params).extracting("name")
        .containsExactly("collection", "query", "maxResults", "minScore");
    assertThat(params).extracting("type")
        .containsExactly("java.lang.String", "java.lang.String",
            "java.lang.Integer", "java.lang.Double");
  }

  @Test
  void parametersAllHaveNonBlankDescriptions() {
    tool.parameters().forEach(p ->
        assertThat(p.description()).as("description for %s", p.name()).isNotBlank());
  }
}
