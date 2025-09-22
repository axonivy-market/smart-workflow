package com.axonivy.utils.smart.workflow.client;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response.Status.Family;

import dev.langchain4j.exception.HttpException;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.SuccessfulHttpResponse;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import dev.langchain4j.http.client.sse.ServerSentEventParser;

public class SmartHttpClient implements HttpClient {

  private final WebTarget target;

  public SmartHttpClient(WebTarget target) {
    this.target = target;
  }

  @SuppressWarnings("unchecked")
  @Override
  public SuccessfulHttpResponse execute(HttpRequest request) throws HttpException, RuntimeException {
    var headers = new MultivaluedHashMap<String, Object>();
    headers.putAll((Map<? extends String, ? extends List<Object>>) request.headers());
    var content = contentType(headers);

    target.register(new UriFilter(request), Priorities.AUTHENTICATION);
    var response = target
        .request()
        .headers(headers)
        .method(request.method().name(), Entity.entity(request.body(), content));

    if (Family.SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
      return SuccessfulHttpResponse.builder()
          .statusCode(response.getStatus())
          .body(response.readEntity(String.class))
          .headers(response.getStringHeaders())
          .build();
    }

    throw new HttpException(response.getStatus(), response.readEntity(String.class));
  }

  private static String contentType(MultivaluedHashMap<String, Object> headers) {
    List<Object> ct = headers.remove("Content-Type");
    return Optional.ofNullable(ct)
        .map(List::getFirst)
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .orElse(MediaType.APPLICATION_JSON);
  }

  @Override
  public void execute(HttpRequest request, ServerSentEventParser parser, ServerSentEventListener listener) {
    throw new RuntimeException("not implemented");
  }

  /**
   * Override default resolved URI from rest-clients.yaml configuration by LangChain URI.
   */
  private static class UriFilter implements ClientRequestFilter {

    private final HttpRequest request;

    private UriFilter(HttpRequest request) {
      this.request = request;
    }

    @Override
    public void filter(ClientRequestContext context) throws IOException {
      context.setUri(URI.create(request.url()));
    }
  }

}
