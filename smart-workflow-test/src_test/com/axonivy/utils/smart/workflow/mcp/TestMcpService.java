package com.axonivy.utils.smart.workflow.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.rest.client.authentication.HttpBasicAuthenticationFeature;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

@IvyProcessTest(enableWebServer = true)
public class TestMcpService {

  @Test
  void disabledBydefault() {
    Response response = smartWorkflowMcp().register(HttpBasicAuthenticationFeature.basic("James", "secret"))
        .request()
        .accept(MediaType.APPLICATION_JSON)
        .post(Entity.json(Map.of(
            "jsonrpc", "2.0",
            "id", 123,
            "method", "tools/list")));
    assertThat(response.getStatus())
        .as("MCP tool exposure is disabled by default")
        .isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  @Test
  void needsAuthentication() {
    Response response = smartWorkflowMcp()
        .request()
        .accept(MediaType.APPLICATION_JSON)
        .post(Entity.json(Map.of(
            "jsonrpc", "2.0",
            "id", 123,
            "method", "tools/list")));
    assertThat(response.getStatus())
        .as("MCP tool exposure requires authentication")
        .isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  void toolsList(AppFixture fixture) {
    fixture.var("AI.Tools.MCP.ExposeTools.Enabled", Boolean.TRUE.toString());
    Response response = smartWorkflowMcp()
        .register(HttpBasicAuthenticationFeature.basic("James", "secret"))
        .request()
        .accept(MediaType.APPLICATION_JSON)
        .post(Entity.json(Map.of(
            "jsonrpc", "2.0",
            "id", 123,
            "method", "tools/list")));
    var tools = response
        .readEntity(String.class);
    assertThat(tools)
        .as("Exposes java provided tools")
        .contains("webSearch");
    assertThat(tools)
        .as("Exposes ivy process tools")
        .contains("whoami");

    var jsonTools = JsonMapper.shared().readTree(tools);
    var jTools = (ArrayNode) jsonTools.get("result").get("tools");
    var addTool = jTools.elements().stream().filter(t -> t.get("name").asText().equals("add")).findFirst().orElse(null);
    var input = addTool.get("inputSchema");
    assertThat(input.toPrettyString()).contains("\"a\"");
  }

  @Test
  void toolsCall(AppFixture fixture) {
    fixture.var("AI.Tools.MCP.ExposeTools.Enabled", Boolean.TRUE.toString());
    Response response = smartWorkflowMcp()
        .register(HttpBasicAuthenticationFeature.basic("James", "secret"))
        .request()
        .accept(MediaType.APPLICATION_JSON)
        .post(Entity.json(Map.of(
            "jsonrpc", "2.0",
            "id", 123,
            "method", "tools/call",
            "params", Map.of(
                "name", "whoami",
                "arguments", Map.of()))));
    var result = response
        .readEntity(String.class);
    assertThat(result)
        .as("Can call ivy process tool")
        .contains("James");
  }

  @Test
  void toolsCallParams(AppFixture fixture) {
    fixture.var("AI.Tools.MCP.ExposeTools.Enabled", Boolean.TRUE.toString());
    Response response = smartWorkflowMcp()
        .register(HttpBasicAuthenticationFeature.basic("James", "secret"))
        .request()
        .accept(MediaType.APPLICATION_JSON)
        .post(Entity.json(Map.of(
            "jsonrpc", "2.0",
            "id", 123,
            "method", "tools/call",
            "params", Map.of(
                "name", "add",
                "arguments", Map.of("a", 1, "b", 2)))));
    var result = response
        .readEntity(String.class);
    assertThat(result)
        .as("Can call ivy process tool")
        .contains("3");
  }

  private WebTarget smartWorkflowMcp() {
    return Ivy.rest().client("smartWorkflow")
        .path("mcp");
  }

}
