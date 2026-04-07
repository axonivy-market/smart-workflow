package com.axonivy.utils.smart.workflow.rag.pipeline.internal;

import java.io.IOException;
import java.util.List;

import com.axonivy.utils.smart.workflow.rag.pipeline.RagVectorStore;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.opensearch.OpenSearchEmbeddingStore;
import org.opensearch.client.opensearch.OpenSearchClient;

public class OpenSearchVectorStore implements RagVectorStore {

  private static final String ERR_CLOSE_CLIENT = "Failed to close OpenSearch client";

  private final OpenSearchEmbeddingStore delegate;
  private final OpenSearchClient client;

  public OpenSearchVectorStore(OpenSearchEmbeddingStore delegate, OpenSearchClient client) {
    this.delegate = delegate;
    this.client = client;
  }

  @Override
  public void addAll(List<Embedding> embeddings, List<TextSegment> segments) {
    delegate.addAll(embeddings, segments);
  }

  @Override
  public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
    return delegate.search(request);
  }

  @Override
  public void close() {
    try {
      client._transport().close();
    } catch (IOException ex) {
      throw new IllegalStateException(ERR_CLOSE_CLIENT, ex);
    }
  }

}
