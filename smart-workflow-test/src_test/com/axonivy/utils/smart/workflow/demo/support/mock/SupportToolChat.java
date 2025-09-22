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
      return responder.send("response1.json");
    }
    return Response.status(404).build();
  }

}
