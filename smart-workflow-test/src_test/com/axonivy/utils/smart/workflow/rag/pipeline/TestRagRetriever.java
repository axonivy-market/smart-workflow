package com.axonivy.utils.smart.workflow.rag.pipeline;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;

/**
 * Unit tests for the {@code performSearch} default method of {@link RagRetriever}.
 *
 * <p>All dependencies are passed as parameters, so tests use lightweight anonymous stubs
 * without requiring an Ivy environment or a live vector store.
 */
public class TestRagRetriever {

  // abstract method is not under test
  @SuppressWarnings("unused")
  private final RagRetriever retriever = (collection, query, maxResults, minScore) -> {
    throw new UnsupportedOperationException("search() not under test");
  };

  private final Embedding stubEmbedding = Embedding.from(new float[]{0.1f, 0.2f, 0.3f});

  @SuppressWarnings("unused")
  private final EmbeddingModel embeddingModel = textSegments ->
      Response.from(textSegments.stream()
          .map(ts -> stubEmbedding)
          .toList());

  @Test
  void performSearchReturnsEmptyMatchesWhenNoResults() {
    var result = retriever.performSearch(
        connectorReturning(List.of()), "my-index", "query", 5, 0.6, embeddingModel);

    assertThat(result.getMatches()).isEmpty();
    assertThat(result.hasError()).isFalse();
  }

  @Test
  void performSearchMapsMatchContentAndScore() {
    TextSegment segment = TextSegment.from("Hello world");
    EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.9, "id-1", stubEmbedding, segment);

    var result = retriever.performSearch(
        connectorReturning(List.of(match)), "my-index", "query", 5, 0.6, embeddingModel);

    assertThat(result.getMatches()).hasSize(1);
    assertThat(result.getMatches().get(0).getContent()).isEqualTo("Hello world");
    assertThat(result.getMatches().get(0).getScore()).isEqualTo(0.9);
  }

  @Test
  void performSearchConvertsMetadataToStringMap() {
    Metadata metadata = Metadata.from("source", "doc.pdf");
    TextSegment segment = TextSegment.from("Content", metadata);
    EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.8, "id-2", stubEmbedding, segment);

    var result = retriever.performSearch(
        connectorReturning(List.of(match)), "my-index", "query", 5, 0.5, embeddingModel);

    var meta = result.getMatches().get(0).getMetadata();
    assertThat(meta).containsEntry("source", "doc.pdf");
  }

  @Test
  void performSearchMapsMultipleMetadataEntries() {
    Metadata metadata = Metadata.from("source", "doc.pdf")
        .put("page", "3");
    TextSegment segment = TextSegment.from("Content", metadata);
    EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(0.7, "id-3", stubEmbedding, segment);

    var result = retriever.performSearch(
        connectorReturning(List.of(match)), "my-index", "query", 5, 0.5, embeddingModel);

    var meta = result.getMatches().get(0).getMetadata();
    assertThat(meta).containsEntry("source", "doc.pdf").containsEntry("page", "3");
  }

  private static RagConnector connectorReturning(List<EmbeddingMatch<TextSegment>> matches) {
    return new RagConnector() {
      @Override
      public boolean indexExists(String collection) {
        return true;
      }

      @Override
      public RagVectorStore connect(String collection) {
        return new RagVectorStore() {
          @Override
          public void addAll(List<Embedding> embeddings, List<TextSegment> segments) {}

          @Override
          public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
            return new EmbeddingSearchResult<>(matches);
          }
        };
      }
    };
  }
}
