package com.axonivy.utils.smart.workflow.rag.opensearch.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

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

  private interface Fields {
    String VECTOR = "vector";
    String TEXT = "text";
    String METADATA = "metadata";
    String TYPE = "type";
    String DIMENSION = "dimension";
    String KNN_VECTOR = "knn_vector";
    String INDEX_KNN = "index.knn";
    String SIZE = "size";
    String QUERY = "query";
    String KNN = "knn";
    String K = "k";
    String SOURCE = "_source";
    String HITS = "hits";
    String SCORE = "_score";
    String ID = "_id";
  }

  private interface Meta {
    String META = "_meta";
    String EMBEDDING_PROVIDER = "embeddingProvider";
    String EMBEDDING_MODEL = "embeddingModel";
    String CHUNK_SIZE = "chunkSize";
    String CHUNK_OVERLAP = "chunkOverlap";
    String CREATED_AT = "createdAt";
    String LAST_INGESTED_AT = "lastIngestedAt";
  }

  private interface Bulk {
    String OP_INDEX = "index";
    String ERRORS = "errors";
    String ERROR = "error";
  }

  private OpenSearchPayloadBuilder() {}

  public static String buildCreateIndexBody(int dimension, OpenSearchIndexMeta meta) {
    ObjectNode root = JsonUtils.getObjectMapper().createObjectNode();
    root.putObject("settings").put(Fields.INDEX_KNN, true);
    ObjectNode mappings = root.putObject("mappings");
    ObjectNode properties = mappings.putObject("properties");
    properties.putObject(Fields.VECTOR).put(Fields.TYPE, Fields.KNN_VECTOR).put(Fields.DIMENSION, dimension);
    properties.putObject(Fields.TEXT).put(Fields.TYPE, "text");
    properties.putObject(Fields.METADATA).put(Fields.TYPE, "object");
    buildMetaNode(mappings, dimension, meta);
    return root.toString();
  }

  private static void buildMetaNode(ObjectNode mappings, int dimension, OpenSearchIndexMeta meta) {
    mappings.putObject(Meta.META)
    .put(Fields.DIMENSION, dimension)
    .put(Meta.CHUNK_SIZE, meta.chunkSize())
    .put(Meta.CHUNK_OVERLAP, meta.chunkOverlap())
    .put(Meta.CREATED_AT, meta.createdAt())
    .put(Meta.EMBEDDING_PROVIDER, StringUtils.defaultIfBlank(meta.embeddingProvider(), null))
    .put(Meta.EMBEDDING_MODEL, StringUtils.defaultIfBlank(meta.embeddingModel(), null));
  }

  public static String buildUpdateLastIngestedBody(OpenSearchIndexMeta currentMeta) {
    ObjectNode root = JsonUtils.getObjectMapper().createObjectNode();
    root.putObject(Meta.META)
        .put(Meta.EMBEDDING_PROVIDER, StringUtils.defaultIfBlank(currentMeta.embeddingProvider(), null))
        .put(Meta.EMBEDDING_MODEL, StringUtils.defaultIfBlank(currentMeta.embeddingModel(), null))
        .put(Meta.CHUNK_SIZE, currentMeta.chunkSize())
        .put(Meta.CHUNK_OVERLAP, currentMeta.chunkOverlap())
        .put(Meta.CREATED_AT, currentMeta.createdAt())
        .put(Meta.LAST_INGESTED_AT, Instant.now().toString());
    return root.toString();
  }

public static String buildBulkNdjson(List<Embedding> embeddings, List<TextSegment> segments) {
  StringBuilder stringBuilder = new StringBuilder();
  for (int i = 0; i < embeddings.size(); i++) {
    Embedding embedding = embeddings.get(i);
    if (embedding.vector() == null) {
      continue;
    }
    stringBuilder.append(buildActionNode()).append('\n');
    stringBuilder.append(buildDocNode(embedding, segments.get(i))).append('\n');
  }
  return stringBuilder.toString();
}

private static ObjectNode buildActionNode() {
  ObjectNode action = JsonUtils.getObjectMapper().createObjectNode();
  action.putObject(Bulk.OP_INDEX).put(Fields.ID, UUID.randomUUID().toString());
  return action;
}

private static ObjectNode buildDocNode(Embedding embedding, TextSegment segment) {
  ObjectNode doc = JsonUtils.getObjectMapper().createObjectNode();
  ArrayNode vectorArray = doc.putArray(Fields.VECTOR);
  for (float v : embedding.vector()) {
    vectorArray.add(v);
  }
  doc.put(Fields.TEXT, segment.text());
  doc.set(Fields.METADATA, JsonUtils.getObjectMapper().valueToTree(segment.metadata().toMap()));
  return doc;
}

  public static String buildSearchBody(EmbeddingSearchRequest request) {
    int maxResults = request.maxResults();
    float[] vector = request.queryEmbedding().vector();

    ObjectNode root = JsonUtils.getObjectMapper().createObjectNode();
    root.put(Fields.SIZE, maxResults);

    ObjectNode knnField = root.putObject(Fields.QUERY).putObject(Fields.KNN).putObject(Fields.VECTOR);
    ArrayNode queryVector = knnField.putArray(Fields.VECTOR);
    for (float v : vector) {
      queryVector.add(v);
    }
    knnField.put(Fields.K, maxResults);

    ArrayNode source = root.putArray(Fields.SOURCE);
    source.add(Fields.TEXT);
    source.add(Fields.METADATA);

    return root.toString();
  }

  public static EmbeddingSearchResult<TextSegment> parseSearchResponse(String json, double minScore) throws JsonProcessingException {
    JsonNode root = JsonUtils.getObjectMapper().readTree(json);
    JsonNode hits = root.path(Fields.HITS).path(Fields.HITS);

    List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
    for (JsonNode hit : hits) {
      double score = hit.path(Fields.SCORE).asDouble(0.0);
      if (score < minScore) {
        continue;
      }
      String text = hit.path(Fields.SOURCE).path(Fields.TEXT).asText("");
      Metadata metadata = new Metadata();
      for (var field : hit.path(Fields.SOURCE).path(Fields.METADATA).properties()) {
        metadata.put(field.getKey(), field.getValue().asText());
      }
      TextSegment segment = TextSegment.from(text, metadata);
      String id = hit.path(Fields.ID).asText(UUID.randomUUID().toString());
      matches.add(new EmbeddingMatch<>(score, id, null, segment));
    }
    return new EmbeddingSearchResult<>(matches);
  }

  public static OpenSearchIndexMeta parseIndexMeta(String json) throws JsonProcessingException {
    JsonNode root = JsonUtils.getObjectMapper().readTree(json);
    // Response shape: { "<indexName>": { "mappings": { "_meta": { ... } } } }
    JsonNode meta = root.elements().next().path("mappings").path(Meta.META);
    return new OpenSearchIndexMeta(
        meta.path(Meta.EMBEDDING_PROVIDER).asText(null),
        meta.path(Meta.EMBEDDING_MODEL).asText(null),
        meta.path(Meta.CHUNK_SIZE).asInt(0),
        meta.path(Meta.CHUNK_OVERLAP).asInt(0),
        meta.path(Meta.CREATED_AT).asText(null),
        meta.path(Meta.LAST_INGESTED_AT).asText(null));
  }

  public static String buildListDocumentsBody(int size) {
    ObjectNode root = JsonUtils.getObjectMapper().createObjectNode();
    root.put(Fields.SIZE, size);
    root.putObject(Fields.QUERY).putObject("match_all");
    ArrayNode source = root.putArray(Fields.SOURCE);
    source.add(Fields.TEXT);
    source.add(Fields.METADATA);
    return root.toString();
  }

  public static Optional<String> parseBulkError(String responseBody) throws JsonProcessingException {
    JsonNode root = JsonUtils.getObjectMapper().readTree(responseBody);
    if (!root.path(Bulk.ERRORS).asBoolean(false)) {
      return Optional.empty();
    }
    return Optional.of(findFirstError(root.path("items")).toString());
  }

  private static JsonNode findFirstError(JsonNode items) {
    for (JsonNode item : items) {
      for (var entry : item.properties()) {
        if (entry.getValue().has(Bulk.ERROR)) {
          return entry.getValue().path(Bulk.ERROR);
        }
      }
    }
    return JsonUtils.getObjectMapper().nullNode();
  }
}
