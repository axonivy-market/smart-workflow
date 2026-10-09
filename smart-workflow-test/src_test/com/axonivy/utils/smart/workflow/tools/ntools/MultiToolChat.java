package com.axonivy.utils.smart.workflow.tools.ntools;

import ch.ivyteam.test.resource.ResourceResponder;
import jakarta.ws.rs.core.Response;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

public class MultiToolChat {

  private final ResourceResponder responder = new ResourceResponder(MultiToolChat.class);

  public Response nTools(JsonNode request) {
    var messages = (ArrayNode) request.get("messages");
    if (messages.size() == 2) {
      return responder.send("nTools_r1.json");
    }
    if (messages.size() == 5) {
      return responder.send("nTools_r2.json");
    }
    return Response.status(404).build();
  }

}
