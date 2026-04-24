package com.axonivy.utils.smart.workflow.rag.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.environment.IvyTest;

/**
 * Unit tests for the default methods of {@link RagIngestor}.
 *
 * <p>Uses a minimal stub connector that throws if called, so tests are valid
 * only for paths that return before reaching the connector (empty / blank sources).
 */
@IvyTest
public class TestRagIngestor {

  private final RagIngestor ingestor = () -> {
    throw new UnsupportedOperationException("connector should not be reached");
  };

  @Test
  void ingestEmptySourcesReturnsNoContentError() {
    var result = ingestor.ingest("my-index", List.of(), 300, 20);

    assertThat(result.hasError()).isTrue();
    assertThat(result.getError()).isEqualTo(RagIngestor.MSG_NO_CONTENT);
  }

  @Test
  void ingestBlankSourcesReturnsNoContentError() {
    var result = ingestor.ingest("my-index", List.of("", "  ", "\t"), 300, 20);

    assertThat(result.hasError()).isTrue();
    assertThat(result.getError()).isEqualTo(RagIngestor.MSG_NO_CONTENT);
  }

  @Test
  void noContentMessageConstantIsStable() {
    assertThat(RagIngestor.MSG_NO_CONTENT).isNotBlank();
  }

  @Test
  void indexedFormatMessageContainsSizePlaceholder() {
    assertThat(RagIngestor.MSG_INDEXED_FORMAT).contains("%d");
    assertThat(String.format(RagIngestor.MSG_INDEXED_FORMAT, 42)).contains("42");
  }
}
