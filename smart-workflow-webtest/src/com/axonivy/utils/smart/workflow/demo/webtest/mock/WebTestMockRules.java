package com.axonivy.utils.smart.workflow.demo.webtest.mock;

import java.util.List;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Holds the rules {@link WebTestMockProvider} answers with. A web test runs outside the engine, so it sends its
 * rules here before it chats and clears them afterwards. Only part of the web test project, never of a product.
 */
@Path(WebTestMockRules.PATH)
@PermitAll
@Hidden
public class WebTestMockRules {

  public static final String PATH = "webTestMock/rules";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static volatile List<WebTestMockRule> rules = List.of();

  /** replaces all rules; they are matched in this order and the first match answers */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  public void define(String json) {
    rules = List.copyOf(MAPPER.readValue(json, new TypeReference<List<WebTestMockRule>>() {}));
  }

  @DELETE
  public void clear() {
    rules = List.of();
  }

  public static List<WebTestMockRule> current() {
    return rules;
  }
}
