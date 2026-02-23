package com.axonivy.utils.smart.workflow.guardrails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.guardrails.adapter.InputGuardrailAdapter;
import com.axonivy.utils.smart.workflow.guardrails.dummy.DummyGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.dummy.SecondDummyGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.input.PromptInjectionGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.provider.DefaultGuardrailProvider;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestGuardrailCollector {

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(DefaultGuardrailProvider.DEFAULT_INPUT_GUARDRAILS, "PromptInjectionGuardrail");
  }

  @Test
  void providersListWithoutFilters(AppFixture fixture) {
    var adapters = GuardrailCollector.inputGuardrailAdapters(null);
    assertThat(adapters).hasSize(1);
    assertThat(adapters.get(0).getDelegate()).isInstanceOf(PromptInjectionGuardrail.class);
  }

  @Test
  void providersListWithFilters() {
    var adapters = GuardrailCollector.inputGuardrailAdapters(
        List.of("PromptInjectionGuardrail", "PromptInjectionGuardrail", "InvalidGuardrail", "DummyGuardrail", "SecondDummyGuardrail"));
    assertThat(adapters).hasSize(3); // duplicates and non-existed should be filtered
    List<SmartWorkflowInputGuardrail> delegates = adapters.stream().map(InputGuardrailAdapter::getDelegate).toList();
    assertThat(delegates.get(0)).isInstanceOf(PromptInjectionGuardrail.class);
    assertThat(delegates.get(1)).isInstanceOf(DummyGuardrail.class);
    assertThat(delegates.get(2)).isInstanceOf(SecondDummyGuardrail.class);
  }

  @Test
  void allInputGuardrailNames() {
    var names = GuardrailCollector.allInputGuardrailNames();
    assertThat(names).containsExactlyInAnyOrder(
        "PromptInjectionGuardrail", "DummyGuardrail", "SecondDummyGuardrail");
  }
}
