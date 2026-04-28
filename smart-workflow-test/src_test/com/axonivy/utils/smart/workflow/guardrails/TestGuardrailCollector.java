package com.axonivy.utils.smart.workflow.guardrails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.guardrails.adapter.InputGuardrailAdapter;
import com.axonivy.utils.smart.workflow.guardrails.adapter.OutputGuardrailAdapter;
import com.axonivy.utils.smart.workflow.guardrails.dummy.DummyInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.dummy.DummyOutputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.dummy.SecondDummyInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.dummy.SecondDummyOutputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.input.PromptInjectionInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.output.SensitiveDataOutputGuardrail;

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
  void inputAdaptersWithVariableDefault() {
    var adapters = GuardrailCollector.inputGuardrailAdapters(null);
    assertThat(adapters).hasSize(1);
    assertThat(adapters.get(0).getDelegate()).isInstanceOf(PromptInjectionInputGuardrail.class);
  }

  @Test
  void inputAdaptersWithEmptyVariable(AppFixture fixture) {
    fixture.var(GuardrailCollector.DEFAULT_INPUT_GUARDRAILS, "");
    assertThat(GuardrailCollector.inputGuardrailAdapters(null)).isEmpty();
  }

  @Test
  void inputAdaptersWithFilters() {
    // Filter order preserved; duplicates/unknowns silently ignored
    var adapters = GuardrailCollector.inputGuardrailAdapters(
        List.of("DummyInputGuardrail", "PromptInjectionInputGuardrail", "PromptInjectionInputGuardrail", "InvalidGuardrail", "SecondDummyInputGuardrail"));
    assertThat(adapters).hasSize(3);
    List<SmartWorkflowInputGuardrail> delegates = adapters.stream().map(InputGuardrailAdapter::getDelegate).toList();
    assertThat(delegates.get(0)).isInstanceOf(DummyInputGuardrail.class);
    assertThat(delegates.get(1)).isInstanceOf(PromptInjectionInputGuardrail.class);
    assertThat(delegates.get(2)).isInstanceOf(SecondDummyInputGuardrail.class);
  }

  @Test
  void allInputGuardrailNames() {
    var names = GuardrailCollector.allInputGuardrailNames();
    assertThat(names).containsExactlyInAnyOrder(
        "PromptInjectionInputGuardrail", "PiiMaskingInputGuardrail", "DummyInputGuardrail", "SecondDummyInputGuardrail", "BlockCompetitorMentionGuardrail");
  }

  @Test
  void outputAdaptersWithVariableDefault() {
    var adapters = GuardrailCollector.outputGuardrailAdapters(null);
    assertThat(adapters).hasSize(1);
    assertThat(adapters.get(0).getDelegate()).isInstanceOf(SensitiveDataOutputGuardrail.class);
  }

  @Test
  void outputAdaptersWithFilters() {
    // Filter order preserved; duplicates/unknowns silently ignored
    var adapters = GuardrailCollector.outputGuardrailAdapters(
        List.of("DummyOutputGuardrail", "SensitiveDataOutputGuardrail", "SensitiveDataOutputGuardrail", "InvalidGuardrail", "SecondDummyOutputGuardrail"));
    assertThat(adapters).hasSize(3);
    List<SmartWorkflowOutputGuardrail> delegates = adapters.stream().map(OutputGuardrailAdapter::getDelegate).toList();
    assertThat(delegates.get(0)).isInstanceOf(DummyOutputGuardrail.class);
    assertThat(delegates.get(1)).isInstanceOf(SensitiveDataOutputGuardrail.class);
    assertThat(delegates.get(2)).isInstanceOf(SecondDummyOutputGuardrail.class);
  }

  @Test
  void allOutputGuardrailNames() {
    var names = GuardrailCollector.allOutputGuardrailNames();
    assertThat(names).containsExactlyInAnyOrder(
        "SensitiveDataOutputGuardrail", "PiiMaskingOutputGuardrail", "DummyOutputGuardrail", "SecondDummyOutputGuardrail");
  }
}
