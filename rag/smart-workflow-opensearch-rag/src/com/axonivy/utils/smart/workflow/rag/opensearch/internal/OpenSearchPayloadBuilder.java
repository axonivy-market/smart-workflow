package com.axonivy.utils.smart.workflow.rag.opensearch.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.axonivy.utils.smart.workflow.utils.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;

final class OpenSearchPayloadBuilder {

  static final String FIELD_VECTOR = "vector";
  static final String FIELD_TEXT = "text";
  static final String FIELD_METADATA = "metadata";

  private static final String FIELD_INDEX_KNN = "index.knn";
  private static final String FIELD_TYPE = "type";
  private static final String FIELD_DIMENSION = "dimension";
  private static final String FIELD_KNN_VECTOR = "knn_vector";
  private static final String FIELD_SIZE = "size";
  private static final String FIELD_QUERY = "query";
  private static final String FIELD_KNN = "knn";
  private static final String FIELD_K = "k";
  private static final String FIELD_SOURCE = "_source";
  private static final String FIELD_HITS = "hits";
  private static final String FIELD_SCORE = "_score";
  private static final String FIELD_ID = "_id";
  private static final String BULK_OP_INDEX = "index";
  private static final String FIELD_ERRORS = "errors";
  private static final String FIELD_ERROR = "error";

  private OpenSearchPayloadBuilder() {}

  public static String buildCreateIndexBody(int dimension) {
    ObjectNode root = JsonUtils.getObjectMapper().createObjectNode();
    root.putObject("settings").put(FIELD_INDEX_KNN, true);
    ObjectNode properties = root.putObject("mappings").putObject("properties");
    properties.putObject(FIELD_VECTOR).put(FIELD_TYPE, FIELD_KNN_VECTOR).put(FIELD_DIMENSION, dimension);
    properties.putObject(FIELD_TEXT).put(FIELD_TYPE, "text");
    properties.putObject(FIELD_METADATA).put(FIELD_TYPE, "object");
    return root.toString();
  }

  public static String buildBulkNdjson(List<Embedding> embeddings, List<TextSegment> segments) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < embeddings.size(); i++) {
      String id = UUID.randomUUID().toString();
      ObjectNode action = JsonUtils.getObjectMapper().createObjectNode();
      action.putObject(BULK_OP_INDEX).put(FIELD_ID, id);
      sb.append(action).append('\n');

      ObjectNode doc = JsonUtils.getObjectMapper().createObjectNode();
      ArrayNode vectorArray = doc.putArray(FIELD_VECTOR);
      for (float v : embeddings.get(i).vector()) {
        vectorArray.add(v);
      }
      doc.put(FIELD_TEXT, segments.get(i).text());
      doc.set(FIELD_METADATA, JsonUtils.getObjectMapper().valueToTree(segments.get(i).metadata().toMap()));
      sb.append(doc).append('\n');
    }
    return sb.toString();
  }

  public static String buildSearchBody(EmbeddingSearchRequest request) {
    int maxResults = request.maxResults();
    float[] vector = request.queryEmbedding().vector();

    ObjectNode root = JsonUtils.getObjectMapper().createObjectNode();
    root.put(FIELD_SIZE, maxResults);

    ObjectNode knnField = root.putObject(FIELD_QUERY).putObject(FIELD_KNN).putObject(FIELD_VECTOR);
    ArrayNode queryVector = knnField.putArray(FIELD_VECTOR);
    for (float v : vector) {
      queryVector.add(v);
    }
    knnField.put(FIELD_K, maxResults);

    ArrayNode source = root.putArray(FIELD_SOURCE);
    source.add(FIELD_TEXT);
    source.add(FIELD_METADATA);

    return root.toString();
  }

  public static EmbeddingSearchResult<TextSegment> parseSearchResponse(String json, double minScore) throws JsonProcessingException {
    JsonNode root = JsonUtils.getObjectMapper().readTree(json);
    JsonNode hits = root.path(FIELD_HITS).path(FIELD_HITS);

    List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
    for (JsonNode hit : hits) {
      double score = hit.path(FIELD_SCORE).asDouble(0.0);
      if (score < minScore) {
        continue;
      }
      String text = hit.path(FIELD_SOURCE).path(FIELD_TEXT).asText("");
      Metadata metadata = new Metadata();
      for (var field : hit.path(FIELD_SOURCE).path(FIELD_METADATA).properties()) {
        metadata.put(field.getKey(), field.getValue().asText());
      }
      TextSegment segment = TextSegment.from(text, metadata);
      String id = hit.path(FIELD_ID).asText(UUID.randomUUID().toString());
      matches.add(new EmbeddingMatch<>(score, id, null, segment));
    }
    return new EmbeddingSearchResult<>(matches);
  }

  public static Optional<String> parseBulkError(String responseBody) throws JsonProcessingException {
    JsonNode root = JsonUtils.getObjectMapper().readTree(responseBody);
    if (!root.path(FIELD_ERRORS).asBoolean(false)) {
      return Optional.empty();
    }
    return Optional.of(findFirstError(root.path("items")).toString());
  }

  private static JsonNode findFirstError(JsonNode items) {
    for (JsonNode item : items) {
      for (var entry : item.properties()) {
        if (entry.getValue().has(FIELD_ERROR)) {
          return entry.getValue().path(FIELD_ERROR);
        }
      }
    }
    return JsonUtils.getObjectMapper().nullNode();
  }
}
