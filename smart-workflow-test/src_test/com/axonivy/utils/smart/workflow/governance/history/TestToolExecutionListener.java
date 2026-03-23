package com.axonivy.utils.smart.workflow.governance.history;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.recorder.internal.ToolExecutionRepository;
import com.axonivy.utils.smart.workflow.governance.listener.ToolExecutionListener;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.observability.api.event.ToolExecutedEvent;

public class TestToolExecutionListener {

  private InMemoryToolExecutionStorage storage;
  private ToolExecutionListener listener;

  @BeforeEach
  void setUp() {
    storage = new InMemoryToolExecutionStorage();
    listener = new ToolExecutionListener(new ToolExecutionRepository("case-1", "task-1", "test-agent", storage));
  }

  @Test
  void recordsToolExecutions() {
    var invocationId = UUID.randomUUID();
    listener.onEvent(buildEvent(invocationId, "getWeather", "{\"city\":\"Lucerne\"}", "Sunny, 25°C"));
    listener.onEvent(buildEvent(invocationId, "getTime", "{}", "12:00"));

    assertThat(storage.findAll()).hasSize(2);

    var first = storage.findAll().get(0);
    assertThat(first.getToolName()).isEqualTo("getWeather");
    assertThat(first.getArguments()).isEqualTo("{\"city\":\"Lucerne\"}");
    assertThat(first.getResultText()).isEqualTo("Sunny, 25°C");
    assertThat(first.getAgentId()).isNotBlank();
    assertThat(first.getCaseUuid()).isEqualTo("case-1");
    assertThat(first.getTaskUuid()).isEqualTo("task-1");
    assertThat(first.getExecutedAt()).isNotNull();

    assertThat(storage.findAll().get(1).getToolName()).isEqualTo("getTime");
    assertThat(storage.findAll().get(0).getAgentId())
        .isEqualTo(storage.findAll().get(1).getAgentId());
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
