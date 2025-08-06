package com.axonivy.utils.ai.mock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.annotation.security.PermitAll;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.swagger.v3.oas.annotations.Hidden;

@Path(MockOpenAI.PATH_SUFFIX)
@PermitAll // allow unauthenticated calls
@Hidden // do not show me on swagger-ui or openapi3 resources.
@SuppressWarnings("all")
public class MockOpenAI {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static final String PATH_SUFFIX = "aiMock";

  @POST
  @Path("chat/completions")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response chat(JsonNode request, @HeaderParam("X-Test") String test) {
    if ("chat".equals(test)) {
      if (request.toPrettyString().contains("ready?")) {
        return Response.ok()
            .entity(load("completions-response.json"))
            .build();
      }
    }
    if ("tool".equals(test)) {
      return toolTest(request);
    }
    if ("tools.filter".equals(test)) {
      return toolsFilter(request);
    }
    return Response.ok().entity("not implemented!").build();
  }

  private Response toolsFilter(JsonNode request) {
    var tools = (ArrayNode) request.get("tools");
    var toolNames = new ArrayList<String>();
    tools.forEach(tool -> toolNames.add(tool.get("function").get("name").asText()));
    if (toolNames.size() == 1 && "whoami".equals(toolNames.get(0))) {
      return Response.ok()
          .entity(load("tools/filter/response.json"))
          .build();
    }
    return Response.serverError().build();
  }

  private Response toolTest(JsonNode request) {
    var messages = (ArrayNode) request.get("messages");
    System.out.println(messages.toPrettyString());
    if (messages.size() >= 1) {
      var current = messages.get(messages.size() - 1);
      if (current.toString().contains("\"tool_call_id\"")) {
        return Response.ok()
            .entity(load("tools/response4.json"))
            .build();
      }
      if (current.toPrettyString().contains("Instruction:\\n- Understand")) {
        return Response.ok()
            .entity(load("tools/response3.json"))
            .build();
      }
      if (current.toPrettyString().contains("Instruction:")) {
        return Response.ok()
            .entity(load("tools/response2.json"))
            .build();
      }
      return Response.ok()
          .entity(load("tools/response1.json"))
          .build();
    }
    return Response.status(404).build();
  }

  public static String load(String json) {
    try (var is = MockOpenAI.class.getResourceAsStream("json/" + json)) {
      if (is == null) {
        throw new RuntimeException("The json file '" + json + "' does not exist.");
      }
      return IOUtils.toString(is, StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new RuntimeException("Failed to read json " + json, ex);
    }
  }

  public static JsonNode json(String raw) {
    try {
      return MAPPER.readTree(raw);
    } catch (JsonProcessingException ex) {
      throw new RuntimeException("Failed to parse JSON from string: " + raw, ex);
    }
  }
}
