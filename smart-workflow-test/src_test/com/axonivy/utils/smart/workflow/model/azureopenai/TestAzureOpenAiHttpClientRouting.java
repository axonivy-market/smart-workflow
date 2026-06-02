package com.axonivy.utils.smart.workflow.model.azureopenai;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URL;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.model.azureopenai.internal.client.SmartAzureHttpClient;

import com.azure.core.http.HttpHeaderName;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.util.Context;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.test.RestResourceTest;

/**
 * Drives a real request through {@link SmartAzureHttpClient} against the in-engine
 * {@link MockOpenAI} resource, verifying that Azure-OpenAI traffic is routed through
 * Ivy's REST client (the source of engine-cockpit tracing) and that the Azure
 * {@code HttpClient} error contract (return, don't throw) is honoured.
 */
@RestResourceTest
public class TestAzureOpenAiHttpClientRouting {

  @Test
  void request_routesThroughIvyRestClient() throws Exception {
    MockOpenAI.defineChat(req -> Response.ok().entity("{\"ok\":true}").build());

    HttpResponse response = send("{\"hello\":\"world\"}");

    assertThat(response.getStatusCode()).isEqualTo(200);
    assertThat(response.getBodyAsString().block()).contains("\"ok\":true");
  }

  @Test
  void errorStatus_isReturnedNotThrown() throws Exception {
    // Azure's pipeline (retry policy + exception mapper) inspects the status code,
    // so the bridge must surface non-2xx responses instead of throwing.
    MockOpenAI.defineChat(req -> Response.status(429).entity("{\"error\":\"rate limited\"}").build());

    HttpResponse response = send("{\"hello\":\"world\"}");

    assertThat(response.getStatusCode()).isEqualTo(429);
    assertThat(response.getBodyAsString().block()).contains("rate limited");
  }

  private static HttpResponse send(String body) throws Exception {
    var client = new SmartAzureHttpClient(Ivy.rest().client("mockClient"));
    URL url = URI.create(OpenAiTestClient.localMockApiUrl("azureTest") + "/chat/completions").toURL();
    var request = new HttpRequest(HttpMethod.POST, url)
        .setHeader(HttpHeaderName.CONTENT_TYPE, "application/json")
        // Ivy's REST API rejects POSTs without this CSRF header; real Azure endpoints don't need it.
        .setHeader(HttpHeaderName.fromString("X-Requested-By"), "ivy")
        .setBody(body);
    return client.sendSync(request, Context.NONE);
  }
}
