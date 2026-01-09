package com.axonivy.utils.smart.workflow.guardrails;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.guardrails.adapter.InputGuardrailAdapter;
import com.axonivy.utils.smart.workflow.guardrails.input.PromptInjectionGuardrail;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestGuardrailProvider {

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(GuardrailProvider.USE_GUARDRAIL, "true");
  }

  @Test
  void providersListWithoutFilters() {
    var adapters = GuardrailProvider.providersList(null);
    assertThat(adapters).isNotEmpty();
    assertThat(adapters).allMatch(a -> a instanceof InputGuardrailAdapter);
  }

  @Test
  void providersListWithFilters() {
    var adapters = GuardrailProvider
        .providersList(List.of("PromptInjectionGuardrail", "PromptInjectionGuardrail", "InvalidGuardrail"));
    assertThat(adapters).hasSize(1); // duplicates and non-existed should be filtered
    assertThat(adapters.get(0).getDelegate()).isInstanceOf(PromptInjectionGuardrail.class);
  }
}
