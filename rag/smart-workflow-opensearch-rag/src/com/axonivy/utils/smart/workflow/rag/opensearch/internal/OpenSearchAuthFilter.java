package com.axonivy.utils.smart.workflow.rag.opensearch.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import org.apache.commons.lang3.StringUtils;

public class OpenSearchAuthFilter implements ClientRequestFilter {

  private static final String AUTH_KEY = "Authorization";
  private static final String API_KEY_FORMAT = "ApiKey %s";
  private static final String BASIC_FORMAT = "Basic %s";

  private final String apiKey;
  private final String userName;
  private final String password;

  public OpenSearchAuthFilter(String apiKey, String userName, String password) {
    this.apiKey    = apiKey;
    this.userName  = userName;
    this.password  = password;
  }

  @Override
  public void filter(ClientRequestContext context) throws IOException {
    boolean hasApiKey    = StringUtils.isNotBlank(apiKey);
    boolean hasBasicAuth = StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(password);

    if (hasApiKey) {
      context.getHeaders().putSingle(AUTH_KEY, String.format(API_KEY_FORMAT, apiKey));
      return;
    }
    if (hasBasicAuth) {
      String raw = userName + ":" + password;
      String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
      context.getHeaders().putSingle(AUTH_KEY, String.format(BASIC_FORMAT, encoded));
    }
  }
}
