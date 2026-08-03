package com.axonivy.utils.smart.workflow.governance.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.ui.model.TaskTreeNode;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestTaskTreeNode {

  private static final LocalDateTime T0 = LocalDateTime.of(2025, 1, 1, 12, 0, 0);

  @Test
  void constructor_emptyList_throwsIllegalArgument() {
    assertThatThrownBy(() -> new TaskTreeNode(List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void constructor_nullList_throwsIllegalArgument() {
    assertThatThrownBy(() -> new TaskTreeNode((List<AgentConversationEntry>) null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void getEntry_returnsSortedFirst() {
    var later = entry("agent-later", T0.plusHours(1));
    var earlier = entry("agent-earlier", T0);
    var node = new TaskTreeNode(List.of(later, earlier));

    assertThat(node.getEntry().getAgentId()).isEqualTo("agent-earlier");
  }

  @Test
  void getAgentCount_reflectsNumberOfEntries() {
    var node = new TaskTreeNode(List.of(
        entry("agent-1", T0),
        entry("agent-2", T0.plusHours(1))
    ));

    assertThat(node.getAgentCount()).isEqualTo(2);
  }

  @Test
  void getMessageCount_skipsBrokenEntries() {
    var withMessages = entry("agent-1", T0);
    withMessages.setMessagesJson("[{},{}]");

    var withNullMessages = entry("agent-2", T0.plusHours(1));

    var node = new TaskTreeNode(List.of(withMessages, withNullMessages));

    assertThat(node.getMessageCount()).isEqualTo(2);
  }

  @Test
  void getLastUpdated_returnsMaxAcrossEntries() {
    var node = new TaskTreeNode(List.of(
        entry("agent-1", T0),
        entry("agent-2", T0.plusHours(3))
    ));

    assertThat(node.getLastUpdated()).isEqualTo(T0.plusHours(3));
  }

  @Test
  void getLastUpdated_allNullLastUpdated_returnsNull() {
    var e = new AgentConversationEntry();
    e.setAgentId("agent-1");
    e.setCaseUuid("case-1");
    e.setTaskUuid("task-1");

    var node = new TaskTreeNode(List.of(e));

    assertThat(node.getLastUpdated()).isNull();
  }

  private static AgentConversationEntry entry(String agentId, LocalDateTime lastUpdated) {
    var e = new AgentConversationEntry();
    e.setAgentId(agentId);
    e.setCaseUuid("case-1");
    e.setTaskUuid("task-1");
    e.setLastUpdated(lastUpdated.toString());
    return e;
  }
}
