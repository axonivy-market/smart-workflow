package com.axonivy.utils.smart.workflow.governance.history;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.entity.ToolExecutionEntry;
import com.axonivy.utils.smart.workflow.governance.history.internal.AgentHistoryTreeBuilder;
import com.axonivy.utils.smart.workflow.governance.history.internal.AgentHistoryTreeBuilder.AgentNode;
import com.axonivy.utils.smart.workflow.governance.history.internal.AgentHistoryTreeBuilder.CaseNode;
import com.axonivy.utils.smart.workflow.governance.history.internal.AgentHistoryTreeBuilder.TaskNode;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestAgentHistoryTreeBuilder {

  private static final LocalDateTime T0 = LocalDateTime.of(2025, 1, 1, 12, 0, 0);

  @Test
  void createsCorrectHierarchy() {
    var parent = chatEntry("parent", T0.plusSeconds(10));
    var sub1 = chatEntry("sub1", T0.plusSeconds(2));
    var sub2 = chatEntry("sub2", T0.plusSeconds(5));

    var tool2 = toolEntry("parent", "assessCompliance", T0.plusSeconds(6));
    var tool1 = toolEntry("parent", "extractHeaderInfo", T0.plusSeconds(3));

    List<CaseNode> cases = AgentHistoryTreeBuilder.buildTree(
        List.of(parent, sub1, sub2),
        List.of(tool2, tool1));

    assertThat(cases).hasSize(1);
    List<AgentNode> agents = cases.get(0).tasks().get(0).agents();

    assertThat(agents).hasSize(1);
    AgentNode root = agents.get(0);
    assertThat(root.chat()).isEqualTo(parent);
    assertThat(root.tools()).containsExactly(tool1, tool2);
    assertThat(root.children()).hasSize(2);
    assertThat(root.children().get(0).chat()).isEqualTo(sub1);
    assertThat(root.children().get(1).chat()).isEqualTo(sub2);
  }

  @Test
  void standaloneAgent() {
    var standalone = chatEntry("standalone", T0);
    var parent = chatEntry("parent", T0.plusSeconds(10));
    var realSub = chatEntry("realSub", T0.plusSeconds(4));

    var tool = toolEntry("parent", "extractHeaderInfo", T0.plusSeconds(5));

    List<CaseNode> cases = AgentHistoryTreeBuilder.buildTree(
        List.of(standalone, parent, realSub),
        List.of(tool));

    assertThat(cases).hasSize(1);
    List<AgentNode> agents = cases.get(0).tasks().get(0).agents();
    assertThat(agents).hasSize(2);

    var standaloneNode = agents.stream()
        .filter(n -> n.chat() == standalone).findFirst().orElseThrow();
    assertThat(standaloneNode.tools()).isEmpty();
    assertThat(standaloneNode.children()).isEmpty();

    var parentNode = agents.stream()
        .filter(n -> n.chat() == parent).findFirst().orElseThrow();
    assertThat(parentNode.children()).hasSize(1);
    assertThat(parentNode.children().get(0).chat()).isEqualTo(realSub);
  }

  @Test
  void nullTaskUuidDefaultsToMinusOne() {
    var entry = new AgentConversationEntry();
    entry.setAgentId("agent-1");
    entry.setCaseUuid("case-1");
    entry.setTaskUuid(null);
    entry.setLastUpdated(T0.toString());

    List<CaseNode> cases = AgentHistoryTreeBuilder.buildTree(List.of(entry), List.of());

    assertThat(cases).hasSize(1);
    List<TaskNode> tasks = cases.get(0).tasks();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).taskUuid()).isEqualTo("-1");
    assertThat(tasks.get(0).agents()).hasSize(1);
  }

  @Test
  void sameCaseDifferentTasksGroupedUnderCase() {
    var agentA = chatEntry("agent-a", "case-1", "task-1", T0.plusSeconds(2));
    var agentB = chatEntry("agent-b", "case-1", "task-2", T0.plusSeconds(5));
    var agentC = chatEntry("agent-c", "case-2", "task-3", T0.plusSeconds(3));

    List<CaseNode> cases = AgentHistoryTreeBuilder.buildTree(
        List.of(agentA, agentB, agentC), List.of());

    assertThat(cases).hasSize(2);

    CaseNode case1 = cases.stream()
        .filter(c -> "case-1".equals(c.caseUuid())).findFirst().orElseThrow();
    assertThat(case1.tasks()).hasSize(2);

    CaseNode case2 = cases.stream()
        .filter(c -> "case-2".equals(c.caseUuid())).findFirst().orElseThrow();
    assertThat(case2.tasks()).hasSize(1);
  }

  @Test
  void oneToOneMatchingPreventsDuplicates() {
    var ocr = chatEntry("ocr", T0.plusSeconds(1));
    var subA = chatEntry("subA", T0.plusSeconds(8));
    var subB = chatEntry("subB", T0.plusSeconds(9));
    var subC = chatEntry("subC", T0.plusSeconds(10));

    var tool1 = toolEntry("orchestrator", "toolA", T0.plusSeconds(11));
    var tool2 = toolEntry("orchestrator", "toolB", T0.plusSeconds(11).plusNanos(100_000));
    var tool3 = toolEntry("orchestrator", "toolC", T0.plusSeconds(11).plusNanos(200_000));

    List<CaseNode> cases = AgentHistoryTreeBuilder.buildTree(
        List.of(ocr, subA, subB, subC),
        List.of(tool1, tool2, tool3));

    List<AgentNode> agents = cases.get(0).tasks().get(0).agents();

    assertThat(agents).hasSize(1);
    var ocrNode = agents.stream()
        .filter(n -> "ocr".equals(n.chat().getAgentId())).findFirst();
    assertThat(ocrNode).isPresent();
    assertThat(ocrNode.get().children()).isEmpty();
  }


  private static AgentConversationEntry chatEntry(String agentId, LocalDateTime lastUpdated) {
    return chatEntry(agentId, "case-1", "task-1", lastUpdated);
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

  private static ToolExecutionEntry toolEntry(String agentId, String toolName,
      LocalDateTime executedAt) {
    var e = new ToolExecutionEntry();
    e.setAgentId(agentId);
    e.setCaseUuid("case-1");
    e.setTaskUuid("task-1");
    e.setToolName(toolName);
    e.setExecutedAt(executedAt.toString());
    return e;
  }
}
