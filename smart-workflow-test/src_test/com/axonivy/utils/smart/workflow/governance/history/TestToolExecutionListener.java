package com.axonivy.utils.smart.workflow.governance.history;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.recorder.internal.ChatHistoryRepository;
import com.axonivy.utils.smart.workflow.governance.listener.ToolExecutionListener;

import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.observability.api.event.ToolExecutedEvent;

@IvyTest
public class TestToolExecutionListener {

  private InMemoryHistoryStorage storage;
  private ChatHistoryRepository repo;
  private ToolExecutionListener listener;

  @BeforeEach
  void setUp() {
    storage = new InMemoryHistoryStorage();
    repo = new ChatHistoryRepository("case-1", "task-1", "test-agent", storage);
    listener = new ToolExecutionListener(repo);
  }

  @Test
  void recordsToolExecutions() {
    var invocationId = UUID.randomUUID();
    listener.onEvent(buildEvent(invocationId, "getWeather", "{\"city\":\"Lucerne\"}", "Sunny, 25°C"));
    listener.onEvent(buildEvent(invocationId, "getTime", "{}", "12:00"));

    assertThat(storage.findAll()).hasSize(1);

    var entry = storage.findAll().get(0);
    var tools = entry.getToolExecutions();
    assertThat(tools).hasSize(2);

    var first = tools.get(0);
    assertThat(first.toolName()).isEqualTo("getWeather");
    assertThat(first.arguments()).isEqualTo("{\"city\":\"Lucerne\"}");
    assertThat(first.resultText()).isEqualTo("Sunny, 25°C");
    assertThat(first.executedAt()).isNotNull();

    assertThat(tools.get(1).toolName()).isEqualTo("getTime");
  }

  @Test
  void stripsBase64FromImageMessages() {
    var image = ImageContent.from("aGVsbG8=", "image/png", ImageContent.DetailLevel.HIGH);
    repo.store(List.of(UserMessage.from(image)), null);

    var messagesJson = storage.findAll().get(0).getMessagesJson();
    assertThat(messagesJson).doesNotContain("base64Data");
    assertThat(messagesJson).contains("mimeType");
  }

  private ToolExecutedEvent buildEvent(UUID invocationId, String toolName, String arguments,
      String resultText) {
    var invocationCtx = InvocationContext.builder()
        .invocationId(invocationId)
        .timestamp(Instant.now())
        .methodName("chat")
        .interfaceName("ChatAgent")
        .build();
    var request = ToolExecutionRequest.builder()
        .name(toolName)
        .arguments(arguments)
        .build();
    return ToolExecutedEvent.builder()
        .invocationContext(invocationCtx)
        .request(request)
        .resultText(resultText)
        .build();
  }
}
