package com.axonivy.utils.smart.workflow.guardrails;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.guardrails.adapter.InputGuardrailAdapter;
import com.axonivy.utils.smart.workflow.guardrails.adapter.OutputGuardrailAdapter;
import com.axonivy.utils.smart.workflow.guardrails.circuitbreaker.CircuitBreakerGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.dummy.DummyGuardrailProvider;
import com.axonivy.utils.smart.workflow.guardrails.dummy.DummyInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.dummy.DummyOutputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.dummy.SecondDummyInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.dummy.SecondDummyOutputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.input.PromptInjectionInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.output.SensitiveDataOutputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.provider.GuardrailProvider;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestGuardrailCollector {

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(GuardrailCollector.DEFAULT_INPUT_GUARDRAILS, "PromptInjectionInputGuardrail");
    fixture.var(GuardrailCollector.DEFAULT_OUTPUT_GUARDRAILS, "SensitiveDataOutputGuardrail");
  }

  @Test
  void inputGuardrailAdapters_nullFilters_returnsInternalFirstThenVariableDefault() {
    var adapters = GuardrailCollector.inputGuardrailAdapters(null);
    assertThat(adapters).hasSize(2);
    List<SmartWorkflowInputGuardrail> delegates = adapters.stream().map(InputGuardrailAdapter::getDelegate).toList();
    assertThat(delegates.get(0)).isInstanceOf(CircuitBreakerGuardrail.class);
    assertThat(delegates.get(1)).isInstanceOf(PromptInjectionInputGuardrail.class);
  }

  @Test
  void inputGuardrailAdapters_blankVariableAndNullFilters_returnsOnlyInternalGuardrail(AppFixture fixture) {
    fixture.var(GuardrailCollector.DEFAULT_INPUT_GUARDRAILS, "");
    var adapters = GuardrailCollector.inputGuardrailAdapters(null);
    assertThat(adapters).hasSize(1);
    assertThat(adapters.get(0).getDelegate()).isInstanceOf(CircuitBreakerGuardrail.class);
  }

  @Test
  void inputGuardrailAdapters_filtersWithDuplicatesAndUnknownName_returnsInternalFirstThenDistinctKnownFilters() {
    var adapters = GuardrailCollector.inputGuardrailAdapters(
        List.of("CircuitBreakerGuardrail", "DummyInputGuardrail", "PromptInjectionInputGuardrail", "PromptInjectionInputGuardrail", "InvalidGuardrail", "SecondDummyInputGuardrail"));
    assertThat(adapters).hasSize(4);
    List<SmartWorkflowInputGuardrail> delegates = adapters.stream().map(InputGuardrailAdapter::getDelegate).toList();
    assertThat(delegates.get(0)).isInstanceOf(CircuitBreakerGuardrail.class);
    assertThat(delegates.get(1)).isInstanceOf(DummyInputGuardrail.class);
    assertThat(delegates.get(2)).isInstanceOf(PromptInjectionInputGuardrail.class);
    assertThat(delegates.get(3)).isInstanceOf(SecondDummyInputGuardrail.class);
  }

  @Test
  void inputGuardrailAdapters_noInternalGuardrailsAndBlankVariable_returnsEmpty(AppFixture fixture) {
    fixture.var(GuardrailCollector.DEFAULT_INPUT_GUARDRAILS, "");
    var adapters = GuardrailCollector.inputGuardrailAdapters(Set.of(new DummyGuardrailProvider()), null);
    assertThat(adapters).isEmpty();
  }

  @Test
  void inputGuardrailAdapters_noInternalGuardrailsWithFilters_returnsFilteredGuardrailsInFilterOrder() {
    var adapters = GuardrailCollector.inputGuardrailAdapters(
        Set.of(new DummyGuardrailProvider()),
        List.of("SecondDummyInputGuardrail", "DummyInputGuardrail"));
    assertThat(adapters).hasSize(2);
    List<SmartWorkflowInputGuardrail> delegates = adapters.stream().map(InputGuardrailAdapter::getDelegate).toList();
    assertThat(delegates.get(0)).isInstanceOf(SecondDummyInputGuardrail.class);
    assertThat(delegates.get(1)).isInstanceOf(DummyInputGuardrail.class);
  }

  @Test
  void inputGuardrailAdapters_duplicateGuardrailNameAcrossProviders_firstProviderWins() {
    var firstProviderGuardrail = new DummyInputGuardrail();
    var secondProviderGuardrail = new DummyInputGuardrail();
    GuardrailProvider firstProvider = new GuardrailProvider() {
      @Override
      public List<SmartWorkflowInputGuardrail> getInputGuardrails() {
        return List.of(firstProviderGuardrail);
      }
    };
    GuardrailProvider secondProvider = new GuardrailProvider() {
      @Override
      public List<SmartWorkflowInputGuardrail> getInputGuardrails() {
        return List.of(secondProviderGuardrail);
      }
    };
    Set<GuardrailProvider> providers = new LinkedHashSet<>(List.of(firstProvider, secondProvider));

    var adapters = GuardrailCollector.inputGuardrailAdapters(providers, List.of("DummyInputGuardrail"));

    assertThat(adapters).hasSize(1);
    assertThat(adapters.get(0).getDelegate()).isSameAs(firstProviderGuardrail);
  }

  @Test
  void inputGuardrailAdapters_filtersProvided_takesPrecedenceOverVariableDefault() {
    var adapters = GuardrailCollector.inputGuardrailAdapters(List.of("DummyInputGuardrail"));
    assertThat(adapters).hasSize(2);
    List<SmartWorkflowInputGuardrail> delegates = adapters.stream().map(InputGuardrailAdapter::getDelegate).toList();
    assertThat(delegates.get(0)).isInstanceOf(CircuitBreakerGuardrail.class);
    assertThat(delegates.get(1)).isInstanceOf(DummyInputGuardrail.class);
    assertThat(delegates).noneMatch(PromptInjectionInputGuardrail.class::isInstance);
  }

  @Test
  void allInputGuardrailNames_always_excludesInternalGuardrails() {
    var names = GuardrailCollector.allInputGuardrailNames();
    assertThat(names).containsExactlyInAnyOrder(
        "PromptInjectionInputGuardrail", "AiPromptInjectionInputGuardrail", "PiiMaskingGuardrail", "DummyInputGuardrail", "SecondDummyInputGuardrail", "BlockCompetitorMentionGuardrail");
  }

  @Test
  void outputGuardrailAdapters_nullFilters_returnsInternalFirstThenVariableDefault() {
    var adapters = GuardrailCollector.outputGuardrailAdapters(null);
    assertThat(adapters).hasSize(2);
    List<SmartWorkflowOutputGuardrail> delegates = adapters.stream().map(OutputGuardrailAdapter::getDelegate).toList();
    assertThat(delegates.get(0)).isInstanceOf(CircuitBreakerGuardrail.class);
    assertThat(delegates.get(1)).isInstanceOf(SensitiveDataOutputGuardrail.class);
  }

  @Test
  void outputGuardrailAdapters_filtersWithDuplicatesAndUnknownName_returnsInternalFirstThenDistinctKnownFilters() {
    var adapters = GuardrailCollector.outputGuardrailAdapters(
        List.of("DummyOutputGuardrail", "SensitiveDataOutputGuardrail", "SensitiveDataOutputGuardrail", "InvalidGuardrail", "SecondDummyOutputGuardrail"));
    assertThat(adapters).hasSize(4);
    List<SmartWorkflowOutputGuardrail> delegates = adapters.stream().map(OutputGuardrailAdapter::getDelegate).toList();
    assertThat(delegates.get(0)).isInstanceOf(CircuitBreakerGuardrail.class);
    assertThat(delegates.get(1)).isInstanceOf(DummyOutputGuardrail.class);
    assertThat(delegates.get(2)).isInstanceOf(SensitiveDataOutputGuardrail.class);
    assertThat(delegates.get(3)).isInstanceOf(SecondDummyOutputGuardrail.class);
  }

  @Test
  void allOutputGuardrailNames_always_excludesInternalGuardrails() {
    var names = GuardrailCollector.allOutputGuardrailNames();
    assertThat(names).containsExactlyInAnyOrder(
        "SensitiveDataOutputGuardrail", "PiiMaskingGuardrail", "DummyOutputGuardrail", "SecondDummyOutputGuardrail");
  }
}
