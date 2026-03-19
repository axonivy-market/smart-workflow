package com.axonivy.utils.smart.workflow.governance.history;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.AgentHistoryCorrelator.AgentLink;
import com.axonivy.utils.smart.workflow.governance.history.AgentHistoryCorrelator.AgentNode;

public class TestAgentHistoryCorrelator {

  private static final LocalDateTime T0 = LocalDateTime.of(2025, 1, 1, 12, 0, 0);

  @Test
  void subAgentIsLinkedToItsToolEntry() {
    var parent = chatEntry("parent", T0.plusSeconds(5));
    var sub = chatEntry("sub1", T0.plusSeconds(2));

    var tool = toolEntry("parent", "extractHeaderInfo", T0.plusSeconds(3));

    List<AgentLink> links = AgentHistoryCorrelator.correlate(
        List.of(parent, sub),
        List.of(tool));

    assertThat(links).hasSize(1);
    assertThat(links.get(0).subAgent()).isEqualTo(sub);
    assertThat(links.get(0).triggerTool()).isEqualTo(tool);
    assertThat(links.get(0).triggerTool().getToolName()).isEqualTo("extractHeaderInfo");
  }

  @Test
  void multipleSubAgentsEachLinkedToCorrectTool() {
    var parent = chatEntry("parent", T0.plusSeconds(10));
    var sub1 = chatEntry("sub1", T0.plusSeconds(2));
    var sub2 = chatEntry("sub2", T0.plusSeconds(5));

    var tool1 = toolEntry("parent", "extractHeaderInfo", T0.plusSeconds(3));
    var tool2 = toolEntry("parent", "assessCompliance", T0.plusSeconds(6));

    List<AgentLink> links = AgentHistoryCorrelator.correlate(
        List.of(parent, sub1, sub2),
        List.of(tool1, tool2));

    assertThat(links).hasSize(2);

    var link1 = links.stream().filter(l -> l.subAgent() == sub1).findFirst().orElseThrow();
    assertThat(link1.triggerTool().getToolName()).isEqualTo("extractHeaderInfo");

    var link2 = links.stream().filter(l -> l.subAgent() == sub2).findFirst().orElseThrow();
    assertThat(link2.triggerTool().getToolName()).isEqualTo("assessCompliance");
  }

  @Test
  void parentEntryIsExcludedFromLinks() {
    var parent = chatEntry("parent", T0.plusSeconds(5));
    var tool = toolEntry("parent", "someTool", T0.plusSeconds(3));

    List<AgentLink> links = AgentHistoryCorrelator.correlate(
        List.of(parent),
        List.of(tool));

    assertThat(links).isEmpty();
  }

  @Test
  void toolWithNoMatchingSubAgentIsOmitted() {
    // tool executed before any sub-agent finished — no candidate
    var tool = toolEntry("parent", "someTool", T0.plusSeconds(2));
    var sub = chatEntry("sub1", T0.plusSeconds(5));  // finished AFTER tool was recorded

    List<AgentLink> links = AgentHistoryCorrelator.correlate(
        List.of(sub),
        List.of(tool));

    assertThat(links).isEmpty();
  }

  @Test
  void standaloneAgentDoesNotStealToolSlotFromRealSubAgent() {
    // standalone ran long before orchestrator; real sub-agent ran just before tool was recorded
    var standalone = chatEntry("standalone", T0);                    // lastUpdated T+0
    var realSub = chatEntry("realSub", T0.plusSeconds(29));          // lastUpdated T+29
    var parent = chatEntry("parent", T0.plusSeconds(35));

    // tool recorded at T+30: realSub gap = 1s, standalone gap = 30s → realSub wins
    var tool = toolEntry("parent", "extractHeaderInfo", T0.plusSeconds(30));

    List<AgentLink> links = AgentHistoryCorrelator.correlate(
        List.of(standalone, realSub, parent),
        List.of(tool));

    assertThat(links).hasSize(1);
    assertThat(links.get(0).subAgent()).isEqualTo(realSub);
  }

  // --- buildSequence tests ---

  @Test
  void buildSequenceCreatesCorrectHierarchy() {
    var parent = chatEntry("parent", T0.plusSeconds(10));
    var sub1 = chatEntry("sub1", T0.plusSeconds(2));
    var sub2 = chatEntry("sub2", T0.plusSeconds(5));

    var tool1 = toolEntry("parent", "extractHeaderInfo", T0.plusSeconds(3));
    var tool2 = toolEntry("parent", "assessCompliance", T0.plusSeconds(6));

    List<AgentNode> forest = AgentHistoryCorrelator.buildSequence(
        List.of(parent, sub1, sub2),
        List.of(tool1, tool2));

    assertThat(forest).hasSize(1);
    AgentNode root = forest.get(0);
    assertThat(root.chat()).isEqualTo(parent);
    assertThat(root.tools()).containsExactly(tool1, tool2);
    assertThat(root.children()).hasSize(2);
    assertThat(root.children().get(0).chat()).isEqualTo(sub1);
    assertThat(root.children().get(1).chat()).isEqualTo(sub2);
  }

  @Test
  void buildSequenceStandaloneAgentBecomesOwnRoot() {
    var standalone = chatEntry("standalone", T0);                    // ran before orchestrator
    var parent = chatEntry("parent", T0.plusSeconds(10));
    var realSub = chatEntry("realSub", T0.plusSeconds(4));

    var tool = toolEntry("parent", "extractHeaderInfo", T0.plusSeconds(5));

    List<AgentNode> forest = AgentHistoryCorrelator.buildSequence(
        List.of(standalone, parent, realSub),
        List.of(tool));

    assertThat(forest).hasSize(2);

    var standaloneNode = forest.stream()
        .filter(n -> n.chat() == standalone).findFirst().orElseThrow();
    assertThat(standaloneNode.tools()).isEmpty();
    assertThat(standaloneNode.children()).isEmpty();

    var parentNode = forest.stream()
        .filter(n -> n.chat() == parent).findFirst().orElseThrow();
    assertThat(parentNode.children()).hasSize(1);
    assertThat(parentNode.children().get(0).chat()).isEqualTo(realSub);
  }

  @Test
  void buildSequenceWithNoToolsReturnsSingleLeaf() {
    var chat = chatEntry("agent1", T0);

    List<AgentNode> forest = AgentHistoryCorrelator.buildSequence(
        List.of(chat),
        List.of());

    assertThat(forest).hasSize(1);
    assertThat(forest.get(0).chat()).isEqualTo(chat);
    assertThat(forest.get(0).tools()).isEmpty();
    assertThat(forest.get(0).children()).isEmpty();
  }

  @Test
  void buildSequenceToolsAreOrderedByExecutedAt() {
    var parent = chatEntry("parent", T0.plusSeconds(10));
    var sub1 = chatEntry("sub1", T0.plusSeconds(2));
    var sub2 = chatEntry("sub2", T0.plusSeconds(5));

    // tools added out of order
    var tool2 = toolEntry("parent", "assessCompliance", T0.plusSeconds(6));
    var tool1 = toolEntry("parent", "extractHeaderInfo", T0.plusSeconds(3));

    List<AgentNode> forest = AgentHistoryCorrelator.buildSequence(
        List.of(parent, sub1, sub2),
        List.of(tool2, tool1));

    assertThat(forest).hasSize(1);
    assertThat(forest.get(0).tools()).containsExactly(tool1, tool2);
  }

  // --- helpers ---

  private static ChatHistoryEntry chatEntry(String agentId, LocalDateTime lastUpdated) {
    var e = new ChatHistoryEntry();
    e.setAgentId(agentId);
    e.setCaseUuid("case-1");
    e.setTaskUuid("task-1");
    e.setLastUpdated(lastUpdated);
    return e;
  }

  private static ToolExecutionEntry toolEntry(String agentId, String toolName,
      LocalDateTime executedAt) {
    var e = new ToolExecutionEntry();
    e.setAgentId(agentId);
    e.setCaseUuid("case-1");
    e.setTaskUuid("task-1");
    e.setToolName(toolName);
    e.setExecutedAt(executedAt);
    return e;
  }
}
