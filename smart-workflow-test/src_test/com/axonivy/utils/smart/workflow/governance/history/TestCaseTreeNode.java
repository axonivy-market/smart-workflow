package com.axonivy.utils.smart.workflow.governance.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.ui.model.CaseTreeNode;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestCaseTreeNode {

  private static final LocalDateTime T0 = LocalDateTime.of(2025, 1, 1, 12, 0, 0);

  @Test
  void buildTree_null_returnsEmpty() {
    assertThat(CaseTreeNode.buildTree(null)).isEmpty();
  }

  @Test
  void buildTree_emptyList_returnsEmpty() {
    assertThat(CaseTreeNode.buildTree(List.of())).isEmpty();
  }

  @Test
  void buildTree_singleEntry_createsOneNode() {
    var entries = List.of(entry("case-1", "task-1", "agent-1", T0));
    var nodes = CaseTreeNode.buildTree(entries);

    assertThat(nodes).hasSize(1);
    var caseNode = nodes.get(0);
    assertThat(caseNode.getCaseUuid()).isEqualTo("case-1");
    assertThat(caseNode.getTasks()).hasSize(1);
  }

  @Test
  void buildTree_sameCaseDifferentTasks_groupedUnderOneCase() {
    var entries = List.of(
        entry("case-1", "task-1", "agent-1", T0),
        entry("case-1", "task-2", "agent-2", T0.plusHours(1)),
        entry("case-2", "task-3", "agent-3", T0.plusHours(2))
    );
    var nodes = CaseTreeNode.buildTree(entries);

    assertThat(nodes).hasSize(2);
    var case1 = nodes.stream()
        .filter(n -> "case-1".equals(n.getCaseUuid()))
        .findFirst().orElseThrow();
    assertThat(case1.getTasks()).hasSize(2);

    var case2 = nodes.stream()
        .filter(n -> "case-2".equals(n.getCaseUuid()))
        .findFirst().orElseThrow();
    assertThat(case2.getTasks()).hasSize(1);
  }

  @Test
  void buildTree_sortsByLastUpdatedDescending() {
    var olderEntry = entry("case-old", "task-1", "agent-1", T0);
    var newerEntry = entry("case-new", "task-1", "agent-1", T0.plusHours(3));
    var nodes = CaseTreeNode.buildTree(List.of(olderEntry, newerEntry));

    assertThat(nodes).hasSize(2);
    assertThat(nodes.get(0).getCaseUuid()).isEqualTo("case-new");
    assertThat(nodes.get(1).getCaseUuid()).isEqualTo("case-old");
  }

  @Test
  void getTaskCount_reflectsDistinctTasks() {
    var entries = List.of(
        entry("case-1", "task-1", "agent-1", T0),
        entry("case-1", "task-2", "agent-2", T0.plusHours(1))
    );
    var nodes = CaseTreeNode.buildTree(entries);

    assertThat(nodes).hasSize(1);
    assertThat(nodes.get(0).getTaskCount()).isEqualTo(2);
  }

  @Test
  void getLastUpdated_returnsMaxAcrossTasks() {
    var entries = List.of(
        entry("case-1", "task-1", "agent-1", T0),
        entry("case-1", "task-2", "agent-2", T0.plusHours(2))
    );
    var nodes = CaseTreeNode.buildTree(entries);

    assertThat(nodes).hasSize(1);
    assertThat(nodes.get(0).getLastUpdated()).isEqualTo(T0.plusHours(2));
  }

  @Test
  void getTopModelName_returnsLatestTaskModel() {
    var oldTokenUsage = "[{\"totalTokens\":10,\"inputTokens\":5,\"outputTokens\":5,\"modelName\":\"model-old\",\"durationMs\":100}]";
    var newTokenUsage = "[{\"totalTokens\":10,\"inputTokens\":5,\"outputTokens\":5,\"modelName\":\"model-new\",\"durationMs\":100}]";
    var entries = List.of(
        entryWithTokenUsage("case-1", "task-1", "agent-1", T0, oldTokenUsage),
        entryWithTokenUsage("case-1", "task-2", "agent-2", T0.plusHours(1), newTokenUsage)
    );
    var nodes = CaseTreeNode.buildTree(entries);

    assertThat(nodes).hasSize(1);
    assertThat(nodes.get(0).getTopModelName()).isEqualTo("model-new");
  }

  @Test
  void expanded_defaultTrue() {
    var nodes = CaseTreeNode.buildTree(List.of(entry("agent-1", T0)));

    assertThat(nodes).hasSize(1);
    assertThat(nodes.get(0).isExpanded()).isTrue();
  }

  // --- factory helpers ---

  private static AgentConversationEntry entry(String caseUuid, String taskUuid, String agentId, LocalDateTime lastUpdated) {
    var e = new AgentConversationEntry();
    e.setCaseUuid(caseUuid);
    e.setTaskUuid(taskUuid);
    e.setAgentId(agentId);
    e.setLastUpdated(lastUpdated.toString());
    return e;
  }

  private static AgentConversationEntry entryWithTokenUsage(String caseUuid, String taskUuid, String agentId, LocalDateTime lastUpdated, String tokenUsageJson) {
    var e = entry(caseUuid, taskUuid, agentId, lastUpdated);
    e.setTokenUsageJson(tokenUsageJson);
    return e;
  }

  private static AgentConversationEntry entry(String agentId, LocalDateTime lastUpdated) {
    return entry("case-1", "task-1", agentId, lastUpdated);
  }
}
