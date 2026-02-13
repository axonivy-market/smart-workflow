package com.axonivy.utils.smart.workflow.guardrails.provider;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.input.PromptInjectionGuardrail;

import ch.ivyteam.ivy.environment.Ivy;

public class DefaultGuardrailProvider implements GuardrailProvider {
    public static final String DEFAULT_INPUT_GUARDRAILS = "AI.Guardrails.DefaultInput";

    public List<SmartWorkflowInputGuardrail> getFilteredDefaultInputGuardrails() {
        String defaultGuardrails = "";
        try {
            defaultGuardrails = Ivy.var().get(DEFAULT_INPUT_GUARDRAILS);
        } catch (Exception e) {
            Ivy.log().error(e.getMessage());
        }

        List<String> guardrailNames = List.of(StringUtils.split(defaultGuardrails, ",")).stream()
        .distinct().filter(StringUtils::isNotBlank).map(String::strip).toList();
        return getInputGuardrails().stream().filter(g -> guardrailNames.contains(g.name())).collect(Collectors.toList());
    }

    @Override
    public List<SmartWorkflowInputGuardrail> getInputGuardrails() {
        return List.of(new PromptInjectionGuardrail()) ;
    }
}
