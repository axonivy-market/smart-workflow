package com.axonivy.utils.smart.workflow.guardrails;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.guardrails.BlockCompetitorMentionGuardrail;

import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;

@IvyProcessTest
public class TestBlockCompetitorMentionGuardrail {

  private BlockCompetitorMentionGuardrail guardrail;

  @BeforeEach
  void setup() {
    guardrail = new BlockCompetitorMentionGuardrail();
  }

  @Test
  void allowNormalText() {
    var result = guardrail.evaluate("Can you help me automate my approval workflow?");
    assertThat(result.isAllowed()).isTrue();
  }

  @Test
  void blockCompetitorMention() {
    var result = guardrail.evaluate("We are considering switching to Alpha Systems.");
    assertThat(result.isAllowed()).isFalse();
    assertThat(result.getReason()).contains("alpha systems");
  }
}
