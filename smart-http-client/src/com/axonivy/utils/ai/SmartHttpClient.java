package com.axonivy.utils.ai;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
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

  private final Client jersey;

  public SmartHttpClient(Client client) {
    this.jersey = client;
  }

  @SuppressWarnings("unchecked")
  @Override
  public SuccessfulHttpResponse execute(HttpRequest request) throws HttpException, RuntimeException {
    request.method();

    var headers = new MultivaluedHashMap<String, Object>();
    headers.putAll((Map<? extends String, ? extends List<Object>>) request.headers());
    List<Object> ct = headers.remove("Content-Type");
    var content = Optional.ofNullable(ct).map(List::getFirst)
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .orElse(MediaType.APPLICATION_JSON);

    var response = jersey.target(request.url())
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

  @Override
  public void execute(HttpRequest request, ServerSentEventParser parser, ServerSentEventListener listener) {
    throw new RuntimeException("not implemented");
  }

}
