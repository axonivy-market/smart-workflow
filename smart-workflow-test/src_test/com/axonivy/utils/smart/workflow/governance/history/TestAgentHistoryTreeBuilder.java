package com.axonivy.utils.smart.workflow.governance.history;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.ToolExecution;
import com.axonivy.utils.smart.workflow.governance.history.internal.AgentHistoryTreeBuilder;
import com.axonivy.utils.smart.workflow.governance.history.internal.AgentHistoryTreeBuilder.AgentNode;
import com.axonivy.utils.smart.workflow.governance.history.internal.AgentHistoryTreeBuilder.CaseNode;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestAgentHistoryTreeBuilder {

  private static final LocalDateTime T0 = LocalDateTime.of(2025, 1, 1, 12, 0, 0);

  @Test
  void allAgentsFlatWithToolsAttached() {
    var tool1 = toolExecution("extractHeaderInfo", T0.plusSeconds(3));
    var tool2 = toolExecution("assessCompliance", T0.plusSeconds(6));
    var orchestrator = chatEntry("orchestrator", T0.plusSeconds(10), List.of(tool1, tool2));
    var sub1 = chatEntry("sub1", T0.plusSeconds(2));
    var sub2 = chatEntry("sub2", T0.plusSeconds(5));

    List<CaseNode> cases = AgentHistoryTreeBuilder.buildTree(List.of(orchestrator, sub1, sub2));

    assertThat(cases).hasSize(1);
    List<AgentNode> agents = cases.get(0).tasks().get(0).agents();

    assertThat(agents).hasSize(3);
    var orchestratorNode = agents.stream()
        .filter(n -> n.chat() == orchestrator).findFirst().orElseThrow();
    assertThat(orchestratorNode.tools()).containsExactly(tool1, tool2);
    assertThat(agents.stream().filter(n -> n.chat() == sub1).findFirst()).isPresent();
    assertThat(agents.stream().filter(n -> n.chat() == sub2).findFirst()).isPresent();
  }

  @Test
  void agentsSortedByLastUpdatedAsc() {
    var tool = toolExecution("extractHeaderInfo", T0.plusSeconds(5));
    var orchestrator = chatEntry("orchestrator", T0.plusSeconds(10), List.of(tool));
    var first = chatEntry("first", T0);
    var second = chatEntry("second", T0.plusSeconds(4));

    List<CaseNode> cases = AgentHistoryTreeBuilder.buildTree(List.of(orchestrator, first, second));

    assertThat(cases).hasSize(1);
    List<AgentNode> agents = cases.get(0).tasks().get(0).agents();
    assertThat(agents).hasSize(3);
    assertThat(agents.get(0).chat()).isEqualTo(first);
    assertThat(agents.get(1).chat()).isEqualTo(second);
    assertThat(agents.get(2).chat()).isEqualTo(orchestrator);
  }

  @Test
  void sameCaseDifferentTasksGroupedUnderCase() {
    var agentA = chatEntry("agent-a", "case-1", "task-1", T0.plusSeconds(2));
    var agentB = chatEntry("agent-b", "case-1", "task-2", T0.plusSeconds(5));
    var agentC = chatEntry("agent-c", "case-2", "task-3", T0.plusSeconds(3));

    List<CaseNode> cases = AgentHistoryTreeBuilder.buildTree(List.of(agentA, agentB, agentC));

    assertThat(cases).hasSize(2);

    CaseNode case1 = cases.stream()
        .filter(c -> "case-1".equals(c.caseUuid())).findFirst().orElseThrow();
    assertThat(case1.tasks()).hasSize(2);

    CaseNode case2 = cases.stream()
        .filter(c -> "case-2".equals(c.caseUuid())).findFirst().orElseThrow();
    assertThat(case2.tasks()).hasSize(1);
  }

  @Test
  void toolExecutionsAttachedToCorrectAgent() {
    var tool1 = toolExecution("toolA", T0.plusSeconds(11));
    var tool2 = toolExecution("toolB", T0.plusSeconds(12));
    var orchestrator = chatEntry("orchestrator", T0.plusSeconds(15), List.of(tool1, tool2));
    var ocr = chatEntry("ocr", T0.plusSeconds(1));
    var sub = chatEntry("sub", T0.plusSeconds(8));

    List<CaseNode> cases = AgentHistoryTreeBuilder.buildTree(List.of(ocr, orchestrator, sub));

    List<AgentNode> agents = cases.get(0).tasks().get(0).agents();
    assertThat(agents).hasSize(3);

    var orchestratorNode = agents.stream()
        .filter(n -> "orchestrator".equals(n.chat().getAgentId())).findFirst().orElseThrow();
    assertThat(orchestratorNode.tools()).containsExactly(tool1, tool2);

    var ocrNode = agents.stream()
        .filter(n -> "ocr".equals(n.chat().getAgentId())).findFirst().orElseThrow();
    assertThat(ocrNode.tools()).isEmpty();
  }

  private static AgentConversationEntry chatEntry(String agentId, LocalDateTime lastUpdated) {
    return chatEntry(agentId, "case-1", "task-1", lastUpdated);
  }

  private static AgentConversationEntry chatEntry(String agentId, LocalDateTime lastUpdated,
      List<ToolExecution> tools) {
    var e = chatEntry(agentId, "case-1", "task-1", lastUpdated);
    e.setToolExecutions(tools);
    return e;
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

  private static ToolExecution toolExecution(String toolName, LocalDateTime executedAt) {
    return new ToolExecution(toolName, null, null, executedAt.toString());
  }
}
