package com.axonivy.utils.ai.mock;

import java.util.function.Function;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Path(MockOpenAI.PATH_SUFFIX)
@PermitAll // allow unauthenticated calls
@Hidden // do not show me on swagger-ui or openapi3 resources.
@SuppressWarnings("all")
public class MockOpenAI {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static final String PATH_SUFFIX = "aiMock";

  private static Function<JsonNode, Response> CHAT = request -> Response.ok().entity("not implemented!").build();
  private static Function<JsonNode, Response> RESPONSES = request -> Response.ok().entity("""
      {"id":"resp_test","model":"gpt-6-astra","status":"completed","output":[{"type":"message","role":"assistant","content":[{"type":"output_text","text":"ok"}]}]}
      """).build();

  public static void defineChat(Function<JsonNode, Response> chat) {
    MockOpenAI.CHAT = chat;
  }

  public static void defineResponses(Function<JsonNode, Response> responses) {
    MockOpenAI.RESPONSES = responses;
  }

  @POST
  @Path("{test}/chat/completions")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response chat(JsonNode request, @PathParam("test") String test) {
    return CHAT.apply(request);
  }

  @POST
  @Path("{test}/responses")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response responses(JsonNode request, @PathParam("test") String test) {
    return RESPONSES.apply(request);
  }

  public static JsonNode json(String raw) {
    try {
      return MAPPER.readTree(raw);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to parse JSON from string: " + raw, ex);
    }
  }
}
