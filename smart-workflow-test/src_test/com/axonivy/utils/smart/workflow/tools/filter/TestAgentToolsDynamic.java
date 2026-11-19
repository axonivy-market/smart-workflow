package com.axonivy.utils.smart.workflow.tools.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.axonivy.utils.smart.workflow.model.dummy.DummyChatModelProvider;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmElement;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.test.log.LoggerAccess;
import dev.langchain4j.agent.tool.ToolSpecification;

@IvyProcessTest
class TestAgentToolsDynamic {

  private static final BpmElement TOOLS_DYNAMIC = BpmProcess.name("TestToolUser").elementName("toolsDynamic(Object)");

  private static final String TICKET_SIGNATURE = "createSupportTicket(String)";

  @RegisterExtension
  LoggerAccess log = new LoggerAccess();

  private final List<String> toolsSentToModel = new ArrayList<>();

  @BeforeEach
  void setup() {
    DummyChatModelProvider.defineChatText(request -> {
      request.toolSpecifications().stream()
          .map(ToolSpecification::name)
          .forEach(toolsSentToModel::add);
      return "done";
    });
  }

  @Test
  void list(BpmClient client) {
    run(client, List.of(TICKET_SIGNATURE, "whoami"));
    assertThat(toolsSentToModel).containsExactlyInAnyOrder("add", "createSupportTicket", "whoami");
  }

  @Test
  void set(BpmClient client) {
    run(client, Set.of(TICKET_SIGNATURE, "whoami"));
    assertThat(toolsSentToModel).containsExactlyInAnyOrder("add", "createSupportTicket", "whoami");
  }

  @Test
  void array(BpmClient client) {
    run(client, new String[] {TICKET_SIGNATURE, "whoami"});
    assertThat(toolsSentToModel).containsExactlyInAnyOrder("add", "createSupportTicket", "whoami");
  }

  @Test
  void singleString(BpmClient client) {
    run(client, TICKET_SIGNATURE);
    assertThat(toolsSentToModel).containsExactlyInAnyOrder("add", "createSupportTicket");
  }

  @Test
  void repeatedTool_isSentOnce(BpmClient client) {
    run(client, List.of("add", "whoami", "whoami"));
    assertThat(toolsSentToModel).containsExactlyInAnyOrder("add", "whoami");
  }

  @Test
  void emptyValue_keepsSelectedTools(BpmClient client) {
    run(client, List.of());
    assertThat(toolsSentToModel).containsExactly("add");
  }

  @Test
  void nullValue_keepsSelectedTools(BpmClient client) {
    run(client, null);
    assertThat(toolsSentToModel).containsExactly("add");
  }

  @Test
  void wrongType_fails(BpmClient client) {
    assertThatThrownBy(() -> run(client, 42))
        .hasStackTraceContaining("must be a Collection, an array or a String");
  }

  @Test
  void nonStringEntry_fails(BpmClient client) {
    assertThatThrownBy(() -> run(client, List.of(42)))
        .hasStackTraceContaining("must only contain Strings");
  }

  @Test
  void unknownTool_isSkippedWithWarning(BpmClient client) {
    run(client, List.of("unknownTool", "whoami"));
    assertThat(toolsSentToModel).containsExactlyInAnyOrder("add", "whoami");
    assertThat(log.warnings()).contains("Runtime tools not found: [unknownTool]");
  }

  private static void run(BpmClient client, Object tools) {
    client.start().subProcess(TOOLS_DYNAMIC).withParam("tools", tools).execute();
  }
}
