package com.axonivy.utils.smart.workflow.demo.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.environment.Ivy;

public class VectorStoreRestClient {

  private static final String OPENSEARCH_CLIENT = "openSearch";
  private static final String VAR_API_KEY   = "AI.RAG.OpenSearch.ApiKey";
  private static final String VAR_USER_NAME = "AI.RAG.OpenSearch.UserName";
  private static final String VAR_PASSWORD  = "AI.RAG.OpenSearch.Password";

  private final WebTarget target;

  private VectorStoreRestClient(WebTarget target) {
    this.target = target;
  }

  public static VectorStoreRestClient fromIvyVars() {
    String apiKey   = Ivy.var().get(VAR_API_KEY);
    String userName = Ivy.var().get(VAR_USER_NAME);
    String password = Ivy.var().get(VAR_PASSWORD);
    WebTarget base = Ivy.rest().client(OPENSEARCH_CLIENT)
        .register(new AuthFilter(apiKey, userName, password));
    return new VectorStoreRestClient(base);
  }

  public boolean indexExists(String indexName) {
    try (Response response = target.path(indexName).request(MediaType.APPLICATION_JSON).head()) {
      return response.getStatus() == Response.Status.OK.getStatusCode();
    }
  }

  public void deleteIndex(String indexName) {    try (Response response = target.path(indexName).request(MediaType.APPLICATION_JSON).delete()) {
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

  public List<String> fetchAllTexts(String indexName, int maxDocs) {
    String body = "{\"query\":{\"match_all\":{}},\"size\":" + maxDocs + ",\"_source\":[\"text\"]}";
    try (Response response = target.path(indexName).path("_search")
        .request(MediaType.APPLICATION_JSON)
        .post(Entity.json(body))) {
      if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
        String err = response.readEntity(String.class);
        throw new IllegalStateException("OpenSearch _search failed for '" + indexName + "': " + err);
      }
      String json = response.readEntity(String.class);
      List<String> texts = new ArrayList<>();
      int pos = 0;
      String textKey = "\"text\":\"";
      while ((pos = json.indexOf(textKey, pos)) != -1) {
        pos += textKey.length();
        StringBuilder sb = new StringBuilder();
        while (pos < json.length()) {
          char c = json.charAt(pos);
          if (c == '"' && (pos == 0 || json.charAt(pos - 1) != '\\')) {
            break;
          }
          if (c == '\\' && pos + 1 < json.length()) {
            char next = json.charAt(pos + 1);
            if (next == '"')      { sb.append('"');  pos += 2; continue; }
            if (next == 'n')      { sb.append('\n'); pos += 2; continue; }
            if (next == 't')      { sb.append('\t'); pos += 2; continue; }
            if (next == '\\')     { sb.append('\\'); pos += 2; continue; }
          }
          sb.append(c);
          pos++;
        }
        texts.add(sb.toString());
      }
      return texts;
    }
  }

  private static class AuthFilter implements ClientRequestFilter {

    private final String apiKey;
    private final String userName;
    private final String password;

    AuthFilter(String apiKey, String userName, String password) {
      this.apiKey   = apiKey;
      this.userName = userName;
      this.password = password;
    }

    @Override
    public void filter(ClientRequestContext context) throws IOException {
      if (StringUtils.isNotBlank(apiKey)) {
        context.getHeaders().add("Authorization", "ApiKey " + apiKey);
      } else if (StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(password)) {
        String credentials = userName + ":" + password;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        context.getHeaders().add("Authorization", "Basic " + encoded);
      }
    }
  }
}
