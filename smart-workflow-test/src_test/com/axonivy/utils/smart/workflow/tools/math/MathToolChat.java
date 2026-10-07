package com.axonivy.utils.smart.workflow.tools.math;

import ch.ivyteam.test.resource.ResourceResponder;
import jakarta.ws.rs.core.Response;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

public class MathToolChat {

  private final ResourceResponder responder = new ResourceResponder(MathToolChat.class);

  public Response toolTest(JsonNode request) {
    var messages = (ArrayNode) request.get("messages");
    if (messages.size() == 2) {
      return responder.send("r1ToolCall.json");
    }
    if (messages.size() == 4) {
      return responder.send("r2Completed.json");
    }
    return Response.status(404).build();
  }

  public Response authError(JsonNode request) {
    return Response.status(401)
        .entity(responder.load("reAuthError.json"))
        .build();
  }

}
