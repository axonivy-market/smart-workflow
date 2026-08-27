package com.axonivy.utils.smart.workflow.mcp;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import tools.jackson.databind.json.JsonMapper;

public class McpServlet {

  private static void tara() {

    McpJsonMapper MAPPER = new JacksonMcpJsonMapper(new JsonMapper());

    var transportProvider = HttpServletStreamableServerTransportProvider.builder()
        .jsonMapper(MAPPER)
        .build();

    // Create a server with custom configuration
    McpSyncServer syncServer = McpServer.sync(transportProvider)
        .serverInfo("my-server", "1.0.0")
        .capabilities(ServerCapabilities.builder()
            .resources(true, true)     // Enable resource support
            .tools(true)         // Enable tool support
            .prompts(true)       // Enable prompt support
            .logging()           // Enable logging support
            .build())
        .build();
  }

}
