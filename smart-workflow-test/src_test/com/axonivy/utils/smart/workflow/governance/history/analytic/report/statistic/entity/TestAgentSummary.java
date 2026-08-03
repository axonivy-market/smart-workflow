package com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestAgentSummary {

  @Test
  void getDisplayName_agentNameSet_returnsAgentName() {
    var agent = new AgentSummary();
    agent.setAgentId("id-1");
    agent.setAgentName("Customer Service Agent");
    assertThat(agent.getDisplayName()).isEqualTo("Customer Service Agent");
  }

  @Test
  void getDisplayName_agentNameBlank_returnsAgentId() {
    var agent = new AgentSummary();
    agent.setAgentId("id-1");
    agent.setAgentName("   ");
    assertThat(agent.getDisplayName()).isEqualTo("id-1");
  }

  @Test
  void getDisplayName_agentNameNull_returnsAgentId() {
    var agent = new AgentSummary();
    agent.setAgentId("id-1");
    agent.setAgentName(null);
    assertThat(agent.getDisplayName()).isEqualTo("id-1");
  }

  @Test
  void getDisplayName_agentNameEmpty_returnsAgentId() {
    var agent = new AgentSummary();
    agent.setAgentId("id-1");
    agent.setAgentName("");
    assertThat(agent.getDisplayName()).isEqualTo("id-1");
  }

  @Test
  void hasAnomalyIssues_withIssues_returnsTrue() {
    var agent = new AgentSummary();
    agent.setAnomalyIssues(List.of("Duration exceeded 30s"));
    assertThat(agent.hasAnomalyIssues()).isTrue();
  }

  @Test
  void hasAnomalyIssues_emptyList_returnsFalse() {
    var agent = new AgentSummary();
    agent.setAnomalyIssues(List.of());
    assertThat(agent.hasAnomalyIssues()).isFalse();
  }

  @Test
  void hasAnomalyIssues_nullList_returnsFalse() {
    var agent = new AgentSummary();
    agent.setAnomalyIssues(null);
    assertThat(agent.hasAnomalyIssues()).isFalse();
  }
}
