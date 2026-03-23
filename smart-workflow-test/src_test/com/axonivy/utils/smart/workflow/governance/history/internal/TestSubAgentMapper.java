package com.axonivy.utils.smart.workflow.governance.history.internal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.entity.ToolExecutionEntry;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestSubAgentMapper {

  private static final LocalDateTime BASE_TIME = LocalDateTime.of(2025, 1, 1, 12, 0, 0);

  @Test
  void mapsSubAgentToCallerExcludingCallerItself() {
    var parent = chatEntry("parent", BASE_TIME.plusSeconds(10));
    var sub = chatEntry("sub", BASE_TIME.plusSeconds(3));
    var tool = toolEntry("parent", "extractData", BASE_TIME.plusSeconds(5));

    Map<String, List<AgentConversationEntry>> result = SubAgentMapper.map(
        List.of(parent, sub), List.of(tool));

    assertThat(result).containsOnlyKeys("parent");
    assertThat(result.get("parent")).containsExactly(sub);
  }

  @Test
  void subAgentNewerThanToolIsNotMatched() {
    var parent = chatEntry("parent", BASE_TIME.plusSeconds(20));
    var sub = chatEntry("sub", BASE_TIME.plusSeconds(10));
    var tool = toolEntry("parent", "doSomething", BASE_TIME.plusSeconds(5));

    Map<String, List<AgentConversationEntry>> result = SubAgentMapper.map(
        List.of(parent, sub), List.of(tool));

    assertThat(result).isEmpty();
  }

  @Test
  void multipleSubAgentsMappedOneToOne() {
    var parent = chatEntry("parent", BASE_TIME.plusSeconds(15));
    var sub1 = chatEntry("sub1", BASE_TIME.plusSeconds(8));
    var sub2 = chatEntry("sub2", BASE_TIME.plusSeconds(9));
    var sub3 = chatEntry("sub3", BASE_TIME.plusSeconds(10));

    var tool1 = toolEntry("parent", "toolA", BASE_TIME.plusSeconds(11));
    var tool2 = toolEntry("parent", "toolB", BASE_TIME.plusSeconds(11).plusNanos(100_000));
    var tool3 = toolEntry("parent", "toolC", BASE_TIME.plusSeconds(11).plusNanos(200_000));

    Map<String, List<AgentConversationEntry>> result = SubAgentMapper.map(
        List.of(parent, sub1, sub2, sub3), List.of(tool1, tool2, tool3));

    assertThat(result).containsOnlyKeys("parent");
    assertThat(result.get("parent")).containsExactlyInAnyOrder(sub1, sub2, sub3);
  }

  @Test
  void multipleCallersMappedSeparately() {
    var callerA = chatEntry("callerA", BASE_TIME.plusSeconds(10));
    var callerB = chatEntry("callerB", BASE_TIME.plusSeconds(10));
    var subOfA = chatEntry("subOfA", BASE_TIME.plusSeconds(3));
    var subOfB = chatEntry("subOfB", BASE_TIME.plusSeconds(6));

    var toolByA = toolEntry("callerA", "toolA", BASE_TIME.plusSeconds(4));
    var toolByB = toolEntry("callerB", "toolB", BASE_TIME.plusSeconds(8));

    Map<String, List<AgentConversationEntry>> result = SubAgentMapper.map(
        List.of(callerA, callerB, subOfA, subOfB), List.of(toolByA, toolByB));

    assertThat(result).containsOnlyKeys("callerA", "callerB");
    assertThat(result.get("callerA")).containsExactly(subOfA);
    assertThat(result.get("callerB")).containsExactly(subOfB);
  }

  private static AgentConversationEntry chatEntry(String agentId, LocalDateTime lastUpdated) {
    var entry = new AgentConversationEntry();
    entry.setAgentId(agentId);
    entry.setCaseUuid("case-1");
    entry.setTaskUuid("task-1");
    entry.setLastUpdated(lastUpdated.toString());
    return entry;
  }

  private static ToolExecutionEntry toolEntry(String agentId, String toolName, LocalDateTime executedAt) {
    var entry = new ToolExecutionEntry();
    entry.setAgentId(agentId);
    entry.setCaseUuid("case-1");
    entry.setTaskUuid("task-1");
    entry.setToolName(toolName);
    entry.setExecutedAt(executedAt.toString());
    return entry;
  }
}
