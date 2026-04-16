package com.axonivy.utils.smart.workflow.tools.math;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import ch.ivyteam.test.resource.ResourceResponder;

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
