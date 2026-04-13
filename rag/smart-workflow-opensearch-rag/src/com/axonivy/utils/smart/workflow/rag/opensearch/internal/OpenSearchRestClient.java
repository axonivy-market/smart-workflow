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
  private static final String PATH_BULK = "_bulk";
  private static final String PATH_SEARCH = "_search";
  private static final int HTTP_OK = 200;
  private static final int HTTP_SUCCESS_MAX = 300;
  private static final int HTTP_BAD_REQUEST = 400;
  private static final String ERR_BULK_PARTIAL = "OpenSearch bulk ingest had errors: %s";
  private static final String ERR_PING_FAILED = "OpenSearch ping failed with status: %d";
  private static final String ERR_CREATE_INDEX = "OpenSearch index creation failed for '%s' with status: %d — %s";

  private final WebTarget target;

  public static OpenSearchRestClient fromIvyVars() {
    String apiKey = Ivy.var().get(OpenSearchConf.API_KEY);
    String userName = Ivy.var().get(OpenSearchConf.USER_NAME);
    String password = Ivy.var().get(OpenSearchConf.PASSWORD);
    WebTarget base = Ivy.rest().client(OPENSEARCH_CLIENT);
    base.register(new AuthFilter(apiKey, userName, password));
    return new OpenSearchRestClient(base);
  }

  private OpenSearchRestClient(WebTarget target) {
    this.target = target;
  }

  public void ping() {
    Response response = target.request(MediaType.APPLICATION_JSON).head();
    int status = response.getStatus();
    if (status < HTTP_OK || status >= HTTP_SUCCESS_MAX) {
      throw new IllegalStateException(String.format(ERR_PING_FAILED, status));
    }
  }

  public boolean indexExists(String indexName) {
    Response response = target.path(indexName).request(MediaType.APPLICATION_JSON).head();
    return response.getStatus() == HTTP_OK;
  }

  public void createIndex(String indexName, int dimension) {
    String body = OpenSearchPayloadBuilder.buildCreateIndexBody(dimension);
    Response response = target.path(indexName).request(MediaType.APPLICATION_JSON)
        .put(Entity.json(body));
    int status = response.getStatus();
    if (status == HTTP_BAD_REQUEST) {
      // resource_already_exists_exception — index was created concurrently, safe to ignore
      return;
    }
    if (status < HTTP_OK || status >= HTTP_SUCCESS_MAX) {
      String responseBody = response.readEntity(String.class);
      throw new IllegalStateException(String.format(ERR_CREATE_INDEX, indexName, status, responseBody));
    }
  }

  public void bulkIngest(String indexName, List<Embedding> embeddings, List<TextSegment> segments) {
    String ndjson = OpenSearchPayloadBuilder.buildBulkNdjson(embeddings, segments);
    Response response = target.path(indexName).path(PATH_BULK)
        .request(MediaType.APPLICATION_JSON)
        .post(Entity.entity(ndjson, NDJSON));
    String responseBody = response.readEntity(String.class);
    try {
      OpenSearchPayloadBuilder.parseBulkError(responseBody)
          .ifPresent(error -> Ivy.log().warn(String.format(ERR_BULK_PARTIAL, error)));
    } catch (JsonProcessingException ex) {
      Ivy.log().warn("Could not parse OpenSearch bulk response", ex);
    }
  }

  public EmbeddingSearchResult<TextSegment> search(String indexName, EmbeddingSearchRequest request) {
    String body = OpenSearchPayloadBuilder.buildSearchBody(request);
    Response response = target.path(indexName).path(PATH_SEARCH)
        .request(MediaType.APPLICATION_JSON)
        .post(Entity.json(body));
    String responseBody = response.readEntity(String.class);
    try {
      return OpenSearchPayloadBuilder.parseSearchResponse(responseBody, request.minScore());
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to parse OpenSearch search response", ex);
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
