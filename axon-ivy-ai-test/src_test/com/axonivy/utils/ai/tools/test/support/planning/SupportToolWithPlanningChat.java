package com.axonivy.utils.ai.tools.test.support.planning;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class SupportToolWithPlanningChat {
  public static Response toolTest(JsonNode request) {
    var messages = (ArrayNode) request.get("messages");
    if (messages.size() >= 1) {
      var current = messages.get(messages.size() - 1);

      if (current.toPrettyString().contains("Example: [HR] User request a day leave")) {
        return Response.ok().entity(load("response4.json")).build();
      }

      if (current.toPrettyString().contains("Query:\\nHelp me, my computer is beeping")) {
        return Response.ok()
            .entity(load("response3.json"))
            .build();
      }

      if (current.toPrettyString().contains("Help me, my computer is beeping")) {
        boolean fromAssistant = messages.get(messages.size() - 2).get("role").textValue().equals("assistant");
        return Response.ok()
            .entity(load(fromAssistant ? "response2.json" : "response1.json"))
            .build();
      }

      if (current.toPrettyString().contains(
          "\\\"type\\\":\\\"TECHNICAL\\\",\\\"name\\\":\\\"Computer Beeping Issue\\\",\\\"description\\\":\\\"Computer started beeping after opening AxonIvy Portal.\\\"")) {

        String targetFile = current.toPrettyString().contains("{\\\"employeeUsername\\\" : \\\"mnhnam\\\"}")
            ? "response6.json"
            : "response5.json";

        return Response.ok(
            ).entity(load(targetFile))
            .build();
      }

      if (current.toPrettyString().contains("{\\r\\n  \\\"aiResult\\\" : \\\"Task is created successfully\\\"\\r\\n}")) {
        return Response.ok()
            .entity(load("response7.json"))
            .build();
      }

      return Response.ok()
          .entity(load("response1.json"))
          .build();
    }
    return Response.status(404).build();
  }

  private static String load(String json) {
    try (var is = SupportToolWithPlanningChat.class.getResourceAsStream(json)) {
      if (is == null) {
        throw new RuntimeException("The json file '" + json + "' does not exist.");
      }
      return IOUtils.toString(is, StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new RuntimeException("Failed to read json " + json, ex);
    }
  }
}
