package com.axonivy.utils.smart.workflow.rag.pipeline;

import java.util.Map;
import java.util.stream.Collectors;

import com.axonivy.utils.smart.workflow.rag.entity.RagMatch;
import com.axonivy.utils.smart.workflow.rag.entity.RagResult;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;

public interface RagRetriever {

  RagConnector getConnector();

  RagResult search(String collection, String query, Integer maxResults, Double minScore);

  default RagResult performSearch(String collection, String query, int maxResults, double minScore, EmbeddingModel embeddingModel) {
    var queryEmbedding = embeddingModel.embed(query).content();
    var searchRequest = EmbeddingSearchRequest.builder()
        .queryEmbedding(queryEmbedding)
        .maxResults(maxResults)
        .minScore(minScore)
        .build();
    RagVectorStore store = getConnector().connectStore(collection);
    try {
      var matches = store.search(searchRequest).matches().stream()
          .map(match -> new RagMatch(
              match.embedded().text(),
              match.score(),
              toStringMap(match.embedded().metadata().toMap())))
          .toList();
      return new RagResult(matches);
    } finally {
      store.close();
    }
  }

  private static Map<String, String> toStringMap(Map<String, Object> source) {
    return source.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> String.valueOf(entry.getValue())));
  }
}
