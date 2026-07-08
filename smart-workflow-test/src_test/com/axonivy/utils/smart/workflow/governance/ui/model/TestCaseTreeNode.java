package com.axonivy.utils.smart.workflow.governance.ui.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestCaseTreeNode {

  private static final LocalDateTime T0 = LocalDateTime.of(2025, 1, 1, 12, 0, 0);

  @Test
  void buildTree_nullOrEmpty_returnsEmpty() {
    assertThat(CaseTreeNode.buildTree(null)).isEmpty();
    assertThat(CaseTreeNode.buildTree(List.of())).isEmpty();
  }

  @Test
  void buildTree_singleEntry_createsSingleCaseWithOneTask() {
    var nodes = CaseTreeNode.buildTree(List.of(entry("case-1", "task-1", T0)));
    assertThat(nodes).hasSize(1);
    assertThat(nodes.get(0).getCaseUuid()).isEqualTo("case-1");
    assertThat(nodes.get(0).getTasks()).hasSize(1);
  }

  @Test
  void buildTree_sameCaseDifferentTasks_groupedUnderSingleCase() {
    var nodes = CaseTreeNode.buildTree(List.of(
        entry("case-1", "task-1", T0),
        entry("case-1", "task-2", T0.plusSeconds(5))));
    assertThat(nodes).hasSize(1);
    assertThat(nodes.get(0).getTaskCount()).isEqualTo(2);
  }

  @Test
  void buildTree_differentCases_createsSeparateNodes() {
    var nodes = CaseTreeNode.buildTree(List.of(
        entry("case-1", "task-1", T0),
        entry("case-2", "task-2", T0.plusSeconds(10))));
    assertThat(nodes).hasSize(2);
  }

  @Test
  void buildTree_sortedByLastUpdatedDescending() {
    var nodes = CaseTreeNode.buildTree(List.of(
        entry("case-older", "task-1", T0),
        entry("case-newer", "task-2", T0.plusHours(1))));
    assertThat(nodes.get(0).getCaseUuid()).isEqualTo("case-newer");
    assertThat(nodes.get(1).getCaseUuid()).isEqualTo("case-older");
  }

  @Test
  void buildTree_nullCaseUuid_groupedUnderEmptyKey() {
    var nodes = CaseTreeNode.buildTree(List.of(
        entry(null, "task-1", T0),
        entry(null, "task-2", T0.plusSeconds(5))));
    assertThat(nodes).hasSize(1);
    assertThat(nodes.get(0).getCaseUuid()).isEmpty();
    assertThat(nodes.get(0).getTaskCount()).isEqualTo(2);
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(value = {
      "processName_firstNonBlank_returned, My Process, My Process",
      "processName_allNull_returnsEmpty,   NULL,       ''"
  }, nullValues = "NULL")
  void buildTree_processNameResolution(String testName, String processName, String expected) {
    var e = entry("case-1", "task-1", T0);
    e.setProcessName(processName);
    var nodes = CaseTreeNode.buildTree(List.of(e));
    assertThat(nodes.get(0).getProcessName()).as(testName).isEqualTo(expected);
  }

  @Test
  void getTotalMessages_sumsAcrossAllTasks() {
    var nodes = CaseTreeNode.buildTree(List.of(
        entryWithMessages("case-1", "task-1", "[{\"type\":\"USER\"},{\"type\":\"AI\"}]"),
        entryWithMessages("case-1", "task-2", "[{\"type\":\"USER\"}]")));
    assertThat(nodes.get(0).getTotalMessages()).isEqualTo(3);
  }

  @Test
  void getTotalTokens_sumsAcrossAllTasks() {
    var nodes = CaseTreeNode.buildTree(List.of(
        entryWithTokens("case-1", "task-1", "[{\"totalTokens\":10,\"modelName\":\"m\"}]"),
        entryWithTokens("case-1", "task-2", "[{\"totalTokens\":20,\"modelName\":\"m\"}]")));
    assertThat(nodes.get(0).getTotalTokens()).isEqualTo(30);
  }

  @Test
  void getLastUpdated_returnsMaxAcrossTasks() {
    var nodes = CaseTreeNode.buildTree(List.of(
        entry("case-1", "task-1", T0),
        entry("case-1", "task-2", T0.plusHours(2))));
    assertThat(nodes.get(0).getLastUpdated()).isEqualTo(T0.plusHours(2));
  }

  @Test
  void getLastUpdated_noValidDates_returnsNullAndDash() {
    var e = new AgentConversationEntry();
    e.setCaseUuid("case-1");
    e.setTaskUuid("task-1");
    var node = CaseTreeNode.buildTree(List.of(e)).get(0);
    assertThat(node.getLastUpdated()).isNull();
    assertThat(node.getLastUpdatedText()).isEqualTo("—");
  }

  private static AgentConversationEntry entry(String caseUuid, String taskUuid, LocalDateTime lastUpdated) {
    var e = new AgentConversationEntry();
    e.setCaseUuid(caseUuid);
    e.setTaskUuid(taskUuid);
    e.setLastUpdated(lastUpdated != null ? lastUpdated.toString() : null);
    return e;
  }

  private static AgentConversationEntry entryWithMessages(String caseUuid, String taskUuid, String messagesJson) {
    var e = new AgentConversationEntry();
    e.setCaseUuid(caseUuid);
    e.setTaskUuid(taskUuid);
    e.setMessagesJson(messagesJson);
    return e;
  }

  private static AgentConversationEntry entryWithTokens(String caseUuid, String taskUuid, String tokenUsageJson) {
    var e = new AgentConversationEntry();
    e.setCaseUuid(caseUuid);
    e.setTaskUuid(taskUuid);
    e.setTokenUsageJson(tokenUsageJson);
    return e;
  }
}
