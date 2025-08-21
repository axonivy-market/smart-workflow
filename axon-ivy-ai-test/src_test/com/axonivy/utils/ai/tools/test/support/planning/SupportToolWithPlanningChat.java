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

      if (current.toPrettyString().contains("Help me, my computer is beeping, it started after opening AxonIvy Portal.")) {
        if (messages.get(1).get("role").textValue().contains("system"))
        return Response.ok().entity(load("response1.json")).build();
      }
      
      if (messages.size() == 1) {
        if (current.toPrettyString().contains(
            "Add custom field: 'employeeUsername', value is the employee username")) {
          return Response.ok().entity(load("response5.json")).build();
        }
        if (current.toPrettyString().contains(
            "Example: [HR] User request a day leave\\n- description must be the whole query\\n- ticket name must be informative")) {
          return Response.ok().entity(load("response3.json")).build();
        }
        if (current.toPrettyString().contains("Do not change the structure, only update the field values")) {
          return Response.ok().entity(load("response4.json")).build();
        }
        if (current.toPrettyString().contains("Query:\\nMy computer is beeping")) {
          return Response.ok().entity(load("response2.json")).build();
        }
      }

      if (current.toPrettyString().contains("Task is created successfully")) {
        return Response.ok().entity(load("response6.json")).build();
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
