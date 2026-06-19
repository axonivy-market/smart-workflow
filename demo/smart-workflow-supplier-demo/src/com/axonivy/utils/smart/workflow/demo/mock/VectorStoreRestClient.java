package com.axonivy.utils.smart.workflow.demo.mock;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.axonivy.utils.smart.workflow.rag.opensearch.internal.OpenSearchRestClient;

/**
 * Minimal REST utility for OpenSearch index management used by {@code StartDemoBean}.
 *
 * WARNING: this implementation is only intended for demonstration purposes, never use in production!
 * It lacks error handling, logging, and other best practices.
 */
public class VectorStoreRestClient {

  private VectorStoreRestClient() {}

  public static void deleteIndex(String indexName) {
    try (Response response = OpenSearchRestClient.fromIvyVars().target()
        .path(indexName).request(MediaType.APPLICATION_JSON).delete()) {
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
