package com.axonivy.utils.smart.workflow.mcp;

import java.util.LinkedHashMap;
import java.util.Map;

import com.axonivy.utils.smart.workflow.tools.internal.IvyToolsProcesses;
import com.axonivy.utils.smart.workflow.tools.provider.IvySubProcessToolsProvider;
import com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowToolsProvider;

import ch.ivyteam.ivy.engine.rest.service.jersey.security.csrf.DisableCsrfProtection;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter.SearchScope;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolProviderResult;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

@Singleton
@DisableCsrfProtection // no X-Requested-By header
@Path("mcp")
@Produces(MediaType.APPLICATION_JSON)
public class McpService {

  private static final String PROTOCOL_VERSION = "2026-07-28";

  private Map<String, AiServiceTool> getTools() {
    var tools = new LinkedHashMap<String, AiServiceTool>();
    addTools(tools, SmartWorkflowToolsProvider.provideTools(null));
    var starts = new IvyToolsProcesses().scope(SearchScope.APPLICATION).toolStarts();
    addTools(tools, new IvySubProcessToolsProvider().getTools(starts));
    return tools;
  }

  private static void addTools(Map<String, AiServiceTool> tools, ToolProviderResult result) {
    result.aiServiceTools().forEach(tool -> tools.putIfAbsent(tool.name(), tool));
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response handle(JsonNode request) {
    // if (!IvyVar.bool("AI.Tools.MCP.ExposeTools.Enabled")) {
    //   return Response.status(Response.Status.NOT_FOUND)
    //     .entity("MCP tool exposure is disabled.")
    // .build();
    // 
    if (request == null || !request.isObject() || !"2.0".equals(request.path("jsonrpc").asText())) {
      return jsonRpcError(null, -32600, "Invalid Request");
    }

    JsonNode id = request.get("id");
    String method = request.path("method").asText(null);
    if (method == null) {
      return jsonRpcError(id, -32600, "Invalid Request");
    }

    return switch (method) {
      case "initialize" -> initialize(id);
      case "ping" -> result(id, object());
      case "tools/list" -> listTools(id);
      case "tools/call" -> callTool(id, request.path("params"));
      case "notifications/initialized" -> Response.status(Response.Status.ACCEPTED).build();
      default -> jsonRpcError(id, -32601, "Method not found");
    };
  }

  @POST
  @Path("initialize")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response initializeApi(JsonNode request) {
    return initialize(requestId(request));
  }

  @POST
  @Path("ping")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response ping(JsonNode request) {
    return result(requestId(request), object());
  }

  @POST
  @Path("tools/list")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response listToolsApi(JsonNode request) {
    return listTools(requestId(request));
  }

  @POST
  @Path("tools/call")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response callToolApi(JsonNode request) {
    return callTool(requestId(request), request == null ? object() : request.path("params"));
  }

  @POST
  @Path("notifications/initialized")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response initialized(JsonNode request) {
    return Response.status(Response.Status.ACCEPTED).build();
  }

  private static JsonNode requestId(JsonNode request) {
    return request == null ? null : request.get("id");
  }

  private Response initialize(JsonNode id) {
    ObjectNode result = object().put("protocolVersion", PROTOCOL_VERSION);
    ObjectNode capabilities = result.putObject("capabilities");
    capabilities.putObject("tools");
    result.putObject("serverInfo")
        .put("name", "smart-workflow")
        .put("version", "14.0.0");
    return result(id, result);
  }

  private Response listTools(JsonNode id) {
    ObjectNode result = object();
    ArrayNode toolList = result.putArray("tools");
    getTools().values().forEach(tool -> toolList.add(toolDefinition(tool)));
    return result(id, result);
  }

  private Response callTool(JsonNode id, JsonNode params) {
    String name = params.path("name").asText(null);
    AiServiceTool tool = getTools().get(name);
    if (tool == null) {
      return jsonRpcError(id, -32602, "Unknown tool: " + name);
    }

    try {
      var request = ToolExecutionRequest.builder()
          .name(name)
          .arguments(argumentsJson(params.path("arguments")))
          .build();
      Object value = tool.toolExecutor().execute(request, null);
      ObjectNode result = object();
      ArrayNode content = result.putArray("content");
      content.addObject().put("type", "text").put("text", stringify(value));
      return result(id, result);
    } catch (Exception ex) {
      Ivy.log().error("MCP tool execution failed for " + name, ex);
      ObjectNode result = object();
      result.put("isError", true);
      result.putArray("content").addObject()
          .put("type", "text")
          .put("text", "Tool execution failed: " + ex.getMessage());
      return result(id, result);
    }
  }

  private static String argumentsJson(JsonNode arguments) {
    if (arguments == null || arguments.isMissingNode() || arguments.isNull()) {
      return "{}";
    }
    return arguments.toString();
  }

  private ObjectNode toolDefinition(AiServiceTool tool) {
    return toolDefinition(tool.toolSpecification());
  }

  private ObjectNode toolDefinition(ToolSpecification tool) {
    ObjectNode definition = object()
        .put("name", tool.name())
        .put("description", tool.description() == null ? "" : tool.description());
    definition.set("inputSchema", inputSchema(tool));
    return definition;
  }

  private JsonNode inputSchema(ToolSpecification tool) {
    if (tool.parameters() == null) {
      return object().put("type", "object");
    }
    try {
      return new ObjectMapper().valueToTree(tool.parameters());
    } catch (Exception ex) {
      Ivy.log().warn("Failed to serialize MCP tool schema for " + tool.name(), ex);
      return object().put("type", "object");
    }
  }

  private String stringify(Object value) {
    if (value instanceof String text) {
      return text;
    }
    try {
      return com.axonivy.utils.smart.workflow.utils.JsonUtils.getObjectMapper().writeValueAsString(value);
    } catch (Exception ex) {
      return String.valueOf(value);
    }
  }

  private static Response result(JsonNode id, JsonNode result) {
    ObjectNode response = object();
    response.put("jsonrpc", "2.0");
    response.set("id", id);
    response.set("result", result);
    return Response.ok(response).build();
  }

  private static Response jsonRpcError(JsonNode id, int code, String message) {
    ObjectNode response = object();
    response.put("jsonrpc", "2.0");
    response.set("id", id);
    response.putObject("error").put("code", code).put("message", message);
    return Response.ok(response).build();
  }

  private static ObjectNode object() {
    return JsonNodeFactory.instance.objectNode();
  }
}
