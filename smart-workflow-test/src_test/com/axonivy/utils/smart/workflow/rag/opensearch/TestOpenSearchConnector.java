package com.axonivy.utils.smart.workflow.rag.opensearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.rag.pipeline.internal.OpenSearchConnector;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestOpenSearchConnector {

  private final OpenSearchConnector connector = new OpenSearchConnector();

  @Test
  void indexExistsReturnsFalseWithoutConfig() {
    assertThat(connector.indexExists("my-index")).isFalse();
    assertThat(connector.indexExists("")).isFalse();
    assertThat(connector.indexExists(null)).isFalse();
  }

  @Test
  void missingMandatoryConfigs() {
    assertThatThrownBy(() -> connector.connect(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("collection is required");

    assertThatThrownBy(() -> connector.connect("my-index"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("AI.RAG.OpenSearch.Url is not configured");
  }
}
