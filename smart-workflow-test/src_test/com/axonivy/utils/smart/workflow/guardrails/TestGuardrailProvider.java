package com.axonivy.utils.smart.workflow.guardrails;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.guardrails.adapter.InputGuardrailAdapter;
import com.axonivy.utils.smart.workflow.guardrails.dummy.DummyGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.input.PromptInjectionGuardrail;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestGuardrailProvider {

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(GuardrailProvider.DEFAULT_INPUT_GUARDRAILS, "PromptInjectionGuardrail");
  }

  @Test
  void providersListWithoutFilters(AppFixture fixture) {
    var adapters = GuardrailProvider.providers(null);
    assertThat(adapters).hasSize(1);
    assertThat(adapters.get(0).getDelegate()).isInstanceOf(PromptInjectionGuardrail.class);
  }

  @Test
  void providersListWithFilters() {
    var adapters = GuardrailProvider.providers(
        List.of("PromptInjectionGuardrail", "PromptInjectionGuardrail", "InvalidGuardrail", "DummyGuardrail"));
    assertThat(adapters).hasSize(2); // duplicates and non-existed should be filtered
    List<SmartWorkflowInputGuardrail> delegates = adapters.stream().map(InputGuardrailAdapter::getDelegate).toList();
    assertThat(delegates.get(0)).isInstanceOf(PromptInjectionGuardrail.class);
    assertThat(delegates.get(1)).isInstanceOf(DummyGuardrail.class);
  }
}
