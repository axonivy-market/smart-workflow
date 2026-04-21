package com.axonivy.utils.smart.workflow.rag.opensearch.internal;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

public class TestOpenSearchIndexMeta {

  @Test
  void constructorSetsFields() {
    var meta = new OpenSearchIndexMeta("gemini", "text-embedding-004", 500, 50);

    assertThat(meta.embeddingProvider()).isEqualTo("gemini");
    assertThat(meta.embeddingModel()).isEqualTo("text-embedding-004");
    assertThat(meta.chunkSize()).isEqualTo(500);
    assertThat(meta.chunkOverlap()).isEqualTo(50);
    assertThat(meta.createdAt()).isNotBlank();
  }
}
