package com.axonivy.utils.smart.workflow.rag.pipeline.internal;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

import com.axonivy.utils.smart.workflow.rag.pipeline.RagConnector;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.store.embedding.opensearch.OpenSearchEmbeddingStore;

public class OpenSearchConnector implements RagConnector {

  private static final String HEADER_AUTHORIZATION = "Authorization";
  private static final String API_KEY_HEADER_VALUE = "ApiKey %s";
  private static final String ERR_URL_NOT_CONFIGURED = "AI.RAG.OpenSearch.Url is not configured.";
  private static final String ERR_CONNECT = "Cannot connect to OpenSearch at: %s \u2014 %s";
  private static final String WARN_INDEX_EXISTS = "Could not check OpenSearch index existence for: %s";
  private static final String INFO_FALLBACK_COLLECTION = "No OpenSearch index configured \u2014 using fallback: %s";
  private static final String ERR_CLOSE_CLIENT = "Failed to close OpenSearch client";
  private static final String ERR_INVALID_URL = "Invalid OpenSearch URL: %s";

  @Override
  public boolean indexExists(String collection) {
    String url = Ivy.var().get(OpenSearchConf.URL);
    String indexName = StringUtils.defaultIfBlank(collection, Ivy.var().get(OpenSearchConf.DEFAULT_COLLECTION));
    if (StringUtils.isAnyBlank(url, indexName)) {
      return false;
    }
    OpenSearchClient client = null;
    try {
      client = buildClient(url,
          Ivy.var().get(OpenSearchConf.API_KEY),
          Ivy.var().get(OpenSearchConf.USER_NAME),
          Ivy.var().get(OpenSearchConf.PASSWORD));
      return client.indices().exists(req -> req.index(indexName)).value();
    } catch (IOException | IllegalArgumentException ex) {
      Ivy.log().warn(String.format(WARN_INDEX_EXISTS, indexName), ex);
      return false;
    } finally {
      closeClient(client);
    }
  }

  @Override
  public Connection connect(String collection) {
    String url = Ivy.var().get(OpenSearchConf.URL);
    if (StringUtils.isBlank(url)) {
      return new Connection(null, ERR_URL_NOT_CONFIGURED);
    }

    OpenSearchClient client;
    try {
      client = buildClient(url);
    } catch (IOException ex) {
      return new Connection(null, String.format(ERR_CONNECT, url, ex.getMessage()));
    }

    String indexName = resolveIndexName(collection);
    OpenSearchEmbeddingStore store = OpenSearchEmbeddingStore.builder()
        .openSearchClient(client)
        .indexName(indexName)
        .build();
    return new Connection(new OpenSearchVectorStore(store, client), null);
  }

  private static void closeClient(OpenSearchClient client) {
    if (client == null) {
      return;
    }
    try {
      client._transport().close();
    } catch (IOException ex) {
      Ivy.log().warn(ERR_CLOSE_CLIENT, ex);
    }
  }

  private OpenSearchClient buildClient(String url) throws IOException {
      OpenSearchClient client = buildClient(url,
          Ivy.var().get(OpenSearchConf.API_KEY),
          Ivy.var().get(OpenSearchConf.USER_NAME),
          Ivy.var().get(OpenSearchConf.PASSWORD));
      client.ping();
      return client;
  }

  private String resolveIndexName(String collection) {
    String indexName = StringUtils.defaultIfBlank(collection, Ivy.var().get(OpenSearchConf.DEFAULT_COLLECTION));
    if (StringUtils.isBlank(indexName)) {
      indexName = OpenSearchConf.FALLBACK_COLLECTION;
      Ivy.log().info(String.format(INFO_FALLBACK_COLLECTION, indexName));
    }
    return indexName;
  }

  private OpenSearchClient buildClient(String serverUrl, String apiKey, String userName, String password) {
    HttpHost host;
    try {
      host = HttpHost.create(serverUrl);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(String.format(ERR_INVALID_URL, serverUrl), e);
    }

    var transport = ApacheHttpClient5TransportBuilder.builder(host)
        .setMapper(new JacksonJsonpMapper())
        .setHttpClientConfigCallback(httpClientBuilder -> {
          configureApiKeyAuth(httpClientBuilder, apiKey);
          configureBasicAuth(httpClientBuilder, host, userName, password);
          httpClientBuilder.setConnectionManager(
              PoolingAsyncClientConnectionManagerBuilder.create().build());
          return httpClientBuilder;
        })
        .build();

    return new OpenSearchClient(transport);
  }

  private static void configureApiKeyAuth(HttpAsyncClientBuilder builder, String apiKey) {
    if (StringUtils.isNotBlank(apiKey)) {
      builder.setDefaultHeaders(Collections.singletonList(
          new BasicHeader(HEADER_AUTHORIZATION, String.format(API_KEY_HEADER_VALUE, apiKey))));
    }
  }

  private static void configureBasicAuth(HttpAsyncClientBuilder builder,
      HttpHost host, String userName, String password) {
    if (StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(password)) {
      var credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(new AuthScope(host),
          new UsernamePasswordCredentials(userName, password.toCharArray()));
      builder.setDefaultCredentialsProvider(credentialsProvider);
    }
  }
}
