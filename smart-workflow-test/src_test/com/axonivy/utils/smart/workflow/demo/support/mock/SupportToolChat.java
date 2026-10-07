package com.axonivy.utils.smart.workflow.demo.support.mock;

import ch.ivyteam.test.resource.ResourceResponder;
import jakarta.ws.rs.core.Response;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

public class SupportToolChat {

  private final ResourceResponder responder = new ResourceResponder(SupportToolChat.class);

  public Response toolTest(JsonNode request) {
    var messages = (ArrayNode) request.get("messages");
    if (messages.size() >= 1) {
      return responder.send("response1.json");
    }
    return Response.status(404).build();
  }

}
