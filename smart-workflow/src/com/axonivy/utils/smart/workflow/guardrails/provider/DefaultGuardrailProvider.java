package com.axonivy.utils.smart.workflow.guardrails.provider;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.input.PromptInjectionInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.output.SensitiveDataOutputGuardrail;

import ch.ivyteam.ivy.environment.Ivy;

public class DefaultGuardrailProvider implements GuardrailProvider {
  public static final String DEFAULT_INPUT_GUARDRAILS = "AI.Guardrails.DefaultInput";
  public static final String DEFAULT_OUTPUT_GUARDRAILS = "AI.Guardrails.DefaultOutput";

  public List<SmartWorkflowInputGuardrail> getFilteredDefaultInputGuardrails() {
    return getGuardrailsByVariableKey(DEFAULT_INPUT_GUARDRAILS, getInputGuardrails());
  }

  public List<SmartWorkflowOutputGuardrail> getFilteredDefaultOutputGuardrails() {
    return getGuardrailsByVariableKey(DEFAULT_OUTPUT_GUARDRAILS, getOutputGuardrails());
  }

  private <T extends SmartWorkflowGuardrail> List<T> getGuardrailsByVariableKey(String variableKey,
      List<T> guardrails) {
    String defaultGuardrails = "";
    try {
      defaultGuardrails = StringUtils.defaultString(Ivy.var().get(variableKey));
    } catch (Exception e) {
      Ivy.log().error("Error reading default guardrails for variable key '" + variableKey + "'", e);
    }
    String[] split = StringUtils.split(defaultGuardrails, ",");
    Set<String> guardrailNames = split == null ? Set.of()
        : Arrays.stream(split).filter(StringUtils::isNotBlank).map(String::strip).collect(Collectors.toSet());
    return guardrails.stream().filter(g -> guardrailNames.contains(g.name())).collect(Collectors.toList());
  }

  @Override
  public List<SmartWorkflowInputGuardrail> getInputGuardrails() {
    return List.of(new PromptInjectionInputGuardrail());
  }

  @Override
  public List<SmartWorkflowOutputGuardrail> getOutputGuardrails() {
    return List.of(new SensitiveDataOutputGuardrail());
  }
}
