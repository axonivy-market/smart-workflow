package com.axonivy.utils.smart.workflow.demo.web.mock;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import ch.ivyteam.test.resource.ResourceResponder;

public class WebSearchDemoChat {

  private final ResourceResponder responder = new ResourceResponder(WebSearchDemoChat.class);

  public Response respond(JsonNode request) {
    var messages = (ArrayNode) request.get("messages");
    if (messages.size() <= 2) {
      return responder.send("response1.json");
    }
    return responder.send("response2.json");
  }
}
