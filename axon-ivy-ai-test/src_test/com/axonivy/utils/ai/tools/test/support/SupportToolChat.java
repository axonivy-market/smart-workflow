package com.axonivy.utils.ai.tools.test.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class SupportToolChat {

  public static Response toolTest(JsonNode request) {
    var messages = (ArrayNode) request.get("messages");
    if (messages.size() >= 1) {
      var current = messages.get(messages.size() - 1);
      if (messages.size() == 2) {
        if (current.get("content").textValue()
            .equals("Help me, my computer is beeping, it started after opening AxonIvy Portal.")) {
          return Response.ok().entity(load("response1.json")).build();
        }
        if (current.get("content").textValue()
            .equals("Computer is beeping after opening AxonIvy Portal. Need technical support.")) {
          return Response.ok().entity(load("response2.json")).build();
        }
      }

      if (current.toString().contains("tool_call_id")) {
        return Response.ok()
            .entity(load("response3.json"))
            .build();
      }
    }
    return Response.status(404).build();
  }

  private static String load(String json) {
    try (var is = SupportToolChat.class.getResourceAsStream(json)) {
      if (is == null) {
        throw new RuntimeException("The json file '" + json + "' does not exist.");
      }
      return IOUtils.toString(is, StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new RuntimeException("Failed to read json " + json, ex);
    }
  }

}
