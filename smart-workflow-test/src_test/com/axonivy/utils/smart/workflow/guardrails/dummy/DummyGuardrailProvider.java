package com.axonivy.utils.smart.workflow.guardrails.dummy;

import java.util.List;

import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.provider.GuardrailProvider;

public class DummyGuardrailProvider implements GuardrailProvider {

    @Override
    public List<SmartWorkflowInputGuardrail> getInputGuardrails() {
        return List.of(
            new DummyInputGuardrail(),
            new SecondDummyInputGuardrail()
        );
    }

    @Override
    public List<SmartWorkflowOutputGuardrail> getOutputGuardrails() {
        return List.of(
            new DummyOutputGuardrail(),
            new SecondDummyOutputGuardrail()
        );
    }
}