package com.axonivy.utils.smart.workflow.model.azureopenai.internal.client;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpHeader;
import com.azure.core.http.HttpHeaderName;
import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Bridges Azure's {@link HttpClient} SPI onto Ivy's Jersey REST client, so that
 * Azure-OpenAI calls flow through the same request/response logging and slow-call
 * tracing (engine-cockpit) that every other provider gets via {@code SmartHttpClient}.
 * <p>
 * Unlike LangChain4j's {@code HttpClient} contract (which expects a thrown
 * {@code HttpException} on error), Azure's {@link HttpClient} sits at the bottom of
 * Azure's {@code HttpPipeline}: its retry policy and exception mapper inspect the
 * returned response's status code. We therefore return the response for <em>every</em>
 * status code and let the Azure pipeline decide on retries and error mapping.
 */
public class SmartAzureHttpClient implements HttpClient {

  private static final String CONTENT_TYPE = "Content-Type";
  // Length/encoding are derived by Jersey/Apache from the entity; forwarding Azure's
  // own Content-Length triggers "Content-Length header already present".
  private static final String CONTENT_LENGTH = "Content-Length";

  private final WebTarget target;

  public SmartAzureHttpClient(WebTarget target) {
    this.target = target;
  }

  /**
   * The reactive entry point. We deliberately do not apply {@code subscribeOn}/
   * {@code publishOn}: {@code Mono.fromCallable} runs on the subscribing thread, which
   * for the synchronous {@code OpenAIClient} is the Ivy request thread. Keeping that
   * thread is what preserves the Ivy request context the tracing relies on.
   */
  @Override
  public Mono<HttpResponse> send(HttpRequest request) {
    return Mono.fromCallable(() -> sendSync(request, Context.NONE));
  }

  @Override
  public HttpResponse sendSync(HttpRequest request, Context context) {
    var headers = new MultivaluedHashMap<String, Object>();
    String contentType = MediaType.APPLICATION_JSON;
    for (HttpHeader header : request.getHeaders()) {
      if (CONTENT_TYPE.equalsIgnoreCase(header.getName())) {
        contentType = header.getValue();
        continue;
      }
      if (CONTENT_LENGTH.equalsIgnoreCase(header.getName())) {
        continue;
      }
      for (String value : header.getValuesList()) {
        headers.add(header.getName(), value);
      }
    }

    BinaryData body = request.getBodyAsBinaryData();
    Entity<String> entity = body == null ? null : Entity.entity(body.toString(), contentType);

    target.register(new UriFilter(request.getUrl().toString()), Priorities.AUTHENTICATION);
    Response response = target
        .request()
        .headers(headers)
        .method(request.getHttpMethod().name(), entity);

    return new JerseyHttpResponse(request, response);
  }

  /**
   * Overrides the URI resolved from rest-clients.yaml with the absolute Azure endpoint
   * URL of the current request.
   */
  private static final class UriFilter implements ClientRequestFilter {

    private final String url;

    private UriFilter(String url) {
      this.url = url;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
      requestContext.setUri(URI.create(url));
    }
  }

  /**
   * Buffers the Jersey response into an Azure {@link HttpResponse}. The body is read
   * eagerly so the response can be consumed by Azure's pipeline regardless of status.
   */
  private static final class JerseyHttpResponse extends HttpResponse {

    private final int statusCode;
    private final HttpHeaders headers;
    private final byte[] body;

    private JerseyHttpResponse(HttpRequest request, Response response) {
      super(request);
      try {
        this.statusCode = response.getStatus();
        this.headers = toAzureHeaders(response);
        this.body = response.hasEntity() ? response.readEntity(byte[].class) : new byte[0];
      } finally {
        response.close();
      }
    }

    private static HttpHeaders toAzureHeaders(Response response) {
      var azureHeaders = new HttpHeaders();
      response.getStringHeaders().forEach((name, values) -> {
        var headerName = HttpHeaderName.fromString(name);
        values.forEach(value -> azureHeaders.add(headerName, value));
      });
      return azureHeaders;
    }

    @Override
    public int getStatusCode() {
      return statusCode;
    }

    @Override
    public String getHeaderValue(String name) {
      return headers.getValue(HttpHeaderName.fromString(name));
    }

    @Override
    public HttpHeaders getHeaders() {
      return headers;
    }

    @Override
    public Flux<ByteBuffer> getBody() {
      return Flux.defer(() -> Flux.just(ByteBuffer.wrap(body)));
    }

    @Override
    public Mono<byte[]> getBodyAsByteArray() {
      return Mono.just(body);
    }

    @Override
    public Mono<String> getBodyAsString() {
      return getBodyAsString(StandardCharsets.UTF_8);
    }

    @Override
    public Mono<String> getBodyAsString(Charset charset) {
      return Mono.just(new String(body, charset));
    }
  }
}
