package com.axonivy.utils.smart.workflow.model.azureopenai.internal.client;

import javax.ws.rs.client.WebTarget;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpClientProvider;
import com.azure.core.util.HttpClientOptions;

import ch.ivyteam.ivy.environment.Ivy;

/**
 * Supplies a {@link SmartAzureHttpClient} backed by the shared {@code langChain} Ivy
 * REST client, so Azure-OpenAI requests are traced and logged like every other provider.
 */
public class SmartAzureHttpClientProvider implements HttpClientProvider {

  private static final String LANG_CHAIN_CLIENT = "langChain";
  private static final String CONNECT_TIMEOUT = "jersey.config.client.connectTimeout";
  private static final String READ_TIMEOUT = "jersey.config.client.readTimeout";

  @Override
  public HttpClient createInstance() {
    return new SmartAzureHttpClient(client());
  }

  /**
   * LangChain4j invokes this overload, passing the timeouts it resolved (60s by default).
   * The {@code langChain} rest-clients.yaml sets none, so we wire them onto the Jersey
   * target here - mirroring the core {@code SmartHttpClientBuilder} - to make sure a
   * stalled Azure call cannot pin an Ivy worker thread indefinitely.
   */
  @Override
  public HttpClient createInstance(HttpClientOptions clientOptions) {
    var target = client();
    if (clientOptions != null) {
      target.property(CONNECT_TIMEOUT, clientOptions.getConnectTimeout().toMillis());
      target.property(READ_TIMEOUT, clientOptions.getReadTimeout().toMillis());
    }
    return new SmartAzureHttpClient(target);
  }

  private static WebTarget client() {
    return Ivy.rest().client(LANG_CHAIN_CLIENT);
  }
}
