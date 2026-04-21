package com.axonivy.utils.smart.workflow.rag.opensearch.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;

public class TestOpenSearchPayloadBuilder {

  private static final OpenSearchIndexMeta META =
      new OpenSearchIndexMeta("openai", "text-embedding-3-small", 300, 20);
  private static final int DIMENSION = 3;

  @Test
  void buildCreateIndexBody() throws Exception {
    String json = OpenSearchPayloadBuilder.buildCreateIndexBody(DIMENSION, META);
    JsonNode root = JsonUtils.getObjectMapper().readTree(json);

    assertThat(root.at("/settings/index.knn").asBoolean()).isTrue();
    assertThat(root.at("/mappings/properties/vector/type").asText())
        .isEqualTo("knn_vector");
    assertThat(root.at("/mappings/properties/vector/dimension").asInt())
        .isEqualTo(DIMENSION);
    assertThat(root.at("/mappings/properties/text/type").asText())
        .isEqualTo("text");
    assertThat(root.at("/mappings/properties/metadata/type").asText())
        .isEqualTo("object");

    JsonNode meta = root.at("/mappings/_meta");
    assertThat(meta.get("chunkSize").asInt()).isEqualTo(300);
    assertThat(meta.get("chunkOverlap").asInt()).isEqualTo(20);
    assertThat(meta.get("createdAt").asText()).isNotBlank();
    assertThat(meta.get("embeddingProvider").asText()).isEqualTo("openai");
    assertThat(meta.get("embeddingModel").asText()).isEqualTo("text-embedding-3-small");
    assertThat(meta.get("dimension").asInt()).isEqualTo(DIMENSION);
  }

  @Test
  void buildCreateIndexBodyBlankProviderWritesNullInMeta() throws Exception {
    OpenSearchIndexMeta metaNoProvider = new OpenSearchIndexMeta("", "", 300, 20);
    String json = OpenSearchPayloadBuilder.buildCreateIndexBody(DIMENSION, metaNoProvider);
    JsonNode meta = JsonUtils.getObjectMapper().readTree(json).at("/mappings/_meta");

    assertThat(meta.get("embeddingProvider").isNull()).isTrue();
    assertThat(meta.get("embeddingModel").isNull()).isTrue();
  }

  @Test
  void buildBulkNdjsonTwoEmbeddingsFourLines() {
    List<Embedding> embeddings = List.of(
        Embedding.from(new float[]{0.1f, 0.2f, 0.3f}),
        Embedding.from(new float[]{0.4f, 0.5f, 0.6f}));
    List<TextSegment> segments = List.of(
        TextSegment.from("First document"),
        TextSegment.from("Second document"));

    String ndjson = OpenSearchPayloadBuilder.buildBulkNdjson(embeddings, segments);
    String[] lines = ndjson.strip().split("\n");

    assertThat(lines).hasSize(4);
    assertThat(lines[0]).contains("\"index\"").contains("\"_id\"");
    assertThat(lines[1]).contains("\"vector\"").contains("First document");
    assertThat(lines[2]).contains("\"index\"");
    assertThat(lines[3]).contains("Second document");
  }

  @Test
  void buildBulkNdjsonIncludesSegmentMetadata() throws Exception {
    Embedding embedding = Embedding.from(new float[]{0.1f, 0.2f});
    TextSegment segment = TextSegment.from("content", Metadata.from("source", "file.pdf"));
    String ndjson = OpenSearchPayloadBuilder.buildBulkNdjson(List.of(embedding), List.of(segment));
    JsonNode doc = JsonUtils.getObjectMapper().readTree(ndjson.strip().split("\n")[1]);

    assertThat(doc.at("/metadata/source").asText()).isEqualTo("file.pdf");
  }

  @Test
  void buildSearchBodyHasKnnQuery() throws Exception {
    float[] vector = {0.1f, 0.2f, 0.3f};
    EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
        .queryEmbedding(Embedding.from(vector))
        .maxResults(5)
        .minScore(0.7)
        .build();

    String json = OpenSearchPayloadBuilder.buildSearchBody(request);
    JsonNode root = JsonUtils.getObjectMapper().readTree(json);

    assertThat(root.get("size").asInt()).isEqualTo(5);
    assertThat(root.at("/query/knn/vector/k").asInt()).isEqualTo(5);
    assertThat(root.at("/query/knn/vector/vector").get(0).floatValue()).isEqualTo(0.1f);
    assertThat(root.get("_source").toString()).contains("text").contains("metadata");
  }

  @Test
  void parseSearchResponse() throws Exception {
    String json = "{\"hits\":{\"hits\":["
        + "{\"_score\":0.9,\"_id\":\"doc1\",\"_source\":{\"text\":\"Hello\",\"metadata\":{\"source\":\"file.pdf\"}}},"
        + "{\"_score\":0.5,\"_id\":\"doc2\",\"_source\":{\"text\":\"World\",\"metadata\":{}}}"
        + "]}}";

    var result = OpenSearchPayloadBuilder.parseSearchResponse(json, 0.7);

    assertThat(result.matches()).hasSize(1);
    assertThat(result.matches().get(0).score()).isEqualTo(0.9);
    assertThat(result.matches().get(0).embedded().text()).isEqualTo("Hello");
    assertThat(result.matches().get(0).embedded().metadata().toMap()).containsEntry("source", "file.pdf");
  }

  @Test
  void parseBulkErrorNoErrorsReturnsEmpty() throws Exception {
    assertThat(OpenSearchPayloadBuilder.parseBulkError("{\"errors\":false,\"items\":[]}")).isEmpty();
  }

  @Test
  void parseBulkErrorExtractsFirstError() throws Exception {
    String json = "{\"errors\":true,\"items\":["
        + "{\"index\":{\"_id\":\"1\",\"error\":{\"type\":\"mapper_exception\",\"reason\":\"failed\"}}}"
        + "]}";

    var error = OpenSearchPayloadBuilder.parseBulkError(json);

    assertThat(error).isPresent();
    assertThat(error.get()).contains("mapper_exception");
  }

  @Test
  void buildUpdateLastIngestedBodyHasTimestamp() throws Exception {
    String json = OpenSearchPayloadBuilder.buildUpdateLastIngestedBody(META);
    assertThat(JsonUtils.getObjectMapper().readTree(json).at("/_meta/lastIngestedAt").asText())
        .isNotBlank();
  }
}
