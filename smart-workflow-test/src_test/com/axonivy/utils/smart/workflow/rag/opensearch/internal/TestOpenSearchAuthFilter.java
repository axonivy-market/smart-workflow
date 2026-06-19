package com.axonivy.utils.smart.workflow.rag.opensearch.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

public class TestOpenSearchAuthFilter {

  @Test
  void filter_withApiKey_setsApiKeyHeader() throws IOException {
    var context = new StubContext();
    new OpenSearchAuthFilter("my-api-key", null, null).filter(context);
    assertThat(context.getHeaders().getFirst("Authorization")).isEqualTo("ApiKey my-api-key");
  }

  @Test
  void filter_withBasicAuth_setsBasicHeader() throws IOException {
    var context = new StubContext();
    new OpenSearchAuthFilter(null, "user", "pass").filter(context);
    String expected = "Basic " + Base64.getEncoder().encodeToString("user:pass".getBytes(StandardCharsets.UTF_8));
    assertThat(context.getHeaders().getFirst("Authorization")).isEqualTo(expected);
  }

  @Test
  void filter_withApiKeyAndBasicAuth_prefersApiKey() throws IOException {
    var context = new StubContext();
    new OpenSearchAuthFilter("key", "user", "pass").filter(context);
    assertThat((String) context.getHeaders().getFirst("Authorization")).startsWith("ApiKey ");
  }

  @Test
  void filter_withNoCredentials_setsNoHeader() throws IOException {
    var context = new StubContext();
    new OpenSearchAuthFilter(null, null, null).filter(context);
    assertThat(context.getHeaders()).doesNotContainKey("Authorization");
  }

  private static class StubContext implements ClientRequestContext {
    private final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

    @Override public MultivaluedMap<String, Object> getHeaders() { return headers; }
    @Override public Object getProperty(String n) { return null; }
    @Override public Collection<String> getPropertyNames() { return null; }
    @Override public void setProperty(String n, Object v) {}
    @Override public void removeProperty(String n) {}
    @Override public URI getUri() { return null; }
    @Override public void setUri(URI u) {}
    @Override public String getMethod() { return null; }
    @Override public void setMethod(String m) {}
    @Override public MultivaluedMap<String, String> getStringHeaders() { return null; }
    @Override public String getHeaderString(String n) { return null; }
    @Override public Date getDate() { return null; }
    @Override public Locale getLanguage() { return null; }
    @Override public MediaType getMediaType() { return null; }
    @Override public List<MediaType> getAcceptableMediaTypes() { return null; }
    @Override public List<Locale> getAcceptableLanguages() { return null; }
    @Override public Map<String, Cookie> getCookies() { return null; }
    @Override public boolean hasEntity() { return false; }
    @Override public Object getEntity() { return null; }
    @Override public Class<?> getEntityClass() { return null; }
    @Override public java.lang.reflect.Type getEntityType() { return null; }
    @Override public void setEntity(Object e) {}
    @Override public void setEntity(Object e, Annotation[] a, MediaType m) {}
    @Override public Annotation[] getEntityAnnotations() { return null; }
    @Override public OutputStream getEntityStream() { return null; }
    @Override public void setEntityStream(OutputStream s) {}
    @Override public Client getClient() { return null; }
    @Override public Configuration getConfiguration() { return null; }
    @Override public void abortWith(Response r) {}
  }
}
