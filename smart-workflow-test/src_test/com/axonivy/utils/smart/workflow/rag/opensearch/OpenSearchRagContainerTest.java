package com.axonivy.utils.smart.workflow.rag.opensearch;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import com.axonivy.utils.smart.workflow.rag.RagConf;
import com.axonivy.utils.smart.workflow.rag.pipeline.internal.OpenSearchConnector;
import com.axonivy.utils.smart.workflow.rag.pipeline.internal.OpenSearchVectorStore;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;

@Testcontainers
@IvyTest
class OpenSearchRagContainerTest {

  @SuppressWarnings("resource")
  @Container
  static GenericContainer<?> openSearch = new GenericContainer<>("opensearchproject/opensearch:2.11.0")
      .withEnv("discovery.type", "single-node")
      .withEnv("DISABLE_SECURITY_PLUGIN", "true")
      .withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true")
      .withExposedPorts(9200)
      .waitingFor(Wait.forHttp("/_cat/health").forStatusCode(200)
          .withStartupTimeout(Duration.ofMinutes(3)));

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var("AI.RAG.OpenSearch.Url",
        "http://localhost:" + openSearch.getMappedPort(9200));
    fixture.var("AI.RAG.OpenSearch.TrustSelfSignedCertificates", "false");
    fixture.var(RagConf.EMBEDDING_PROVIDER, "OpenAI");
    fixture.var(RagConf.EMBEDDING_MODEL_NAME, "text-embedding-3-small");
  }

  @Test
  void indexExistsReturnsFalseForMissingIndex() {
    assertThat(new OpenSearchConnector().indexExists("it-non-existent")).isFalse();
  }

  @Test
  void connectReturnsVectorStore() {
    var store = new OpenSearchConnector().vectorStore("it-connect-index");
    assertThat(store).isInstanceOf(OpenSearchVectorStore.class);
  }

  @Test
  void addAllCreatesIndexLazily() {
    new OpenSearchConnector().vectorStore("it-lazy-index")
        .addAll(
            List.of(Embedding.from(new float[]{0.1f, 0.2f, 0.3f})),
            List.of(TextSegment.from("Lazy index creation test")));

    assertThat(new OpenSearchConnector().indexExists("it-lazy-index")).isTrue();
  }

  @Test
  void searchAfterIngestReturnsMatchingSegment() {
    var store = new OpenSearchConnector().vectorStore("it-search-index");
    float[] vector = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
    Embedding embedding = Embedding.from(vector);

    store.addAll(
        List.of(embedding),
        List.of(TextSegment.from("OpenSearch integration test document")));

    var request = EmbeddingSearchRequest.builder()
        .queryEmbedding(embedding)
        .maxResults(5)
        .minScore(0.0)
        .build();
    Awaitility.await()
        .atMost(Duration.ofSeconds(5))
        .pollInterval(Duration.ofMillis(200))
        .until(() -> !store.search(request).matches().isEmpty());

    var result = store.search(request);
    assertThat(result.matches()).isNotEmpty();
    assertThat(result.matches().get(0).embedded().text())
        .isEqualTo("OpenSearch integration test document");
  }

  @Test
  void searchReturnsTopKByScore() {
    var store = new OpenSearchConnector().vectorStore("it-topk-index");
    float[] queryVector = {1.0f, 0.0f, 0.0f};

    store.addAll(
        List.of(
            Embedding.from(new float[]{1.0f, 0.0f, 0.0f}),
            Embedding.from(new float[]{0.9f, 0.1f, 0.0f}),
            Embedding.from(new float[]{0.0f, 0.0f, 1.0f})),
        List.of(
            TextSegment.from("Exact match"),
            TextSegment.from("Close match"),
            TextSegment.from("Far match")));

    var request = EmbeddingSearchRequest.builder()
        .queryEmbedding(Embedding.from(queryVector))
        .maxResults(2)
        .minScore(0.0)
        .build();
    Awaitility.await()
        .atMost(Duration.ofSeconds(5))
        .pollInterval(Duration.ofMillis(200))
        .until(() -> store.search(request).matches().size() >= 2);

    var result = store.search(request);
    assertThat(result.matches()).hasSize(2);
    assertThat(result.matches().get(0).embedded().text()).isEqualTo("Exact match");
  }

  @Test
  void addAllAppendsToExistingIndex() {
    var store = new OpenSearchConnector().vectorStore("it-append-index");

    store.addAll(List.of(Embedding.from(new float[]{0.1f, 0.0f})),
        List.of(TextSegment.from("First batch")));
    store.addAll(List.of(Embedding.from(new float[]{0.0f, 0.1f})),
        List.of(TextSegment.from("Second batch")));

    var request = EmbeddingSearchRequest.builder()
        .queryEmbedding(Embedding.from(new float[]{0.1f, 0.0f}))
        .maxResults(10)
        .minScore(0.0)
        .build();
    Awaitility.await()
        .atMost(Duration.ofSeconds(5))
        .pollInterval(Duration.ofMillis(200))
        .until(() -> store.search(request).matches().size() >= 2);

    var result = store.search(request);
    assertThat(result.matches()).hasSizeGreaterThanOrEqualTo(2);
  }
}
