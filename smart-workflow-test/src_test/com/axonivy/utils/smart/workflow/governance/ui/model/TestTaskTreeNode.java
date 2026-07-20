package com.axonivy.utils.smart.workflow.governance.ui.model;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.utils.DatePatternUtils;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestTaskTreeNode {

  private static final LocalDateTime T0 = LocalDateTime.of(2025, 1, 1, 12, 0, 0);

  @Test
  void agentCount_matchesNumberOfEntries() {
    assertThat(new TaskTreeNode(entry("task-1", T0)).getAgentCount()).isEqualTo(1);
    assertThat(new TaskTreeNode(List.of(
        entry("task-1", T0),
        entry("task-1", T0.plusSeconds(1)))).getAgentCount()).isEqualTo(2);
  }

  @Test
  void getTaskUuid_returnsFirstEntryTaskUuid() {
    assertThat(new TaskTreeNode(entry("task-42", T0)).getTaskUuid()).isEqualTo("task-42");
  }

  @Test
  void getMessageCount_sumsAcrossAllEntries() {
    var node = new TaskTreeNode(List.of(
        entryWithMessages("[{\"type\":\"USER\"},{\"type\":\"AI\"}]"),
        entryWithMessages("[{\"type\":\"USER\"}]")));
    assertThat(node.getMessageCount()).isEqualTo(3);
  }

  @Test
  void getTotalTokens_sumsAcrossAllEntries() {
    var node = new TaskTreeNode(List.of(
        entryWithTokens("[{\"totalTokens\":10,\"modelName\":\"m\"}]"),
        entryWithTokens("[{\"totalTokens\":25,\"modelName\":\"m\"}]")));
    assertThat(node.getTotalTokens()).isEqualTo(35);
  }

  @Test
  void getAvgDurationMs_averagesNonZeroDurations() {
    var node = new TaskTreeNode(List.of(
        entryWithTokens("[{\"durationMs\":400,\"modelName\":\"m\"}]"),
        entryWithTokens("[{\"durationMs\":600,\"modelName\":\"m\"}]")));
    assertThat(node.getAvgDurationMs()).isEqualTo(500);
  }

  @Test
  void getAvgDurationMs_zeroDurationEntriesExcluded() {
    var node = new TaskTreeNode(List.of(
        entryWithTokens("[{\"totalTokens\":10,\"modelName\":\"m\"}]"),
        entryWithTokens("[{\"durationMs\":600,\"modelName\":\"m\"}]")));
    assertThat(node.getAvgDurationMs()).isEqualTo(600);
  }

  @Test
  void getAvgDurationMs_noDurations_returnsZero() {
    assertThat(new TaskTreeNode(entry("task-1", T0)).getAvgDurationMs()).isEqualTo(0);
  }

  @Test
  void getLastUpdated_returnsMaxAcrossEntriesAndFormatsCorrectly() {
    var node = new TaskTreeNode(List.of(
        entry("task-1", T0),
        entry("task-1", T0.plusHours(3))));
    assertThat(node.getLastUpdated()).isEqualTo(T0.plusHours(3));
    assertThat(node.getLastUpdatedText()).isEqualTo(T0.plusHours(3).format(DatePatternUtils.DISPLAY_FMT));
  }

  @Test
  void getLastUpdated_noDate_returnsNullAndDash() {
    var e = new AgentConversationEntry();
    e.setTaskUuid("task-1");
    var node = new TaskTreeNode(e);
    assertThat(node.getLastUpdated()).isNull();
    assertThat(node.getLastUpdatedText()).isEqualTo("—");
  }

  private static AgentConversationEntry entry(String taskUuid, LocalDateTime lastUpdated) {
    var e = new AgentConversationEntry();
    e.setTaskUuid(taskUuid);
    e.setLastUpdated(lastUpdated != null ? lastUpdated.toString() : null);
    return e;
  }

  private static AgentConversationEntry entryWithMessages(String messagesJson) {
    var e = new AgentConversationEntry();
    e.setTaskUuid("task-1");
    e.setMessagesJson(messagesJson);
    return e;
  }

  private static AgentConversationEntry entryWithTokens(String tokenUsageJson) {
    var e = new AgentConversationEntry();
    e.setTaskUuid("task-1");
    e.setTokenUsageJson(tokenUsageJson);
    return e;
  }
}
