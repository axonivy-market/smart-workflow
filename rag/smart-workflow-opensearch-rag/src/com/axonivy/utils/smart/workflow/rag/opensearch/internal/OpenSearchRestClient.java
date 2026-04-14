package com.axonivy.utils.smart.workflow.rag.opensearch.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.rag.pipeline.internal.OpenSearchConf;
import com.fasterxml.jackson.core.JsonProcessingException;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;

public class OpenSearchRestClient {

  private static final String OPENSEARCH_CLIENT = "openSearch";
  private static final String NDJSON = "application/x-ndjson";

  private interface Paths {
    String BULK = "_bulk";
    String MAPPING = "_mapping";
    String SEARCH = "_search";
  }

  private interface Errors {
    String BULK_PARTIAL = "OpenSearch bulk ingest had errors: %s";
    String PING_FAILED = "OpenSearch ping failed with status: %d";
    String CREATE_INDEX = "OpenSearch index creation failed for '%s' with status: %d — %s";
  }

  private final WebTarget target;

  public static OpenSearchRestClient fromIvyVars() {
    String apiKey = Ivy.var().get(OpenSearchConf.API_KEY);
    String userName = Ivy.var().get(OpenSearchConf.USER_NAME);
    String password = Ivy.var().get(OpenSearchConf.PASSWORD);
    WebTarget base = Ivy.rest().client(OPENSEARCH_CLIENT)
      .register(new AuthFilter(apiKey, userName, password));
    return new OpenSearchRestClient(base);
  }

  private OpenSearchRestClient(WebTarget target) {
    this.target = target;
  }

  public void ping() {
    try (Response response = target.request(MediaType.APPLICATION_JSON).head()) {
      if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
        throw new IllegalStateException(String.format(Errors.PING_FAILED, response.getStatus()));
      }
    }
  }

  public boolean indexExists(String indexName) {
    try (Response response = target.path(indexName).request(MediaType.APPLICATION_JSON).head()) {
      return response.getStatusInfo() == Response.Status.OK;
    }
  }

  public void createIndex(String indexName, int dimension, OpenSearchIndexMeta meta) {
    String body = OpenSearchPayloadBuilder.buildCreateIndexBody(dimension, meta);
    try (Response response = target.path(indexName).request(MediaType.APPLICATION_JSON)
        .put(Entity.json(body))) {
      if (response.getStatusInfo() == Response.Status.BAD_REQUEST) {
        // resource_already_exists_exception — index was created concurrently, safe to ignore
        return;
      }
      if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
        String responseBody = response.readEntity(String.class);
        throw new IllegalStateException(String.format(Errors.CREATE_INDEX, indexName, response.getStatus(), responseBody));
      }
    }
  }

  public void bulkIngest(String indexName, List<Embedding> embeddings, List<TextSegment> segments) {
    String ndjson = OpenSearchPayloadBuilder.buildBulkNdjson(embeddings, segments);
    try (Response response = target.path(indexName).path(Paths.BULK)
        .request(MediaType.APPLICATION_JSON)
        .post(Entity.entity(ndjson, NDJSON))) {
      String responseBody = response.readEntity(String.class);
      try {
        OpenSearchPayloadBuilder.parseBulkError(responseBody)
            .ifPresent(error -> Ivy.log().warn(String.format(Errors.BULK_PARTIAL, error)));
      } catch (JsonProcessingException ex) {
        Ivy.log().warn("Could not parse OpenSearch bulk response", ex);
      }
    }
    target.path(indexName).path(Paths.MAPPING)
        .request(MediaType.APPLICATION_JSON).put(Entity.json(OpenSearchPayloadBuilder.buildUpdateLastIngestedBody())).close();
  }

  public EmbeddingSearchResult<TextSegment> search(String indexName, EmbeddingSearchRequest request) {
    String body = OpenSearchPayloadBuilder.buildSearchBody(request);
    try (Response response = target.path(indexName).path(Paths.SEARCH)
        .request(MediaType.APPLICATION_JSON)
        .post(Entity.json(body))) {
      String responseBody = response.readEntity(String.class);
      try {
        return OpenSearchPayloadBuilder.parseSearchResponse(responseBody, request.minScore());
      } catch (JsonProcessingException ex) {
        throw new IllegalStateException("Failed to parse OpenSearch search response", ex);
      }
    }
  }

  private static final class AuthFilter implements ClientRequestFilter {

    private static final String AUTH_KEY = "Authorization";
    private static final String API_KEY_FORMAT = "ApiKey %s";
    private static final String BASIC_FORMAT = "Basic %s";

    private final String apiKey;
    private final String userName;
    private final String password;

    AuthFilter(String apiKey, String userName, String password) {
      this.apiKey = apiKey;
      this.userName = userName;
      this.password = password;
    }

    @Override
    public void filter(ClientRequestContext context) throws IOException {
      boolean hasApiKey = StringUtils.isNotBlank(apiKey);
      boolean hasBasicAuth = StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(password);

      if (hasApiKey) {
        addApiKeyHeader(context);
        return;
      }
      if (hasBasicAuth) {
        addBasicAuthHeader(context);
      }
    }

    private void addApiKeyHeader(ClientRequestContext context) {
      context.getHeaders().putSingle(AUTH_KEY, String.format(API_KEY_FORMAT, apiKey));
    }

    private void addBasicAuthHeader(ClientRequestContext context) {
      String raw = userName + ":" + password;
      String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
      context.getHeaders().putSingle(AUTH_KEY, String.format(BASIC_FORMAT, encoded));
    }

  }
}
