package com.axonivy.utils.smart.workflow.governance.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.primefaces.model.TreeNode;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.ui.HistoryTreeBuilder;
import com.axonivy.utils.smart.workflow.governance.ui.entity.CaseHistoryGroup;
import com.axonivy.utils.smart.workflow.governance.ui.entity.TaskHistoryGroup;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestHistoryTreeBuilder {

  private static final LocalDateTime T0 = LocalDateTime.of(2025, 1, 1, 12, 0, 0);

  @Test
  void build_noEntries_returnsEmptyRoot() {
    TreeNode<Object> root = HistoryTreeBuilder.build(List.of());
    assertThat(root.getType()).isEqualTo("root");
    assertThat(root.getChildCount()).isZero();
  }

  @Test
  void build_singleEntry_createsCorrectHierarchy() {
    var entry = chatEntry("agent-1", "case-1", "task-1", T0);
    TreeNode<Object> root = HistoryTreeBuilder.build(List.of(entry));

    assertThat(root.getChildCount()).isOne();
    TreeNode<Object> caseNode = root.getChildren().get(0);
    assertThat(caseNode.getType()).isEqualTo("case");
    assertThat(caseNode.getData()).isInstanceOf(CaseHistoryGroup.class);
    assertThat(caseNode.isExpanded()).isFalse();

    assertThat(caseNode.getChildCount()).isOne();
    TreeNode<Object> taskNode = caseNode.getChildren().get(0);
    assertThat(taskNode.getType()).isEqualTo("task");
    assertThat(taskNode.getData()).isInstanceOf(TaskHistoryGroup.class);
    assertThat(taskNode.isExpanded()).isFalse();

    assertThat(taskNode.getChildCount()).isOne();
    TreeNode<Object> agentNode = taskNode.getChildren().get(0);
    assertThat(agentNode.getType()).isEqualTo("agent");
    assertThat(agentNode.getData()).isSameAs(entry);
  }

  @Test
  void build_multipleCasesAndTasks_correctCounts() {
    var a1 = chatEntry("agent-a1", "case-1", "task-1", T0);
    var a2 = chatEntry("agent-a2", "case-1", "task-2", T0.plusSeconds(1));
    var a3 = chatEntry("agent-a3", "case-2", "task-3", T0.plusSeconds(2));
    TreeNode<Object> root = HistoryTreeBuilder.build(List.of(a1, a2, a3));

    assertThat(root.getChildCount()).isEqualTo(2);
    TreeNode<Object> case1 = root.getChildren().stream()
        .filter(n -> ((CaseHistoryGroup) n.getData()).getCaseUuid().equals("case-1"))
        .findFirst().orElseThrow();
    assertThat(case1.getChildCount()).isEqualTo(2);
  }

  private static AgentConversationEntry chatEntry(String agentId, String caseUuid, String taskUuid,
      LocalDateTime lastUpdated) {
    var e = new AgentConversationEntry();
    e.setAgentId(agentId);
    e.setCaseUuid(caseUuid);
    e.setTaskUuid(taskUuid);
    e.setLastUpdated(lastUpdated.toString());
    return e;
  }
}
