package com.axonivy.utils.smart.workflow.demo.mock;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.axonivy.utils.smart.workflow.rag.opensearch.internal.OpenSearchAuthFilter;
import com.axonivy.utils.smart.workflow.rag.pipeline.internal.OpenSearchConf;

import ch.ivyteam.ivy.environment.Ivy;

/**
 * Minimal REST utility for OpenSearch index management used by {@code StartDemoBean}.
 *
 * WARNING: this implementation is only intended for demonstration purposes, never use in production!
 * It lacks error handling, logging, and other best practices.
 */
public class VectorStoreRestClient {

  private static final String OPENSEARCH_CLIENT = "openSearch";

  private VectorStoreRestClient() {}

  public static void deleteIndex(String indexName) {
    String apiKey   = Ivy.var().get(OpenSearchConf.API_KEY);
    String userName = Ivy.var().get(OpenSearchConf.USER_NAME);
    String password = Ivy.var().get(OpenSearchConf.PASSWORD);
    WebTarget target = Ivy.rest().client(OPENSEARCH_CLIENT)
        .register(new OpenSearchAuthFilter(apiKey, userName, password));
    try (Response response = target.path(indexName).request(MediaType.APPLICATION_JSON).delete()) {
      int status = response.getStatus();
      if (status == Response.Status.NOT_FOUND.getStatusCode()) {
        return;
      }
      if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
        String body = response.readEntity(String.class);
        throw new IllegalStateException(
            "OpenSearch deleteIndex failed for '" + indexName + "' with status: " + status + " — " + body);
      }
    }
  }
}
