package com.axonivy.utils.smart.workflow.rag.opensearch.internal;

import java.time.Instant;

public record OpenSearchIndexMeta(
    String embeddingProvider,
    String embeddingModel,
    int chunkSize,
    int chunkOverlap,
    String createdAt,
    String lastIngestedAt) {

  public OpenSearchIndexMeta(String embeddingProvider, String embeddingModel, int chunkSize, int chunkOverlap) {
    this(embeddingProvider, embeddingModel, chunkSize, chunkOverlap, Instant.now().toString(), null);
  }
}
