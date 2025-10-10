package com.axonivy.utils.smart.workflow.client.mcp;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.axonivy.utils.smart.workflow.connector.OpenAiServiceConnector;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.test.log.LoggerAccess;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.http.client.log.LoggingHttpClient;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolExecutor;

@IvyTest
class TestMcpClient {

  @RegisterExtension
  LoggerAccess mcpLog = new LoggerAccess("MCP");

  @RegisterExtension
  LoggerAccess mainLog = new LoggerAccess(LoggingHttpClient.class.getName());

  @BeforeEach
  void setup() {
    // git clone >>https://github.com/github/github-mcp-server?tab=readme-ov-file#local-github-mcp-server
    // docker build -t mcp/github .
    // lease a token and setup the var "AI.MCP.github.token"
  }

  @Test
  void fetchGithubData() {
    var model = OpenAiServiceConnector
        .buildOpenAiModel()
        .build();

    try (var mcpClient = GitMcp.client()) {
      Bot bot = gitBot(model, GitMcp.toolProvider(mcpClient));
      String response = bot.chat("Summarize the last 3 commits of the axonivy/core GitHub repository");
      System.out.println("RESPONSE: " + response);
    }

    mcpLog.debugs().stream()
        .forEach(System.out::println);

    mainLog.debugs().stream()
        .forEach(System.out::println);
  }

  private Bot gitBot(OpenAiChatModel model, McpToolProvider toolProvider) {
    return AiServices.builder(Bot.class)
        .chatModel(model)
        .toolProvider(toolProvider)
        .build();
  }

  public interface Bot {
    String chat(String message);
  }

  private static class GitMcp {

    private static McpToolProvider toolProvider(DefaultMcpClient mcpClient) {
      return McpToolProvider.builder()
          .mcpClients(List.of(mcpClient))
          .toolWrapper(ToolBouncer::new)
          .build();
    }

    private static DefaultMcpClient client() {
      McpTransport transport = new StdioMcpTransport.Builder()
          .command(List.of("/usr/bin/docker", "run",
              "-e", "GITHUB_PERSONAL_ACCESS_TOKEN=" + Ivy.var().get("AI.MCP.github.token"),
              "-i", "mcp/github"))
          .logEvents(true)
          .build();

      return new DefaultMcpClient.Builder()
          .transport(transport)
          .build();
    }
  }

  /**
   * Let's tightly control what we call rather than letting the LLM call directly.
   *
   * <p>It seems like it's never the LLM that directly calls remote services:</p>
   * - LLM selects the tool to call
   * - the MCPclient runs the remote tool call.
   *
   * dev.langchain4j.service.tool.ToolService.executeInferenceAndToolsLoop(ChatResponse, ChatRequestParameters, List<ChatMessage>, ChatModel, ChatMemory, Object, Map<String, ToolExecutor>)
   */
  static class ToolBouncer implements ToolExecutor {

    private final ToolExecutor origin;

    public ToolBouncer(ToolExecutor origin) {
      this.origin = origin;
    }

    @Override
    public String execute(ToolExecutionRequest execRequest, Object memoryId) {
      System.out.println("routing tool request " + execRequest);
      var response = origin.execute(execRequest, memoryId);
      System.out.println("MCP tool returned " + response);
      return response;
    }

  }

}
