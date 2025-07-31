package com.axonivy.utils.ai.mock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Predicate;

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

import io.swagger.v3.oas.annotations.Hidden;

@Path(MockOpenAI.PATH_SUFFIX)
@PermitAll // allow unauthenticated calls
@Hidden // do not show me on swagger-ui or openapi3 resources.
@SuppressWarnings("all")
public class MockOpenAI {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static final String PATH_SUFFIX = "aiMock";

  // Data mapper response mappings
  private static final Map<Predicate<String>, String> DATA_MAPPER_RESPONSES = Map.of(
      request -> request.contains("Jane Smith, 25"), "jane_smith",
      request -> request.contains("Alice Johnson, Senior Developer, 75000 salary"), "alice_johnson",
      request -> request.contains("Chris Davis, twenty-five years, working"), "chris_davis",
      request -> request.contains("invalid_json_response"), "invalid_json");

  // Decision maker response mappings
  private static final Map<Predicate<String>, String> DECISION_MAKER_RESPONSES = Map.of(
      request -> request.contains("3 weeks vacation during busy season"), "reject_vacation",
      request -> request.contains("incorrect billing charges"), "billing_issue",
      request -> request.contains("System is down and customers cannot process payments"), "critical_priority",
      request -> request.contains("Senior manager requesting 1 week vacation"), "approve_manager",
      request -> request.contains("invalid_option_response"), "invalid_option",
      request -> request.contains("malformed_json_response"), "malformed_json");

  // Agent planner response mappings
  private static final Map<Predicate<String>, String> AGENT_PLANNER_RESPONSES = Map.of(
      request -> request.contains("Process a customer support ticket"), "simple_support_plan",
      request -> request.contains("Complete customer onboarding process"), "complex_onboarding_plan",
      request -> request.contains("Handle urgent customer complaint"), "escalation_plan",
      request -> request.contains("Generate monthly sales report"), "single_step_plan",
      request -> request.contains("invalid_json_response"), "invalid_plan");

  @POST
  @Path("chat/completions")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response chat(JsonNode request, @HeaderParam("X-Test") String test) {
    return switch (test) {
    case "chat" -> handleChatTest(request);
    case "datamapper" -> handleTestWithResponseMapping(request, test, DATA_MAPPER_RESPONSES);
    case "decisionmaker" -> handleTestWithResponseMapping(request, test, DECISION_MAKER_RESPONSES);
    case "agentplanner" -> handleTestWithResponseMapping(request, test, AGENT_PLANNER_RESPONSES);
    default -> Response.ok().entity("not implemented!").build();
    };
  }

  private Response handleChatTest(JsonNode request) {
    if (request.toPrettyString().contains("ready?")) {
      return Response.ok()
          .entity(load("completions-response.json"))
          .build();
    }
    return Response.ok().entity("not implemented!").build();
  }

  private Response handleTestWithResponseMapping(JsonNode request, String testType,
      Map<Predicate<String>, String> responseMapping) {
    String requestStr = request.toPrettyString();

    try {
      JsonNode responses = json(load(testType + ".json"));

      // Find matching response key using stream and lambda
      String responseKey = responseMapping.entrySet().stream().filter(entry -> entry.getKey().test(requestStr))
          .map(Map.Entry::getValue).findFirst().orElse("default");

      // Get the specific response
      JsonNode selectedResponse = responses.get(responseKey);
      if (selectedResponse != null) {
        return Response.ok().entity(selectedResponse.toString()).build();
      }

      // Fallback to default
      return Response.ok().entity(responses.get("default").toString()).build();

    } catch (Exception e) {
      // If file loading fails, return basic error response
      return Response.ok().entity(createBasicErrorResponse()).build();
    }
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

  /**
   * Creates a basic error response when file loading fails
   */
  private String createBasicErrorResponse() {
    return """
        {
          "id": "chatcmpl-error",
          "object": "chat.completion",
          "created": 1681911424,
          "model": "gpt-4o-mini",
          "choices": [
            {
              "message": {
                "role": "assistant",
                "content": "Error loading response"
              },
              "finish_reason": "stop",
              "index": 0
            }
          ]
        }
        """;
  }
}
