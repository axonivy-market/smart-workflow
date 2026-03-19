package com.axonivy.utils.smart.workflow.governance.history;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.listener.ToolExecutionHistoryListener;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.observability.api.event.ToolExecutedEvent;

public class TestToolExecutionHistoryListener {

  private InMemoryToolExecutionStorage storage;
  private ToolExecutionHistoryListener listener;

  @BeforeEach
  void setUp() {
    storage = new InMemoryToolExecutionStorage();
    ToolExecutionRepository.testStorage = storage;
    listener = new ToolExecutionHistoryListener("case-1", "task-1");
  }

  @AfterEach
  void tearDown() {
    ToolExecutionRepository.testStorage = null;
  }

  @Test
  void onEventRecordsToolExecution() {
    listener.onEvent(buildEvent("getWeather", "{\"city\":\"Lucerne\"}", "Sunny, 25°C"));

    assertThat(storage.findAll()).hasSize(1);
    var entry = storage.findAll().get(0);
    assertThat(entry.getToolName()).isEqualTo("getWeather");
    assertThat(entry.getArguments()).isEqualTo("{\"city\":\"Lucerne\"}");
    assertThat(entry.getResultText()).isEqualTo("Sunny, 25°C");
    assertThat(entry.getAgentId()).isNotBlank();
    assertThat(entry.getCaseUuid()).isEqualTo("case-1");
    assertThat(entry.getTaskUuid()).isEqualTo("task-1");
    assertThat(entry.getExecutedAt()).isNotNull();
  }

  @Test
  void onEventRecordsMultipleToolCallsSeparately() {
    var invocationId = UUID.randomUUID();
    listener.onEvent(buildEvent(invocationId, "getWeather", "{\"city\":\"Lucerne\"}", "Sunny"));
    listener.onEvent(buildEvent(invocationId, "getTime", "{}", "12:00"));

    assertThat(storage.findAll()).hasSize(2);
    assertThat(storage.findAll().get(0).getToolName()).isEqualTo("getWeather");
    assertThat(storage.findAll().get(1).getToolName()).isEqualTo("getTime");
    assertThat(storage.findAll().get(0).getAgentId())
        .isEqualTo(storage.findAll().get(1).getAgentId());
  }

  private ToolExecutedEvent buildEvent(String toolName, String arguments, String resultText) {
    return buildEvent(UUID.randomUUID(), toolName, arguments, resultText);
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
