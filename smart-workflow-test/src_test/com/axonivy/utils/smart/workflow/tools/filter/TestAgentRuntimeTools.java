package com.axonivy.utils.smart.workflow.tools.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.model.dummy.DummyChatModelProvider;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import dev.langchain4j.agent.tool.ToolSpecification;

@IvyProcessTest
class TestAgentRuntimeTools {

  private static final BpmProcess AGENT_TOOLS = BpmProcess.name("TestToolUser");

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
    client.start().process(AGENT_TOOLS.elementName("runtimeToolsList")).execute();
    assertThat(toolsSentToModel).containsExactlyInAnyOrder("createSupportTicket", "whoami");
  }

  @Test
  void set(BpmClient client) {
    client.start().process(AGENT_TOOLS.elementName("runtimeToolsSet")).execute();
    assertThat(toolsSentToModel).containsExactlyInAnyOrder("createSupportTicket", "whoami");
  }

  @Test
  void array(BpmClient client) {
    client.start().process(AGENT_TOOLS.elementName("runtimeToolsArray")).execute();
    assertThat(toolsSentToModel).containsExactlyInAnyOrder("createSupportTicket", "whoami");
  }

  @Test
  void singleString(BpmClient client) {
    client.start().process(AGENT_TOOLS.elementName("runtimeToolsString")).execute();
    assertThat(toolsSentToModel).containsExactly("createSupportTicket");
  }
}
