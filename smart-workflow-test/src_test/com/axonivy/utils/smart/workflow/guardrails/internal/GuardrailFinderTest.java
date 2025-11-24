package com.axonivy.utils.smart.workflow.guardrails.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.guardrails.dummy.DummyInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.dummy.DummyNonAnnotatedInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.dummy.DummyNonAnnotatedOutputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.dummy.DummyOutputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.internal.annotation.SmartWorkflowGuardrail.GuardrailType;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class GuardrailFinderTest {
  @Test
  void loadInputGuardrailsClassNames() {
    var guardrails = GuardrailFinder.findGuardrailClassNames(GuardrailType.INPUT);
    assertThat(guardrails.contains(DummyInputGuardrail.class.getName()));
    assertThat(!guardrails.contains(DummyNonAnnotatedInputGuardrail.class.getName()));
    assertThat(!guardrails.contains(DummyOutputGuardrail.class.getName()));


  }

  @Test
  void loadOutputGuardrailsClassNames() {
    var guardrails = GuardrailFinder.findGuardrailClassNames(GuardrailType.OUTPUT);
    assertThat(guardrails.contains(DummyInputGuardrail.class.getName()));
    assertThat(!guardrails.contains(DummyNonAnnotatedOutputGuardrail.class.getName()));
    assertThat(!guardrails.contains(DummyOutputGuardrail.class.getName()));
  }

  @Test
  void loadInputGuardrail() {
    var guardrails = GuardrailFinder
        .findInputGuardrailsByClassNames(Arrays.asList(DummyInputGuardrail.class.getName()));

    assertThat(guardrails.stream()
        .anyMatch(g -> g.getClass().getName().equals(DummyInputGuardrail.class.getName())));
    assertThat(guardrails.stream()
        .noneMatch(g -> g.getClass().getName().equals(DummyNonAnnotatedInputGuardrail.class.getName())));
    assertThat(guardrails.stream()
        .noneMatch(g -> g.getClass().getName().equals(DummyOutputGuardrail.class.getName())));
  }

  @Test
  void loadOutputGuardrail() {
    var guardrails = GuardrailFinder
        .findOutputGuardrailsByClassNames(Arrays.asList(DummyOutputGuardrail.class.getName()));

    assertThat(guardrails.stream()
        .anyMatch(g -> g.getClass().getName().equals(DummyOutputGuardrail.class.getName())));
    assertThat(guardrails.stream()
        .noneMatch(g -> g.getClass().getName().equals(DummyNonAnnotatedOutputGuardrail.class.getName())));
    assertThat(guardrails.stream()
        .noneMatch(g -> g.getClass().getName().equals(DummyInputGuardrail.class.getName())));
  }
}
