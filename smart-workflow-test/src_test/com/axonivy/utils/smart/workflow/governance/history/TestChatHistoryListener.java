package com.axonivy.utils.smart.workflow.governance.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.listener.ChatHistoryListener;
import com.axonivy.utils.smart.workflow.governance.history.recorder.internal.ChatHistoryRepository;

import dev.langchain4j.data.message.UserMessage;

public class TestChatHistoryListener {

  @Test
  void agentId_isDeterministicForSameAgentName() {
    String agentName = "MyOrchestrator";
    String id1 = UUID.nameUUIDFromBytes(agentName.getBytes(StandardCharsets.UTF_8)).toString();
    String id2 = UUID.nameUUIDFromBytes(agentName.getBytes(StandardCharsets.UTF_8)).toString();
    assertThat(id1).isEqualTo(id2);
  }

  @Test
  void agentId_isDifferentForDifferentAgentNames() {
    String id1 = UUID.nameUUIDFromBytes("AgentA".getBytes(StandardCharsets.UTF_8)).toString();
    String id2 = UUID.nameUUIDFromBytes("AgentB".getBytes(StandardCharsets.UTF_8)).toString();
    assertThat(id1).isNotEqualTo(id2);
  }


  @Test
  void deterministicAgentId_mergesEntriesAcrossRepositoryInstances() {
    String agentName = "MyOrchestrator";
    String agentId = UUID.nameUUIDFromBytes(agentName.getBytes(StandardCharsets.UTF_8)).toString();
    var storage = new InMemoryHistoryStorage();

    var repo1 = new ChatHistoryRepository("case-1", "task-1", agentId, agentName, "proc", storage);
    repo1.store(List.of(UserMessage.from("Hello")), null);
    assertThat(storage.findAll()).hasSize(1);

    var repo2 = new ChatHistoryRepository("case-1", "task-1", agentId, agentName, "proc", storage);
    repo2.store(List.of(UserMessage.from("Hello"), UserMessage.from("Follow-up")), null);

    assertThat(storage.findAll()).hasSize(1);
    assertThat(storage.findAll().get(0).getAgentName()).isEqualTo(agentName);
  }

  @Test
  void differentAgentIds_createSeparateEntries() {
    String idA = UUID.nameUUIDFromBytes("AgentA".getBytes(StandardCharsets.UTF_8)).toString();
    String idB = UUID.nameUUIDFromBytes("AgentB".getBytes(StandardCharsets.UTF_8)).toString();
    var storage = new InMemoryHistoryStorage();

    new ChatHistoryRepository("case-1", "task-1", idA, "AgentA", "proc", storage)
        .store(List.of(UserMessage.from("Hello from A")), null);
    new ChatHistoryRepository("case-1", "task-1", idB, "AgentB", "proc", storage)
        .store(List.of(UserMessage.from("Hello from B")), null);

    assertThat(storage.findAll()).hasSize(2);
  }

  @Test
  void chatHistoryListener_constructorAcceptsAgentName() {
    var listener = new ChatHistoryListener("TestAgent");
    assertThat(listener).isNotNull();
  }

  @Test
  void chatHistoryListener_constructorAcceptsBlankAgentName() {
    var listener = new ChatHistoryListener("");
    assertThat(listener).isNotNull();
  }
}
