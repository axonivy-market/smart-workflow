package com.axonivy.utils.smart.workflow.demo.erp.shopping.mock;

import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import ch.ivyteam.test.resource.ResourceResponder;

public class ShoppingChat {

  private final ResourceResponder responder = new ResourceResponder(ShoppingChat.class);

  public Response brandAgentResponse(JsonNode request) {
    var messages = (ArrayNode) request.get("messages");
    if (messages.size() <= 2) {
      return responder.send("brandAgentToolCallResponse.json");
    }
    return responder.send("brandAgentFinalResponse.json");
  }

  public Response supplierAgentResponse(JsonNode request) {
    var messages = (ArrayNode) request.get("messages");
    if (messages.size() <= 2) {
      return responder.send("supplierAgentToolCallResponse.json");
    }
    return responder.send("supplierAgentFinalResponse.json");
  }

  public Response productAgentResponse(JsonNode request) {
    var messages = (ArrayNode) request.get("messages");
    if (messages.size() <= 2) {
      return responder.send("productAgentToolCallResponse.json");
    }
    return responder.send("productAgentFinalResponse.json");
  }

  public Response categoryAgentResponse(JsonNode request) {
    var messages = (ArrayNode) request.get("messages");
    if (messages.size() <= 2) {
      return responder.send("categoryAgentToolCallResponse.json");
    }
    return responder.send("categoryAgentFinalResponse.json");
  }

  public Response translationResponse(JsonNode request) {
    return responder.send("translationResponse.json");
  }
}
