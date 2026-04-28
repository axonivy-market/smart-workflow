package com.axonivy.utils.smart.workflow.rag.pipeline;

import java.util.Map;
import java.util.stream.Collectors;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;

public interface RagRetriever {

  RagResult search(String collection, String query, int maxResults, double minScore);

  default RagResult performSearch(RagConnector connector, String collection, String query, int maxResults, double minScore, EmbeddingModel embeddingModel) {
    var queryEmbedding = embeddingModel.embed(query).content();
    var searchRequest = EmbeddingSearchRequest.builder()
        .queryEmbedding(queryEmbedding)
        .maxResults(maxResults)
        .minScore(minScore)
        .build();
    RagVectorStore store = connector.vectorStore(collection);
    var matches = store.search(searchRequest).matches().stream()
        .map(match -> new RagMatch(
            match.embedded().text(),
            match.score(),
            toStringMap(match.embedded().metadata().toMap())))
        .toList();
    return new RagResult(matches);
  }

  private static Map<String, String> toStringMap(Map<String, Object> source) {
    return source.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> String.valueOf(entry.getValue())));
  }
}
