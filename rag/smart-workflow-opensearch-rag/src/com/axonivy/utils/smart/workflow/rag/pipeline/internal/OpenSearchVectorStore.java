package com.axonivy.utils.smart.workflow.rag.pipeline.internal;

import java.util.List;

import com.axonivy.utils.smart.workflow.rag.opensearch.internal.OpenSearchIndexMeta;
import com.axonivy.utils.smart.workflow.rag.opensearch.internal.OpenSearchRestClient;
import com.axonivy.utils.smart.workflow.rag.pipeline.RagVectorStore;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;

public class OpenSearchVectorStore implements RagVectorStore {

  private static final int DEFAULT_DIMENSION = 384;

  private final OpenSearchRestClient client;
  private final String indexName;
  private final OpenSearchIndexMeta meta;

  public OpenSearchVectorStore(OpenSearchRestClient client, String indexName, OpenSearchIndexMeta meta) {
    this.client = client;
    this.indexName = indexName;
    this.meta = meta;
  }

  @Override
  public void addAll(List<Embedding> embeddings, List<TextSegment> segments) {
    if (!client.indexExists(indexName)) {
      int dimension = embeddings.isEmpty() ? DEFAULT_DIMENSION : embeddings.get(0).dimension();
      client.createIndex(indexName, dimension, meta);
    }
    client.bulkIngest(indexName, embeddings, segments);
  }

  @Override
  public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
    return client.search(indexName, request);
  }


}
