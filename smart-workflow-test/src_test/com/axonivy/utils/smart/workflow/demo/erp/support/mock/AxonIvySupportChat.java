package com.axonivy.utils.smart.workflow.demo.erp.support.mock;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import ch.ivyteam.test.resource.ResourceResponder;

public class AxonIvySupportChat {

  private final ResourceResponder responder = new ResourceResponder(AxonIvySupportChat.class);

  public Response agentResponse(JsonNode request) {
    var messages = (ArrayNode) request.get("messages");
    if (messages.size() <= 2) {
      return responder.send("agentResponse.json");
    }
    return responder.send("agentFinalResponse.json");
  }

  public Response toolResponse(JsonNode request) {
    var messages = (ArrayNode) request.get("messages");
    if (messages.size() <= 2) {
      return responder.send("toolResponse1.json");
    }
    if (messages.size() <= 4) {
      return responder.send("toolResponse2.json");
    }
    return responder.send("toolResponse3.json");
  }
}
