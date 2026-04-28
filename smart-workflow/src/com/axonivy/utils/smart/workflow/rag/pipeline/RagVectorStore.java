package com.axonivy.utils.smart.workflow.rag.pipeline;

import java.util.List;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;

public interface RagVectorStore {

  void addAll(List<Embedding> embeddings, List<TextSegment> segments);

  EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request);
}
