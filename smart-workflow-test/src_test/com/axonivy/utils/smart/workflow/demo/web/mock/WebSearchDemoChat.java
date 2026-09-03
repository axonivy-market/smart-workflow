package com.axonivy.utils.smart.workflow.demo.web.mock;

import ch.ivyteam.test.resource.ResourceResponder;
import jakarta.ws.rs.core.Response;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

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
