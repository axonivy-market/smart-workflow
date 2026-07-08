package com.axonivy.utils.smart.workflow.governance.ui.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.ToolExecution;
import com.axonivy.utils.smart.workflow.governance.history.internal.ChatHistoryJsonParser.ArgumentEntry;
import com.axonivy.utils.smart.workflow.governance.utils.DatePatternUtils;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestAgentTreeNode {

  private static final LocalDateTime T0 = LocalDateTime.of(2025, 4, 10, 9, 0, 0);

  @Test
  void getAgentId_withId_returnsId() {
    assertThat(new AgentTreeNode(entry("my-agent", T0)).getAgentId()).isEqualTo("my-agent");
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(value = {
      "null agentId,  NULL",
      "blank agentId, '   '"
  }, nullValues = "NULL")
  void getAgentId_missingId_returnsDefaultLabel(String testName, String agentId) {
    var e = new AgentConversationEntry();
    e.setAgentId(agentId);
    assertThat(new AgentTreeNode(e).getAgentId()).as(testName).isEqualTo("Agent");
  }

  @Test
  void getToolCount_matchesToolExecutionsSize() {
    assertThat(new AgentTreeNode(entry("agent-1", T0)).getToolCount()).isEqualTo(0);
    var e = entry("agent-1", T0);
    e.setToolExecutions(List.of(
        new ToolExecution("toolA", null, null, null),
        new ToolExecution("toolB", null, null, null)));
    assertThat(new AgentTreeNode(e).getToolCount()).isEqualTo(2);
  }

  @Test
  void lastUpdated_validDate_parsedAndFormatted() {
    var node = new AgentTreeNode(entry("agent-1", T0));
    assertThat(node.getLastUpdated()).isEqualTo(T0);
    assertThat(node.getLastUpdatedText()).isEqualTo(T0.format(DatePatternUtils.DISPLAY_FMT));
  }

  @Test
  void lastUpdated_noDate_returnsNullAndDash() {
    var node = new AgentTreeNode(new AgentConversationEntry());
    assertThat(node.getLastUpdated()).isNull();
    assertThat(node.getLastUpdatedText()).isEqualTo("—");
  }

  @Test
  void getTools_nullExecutions_returnsEmpty() {
    assertThat(new AgentTreeNode(entry("agent-1", T0)).getTools()).isEmpty();
  }

  @Test
  void getTools_withExecutions_exposesToolDataAndArgumentEntries() {
    var e = entry("agent-1", T0);
    e.setToolExecutions(List.of(
        new ToolExecution("searchDocs", "{\"query\":\"hello\",\"limit\":\"10\"}", "found", "2025-04-10T09:00:01")));
    var tools = new AgentTreeNode(e).getTools();
    assertThat(tools).hasSize(1);
    assertThat(tools.get(0).getToolName()).isEqualTo("searchDocs");
    assertThat(tools.get(0).getResultText()).isEqualTo("found");
    assertThat(tools.get(0).getExecutedAt()).isEqualTo("2025-04-10T09:00:01");

    List<ArgumentEntry> entries = tools.get(0).getArgumentEntries();
    assertThat(entries).hasSize(2);
    assertThat(entries).extracting(ArgumentEntry::getKey).containsExactly("query", "limit");
    assertThat(entries).extracting(ArgumentEntry::getValue).containsExactly("hello", "10");
  }

  @Test
  void getTools_noArguments_argumentEntriesEmpty() {
    var e = entry("agent-1", T0);
    e.setToolExecutions(List.of(new ToolExecution("noArgTool", null, "result", null)));
    assertThat(new AgentTreeNode(e).getTools().get(0).getArgumentEntries()).isEmpty();
  }

  private static AgentConversationEntry entry(String agentId, LocalDateTime lastUpdated) {
    var e = new AgentConversationEntry();
    e.setAgentId(agentId);
    e.setLastUpdated(lastUpdated != null ? lastUpdated.toString() : null);
    return e;
  }
}
