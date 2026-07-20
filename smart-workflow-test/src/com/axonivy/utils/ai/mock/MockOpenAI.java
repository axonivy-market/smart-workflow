package com.axonivy.utils.ai.mock;

import java.util.function.Function;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Hidden;

@Path(MockOpenAI.PATH_SUFFIX)
@PermitAll // allow unauthenticated calls
@Hidden // do not show me on swagger-ui or openapi3 resources.
@SuppressWarnings("all")
public class MockOpenAI {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static final String PATH_SUFFIX = "aiMock";

  private static Function<JsonNode, Response> CHAT = request -> Response.ok().entity("not implemented!").build();

  public static void defineChat(Function<JsonNode, Response> chat) {
    MockOpenAI.CHAT = chat;
  }

  @POST
  @Path("{test}/chat/completions")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response chat(JsonNode request, @PathParam("test") String test) {
    return CHAT.apply(request);
  }

  public static JsonNode json(String raw) {
    try {
      return MAPPER.readTree(raw);
    } catch (JsonProcessingException ex) {
      throw new RuntimeException("Failed to parse JSON from string: " + raw, ex);
    }
  }
}
