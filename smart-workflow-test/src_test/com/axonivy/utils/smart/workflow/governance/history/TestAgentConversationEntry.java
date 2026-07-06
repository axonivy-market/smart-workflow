package com.axonivy.utils.smart.workflow.governance.history;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestAgentConversationEntry {

  @Test
  void agentNameAndProcessName_setAndGetCorrectly() {
    var entry = new AgentConversationEntry();
    entry.setAgentName("My Agent");
    entry.setProcessName("My Process");
    assertThat(entry.getAgentName()).isEqualTo("My Agent");
    assertThat(entry.getProcessName()).isEqualTo("My Process");
  }
}
