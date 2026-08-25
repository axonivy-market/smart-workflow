package com.axonivy.utils.smart.workflow.demo.supplier.mock;

import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import ch.ivyteam.test.resource.ResourceResponder;

public class SupplierDemoChat {

  private final ResourceResponder responder = new ResourceResponder(SupplierDemoChat.class);

  public Response assistantResponse(JsonNode request) {
    return responder.send("assistantResponse.json");
  }

  public Response parseResponse(JsonNode request) {
    return responder.send("parseResponse.json");
  }

  public Response duplicateCheckResponse(JsonNode request) {
    return responder.send("duplicateCheckResponse.json");
  }
}
