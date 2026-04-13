package com.axonivy.utils.smart.workflow.rag.pipeline.internal;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.rag.opensearch.internal.OpenSearchRestClient;
import com.axonivy.utils.smart.workflow.rag.pipeline.RagConnector;
import com.axonivy.utils.smart.workflow.rag.pipeline.RagVectorStore;

import ch.ivyteam.ivy.environment.Ivy;

public class OpenSearchConnector implements RagConnector {

  private static final String ERR_URL_NOT_CONFIGURED = "AI.RAG.OpenSearch.Url is not configured.";
  private static final String ERR_COLLECTION_REQUIRED = "collection is required";
  private static final String WARN_INDEX_EXISTS = "Could not check OpenSearch index existence for: %s";

  @Override
  public boolean indexExists(String collection) {
    String url = Ivy.var().get(OpenSearchConf.URL);
    if (StringUtils.isAnyBlank(url, collection)) {
      return false;
    }
    try {
      return OpenSearchRestClient.fromIvyVars().indexExists(collection);
    } catch (Exception ex) {
      Ivy.log().warn(String.format(WARN_INDEX_EXISTS, collection), ex);
      return false;
    }
  }

  @Override
  public RagVectorStore connect(String collection) {
    if (StringUtils.isBlank(collection)) {
      throw new IllegalArgumentException(ERR_COLLECTION_REQUIRED);
    }
    String url = Ivy.var().get(OpenSearchConf.URL);
    if (StringUtils.isBlank(url)) {
      throw new IllegalStateException(ERR_URL_NOT_CONFIGURED);
    }
    OpenSearchRestClient client = OpenSearchRestClient.fromIvyVars();
    client.ping();
    return new OpenSearchVectorStore(client, collection);
  }
}
