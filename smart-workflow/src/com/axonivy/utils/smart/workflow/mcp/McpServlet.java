package com.axonivy.utils.smart.workflow.mcp;

import java.io.IOException;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

public class McpServlet extends HttpServlet {

  private HttpServletStreamableServerTransportProvider transportProvider;
  private McpSyncServer syncServer;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    McpJsonMapper MAPPER = new JacksonMcpJsonMapper(new JsonMapper());

    transportProvider = HttpServletStreamableServerTransportProvider.builder()
        .jsonMapper(MAPPER)
        .mcpEndpoint("/mcpeee")
        .build();

    syncServer = McpServer.sync(transportProvider)
        .serverInfo("my-server", "1.0.0")
        .capabilities(ServerCapabilities.builder()
            .resources(true, true)
            .tools(true)
            .prompts(true)
            .logging()
            .build())
        .build();

    transportProvider.init(config);
  }

  @Override
  protected void service(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    transportProvider.service(request, response);
  }

  @Override
  public void destroy() {
    if (syncServer != null) {
      syncServer.close();
    }
    if (transportProvider != null) {
      transportProvider.destroy();
    }
    super.destroy();
  }

}
