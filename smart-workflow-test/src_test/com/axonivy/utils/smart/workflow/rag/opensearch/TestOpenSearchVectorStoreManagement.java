package com.axonivy.utils.smart.workflow.rag.opensearch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.rag.pipeline.RagIngestor;
import com.axonivy.utils.smart.workflow.rag.pipeline.internal.OpenSearchIngestor;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestOpenSearchVectorStoreManagement {

  private final OpenSearchIngestor ingestor = new OpenSearchIngestor();

  @Test
  void ingestEmptySourcesReturnsNoContentError() {
    assertThat(RagIngestor.MSG_NO_CONTENT).isNotBlank();
    var result = ingestor.ingest("my-index", List.of());

    assertThat(result.hasError()).isTrue();
    assertThat(result.getError()).isEqualTo(RagIngestor.MSG_NO_CONTENT);
  }

  @Test
  void ingestWithoutEmbeddingProviderReturnsError() {
    var result = ingestor.ingest("my-index", List.of("Hello world, this is some test content."));

    assertThat(result.hasError()).isTrue();
  }

  @Test
  void ingestorConnectorBinding() {
    assertThat(ingestor.connector()).isInstanceOf(
        com.axonivy.utils.smart.workflow.rag.pipeline.internal.OpenSearchConnector.class);

    var customConnector = new com.axonivy.utils.smart.workflow.rag.pipeline.internal.OpenSearchConnector();
    var customIngestor = new OpenSearchIngestor(customConnector);

    assertThat(customIngestor.connector()).isSameAs(customConnector);
  }
}
