package com.axonivy.utils.smart.orchestrator.demo.support.planning;

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

      if (current.toPrettyString()
          .equals("Help me, my computer is beeping, it started after opening AxonIvy Portal.")) {
        return Response.ok().entity(load("response1.json")).build();
      }

      if (messages.size() == 2 && messages.get(0).get("content").toPrettyString()
          .equals("Instruction to follow:\\n- Don't fill information related to approval\\n[]\\n")) {
        return Response.ok().entity(load("response2.json")).build();
      }

      if (current.get("role").textValue().equals("tool")
          && current.toPrettyString().contains("\\\"id\\\" : \\\"882bb24b848b4583aca8e7cc503e807d\\\"")) {
        return Response.ok().entity(load("response3.json")).build();
      }

      if (current.get("role").textValue().equals("user")
          && current.toPrettyString().contains("\\\"id\\\" : \\\"882bb24b848b4583aca8e7cc503e807d\\\"")) {
        return Response.ok().entity(load("response4.json")).build();
      }

      return Response.ok()
          .entity(load("response5.json"))
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
