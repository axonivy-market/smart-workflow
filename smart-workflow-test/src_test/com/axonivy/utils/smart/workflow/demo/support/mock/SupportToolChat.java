package com.axonivy.utils.smart.workflow.demo.support.mock;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import ch.ivyteam.test.resource.ResourceResponder;

public class SupportToolChat {

  private final ResourceResponder responder = new ResourceResponder(SupportToolChat.class);

  public Response toolTest(JsonNode request) {
    var messages = (ArrayNode) request.get("messages");
    if (messages.size() >= 1) {
      var current = messages.get(messages.size() - 1);
      if (messages.size() == 2) {
        if ("Help me, my computer is beeping, it started after opening AxonIvy Portal."
            .equals(current.get("content").textValue())) {
          return responder.send("response1.json");
        }
        if ("Computer is beeping after opening AxonIvy Portal. Need technical support."
            .equals(current.get("content").textValue())) {
          return responder.send("response2.json");
        }
      }

      if (current.toString().contains("tool_call_id")
          || "Computer is beeping after opening AxonIvy Portal".equals(current.get("content").textValue())) {
        return responder.send("response3.json");
      }
    }
    return Response.status(404).build();
  }

}
