package com.axonivy.utils.smart.workflow.guardrails.dummy;

import java.util.List;

import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.provider.GuardrailProvider;

public class DummyGuardrailProvider implements GuardrailProvider {

    @Override
    public List<SmartWorkflowInputGuardrail> getInputGuardrails() {
        return List.of(
            new DummyGuardrail(),
            new SecondDummyGuardrail()
        );
    }
}