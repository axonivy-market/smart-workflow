package com.axonivy.utils.smart.workflow.guardrails;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.guardrails.entity.AbstractInputGuardrail;
import com.axonivy.utils.smart.workflow.internal.SpiLoader;
import com.axonivy.utils.smart.workflow.utils.IvyUtils;

public class GuardrailFactory {

  public static AbstractInputGuardrail createInputGuardrail(String name) {
    if (StringUtils.isBlank(name)) {
      return null;
    }

    var provider = GuardrailFactory.create(name)
        .orElseThrow(() -> new IllegalArgumentException("Unknown guardrail provider " + name));
    return provider;
  }

  public static Optional<AbstractInputGuardrail> create(String name) {
    return providers().stream()
        .filter(impl -> Objects.equals(impl.name(), name))
        .findFirst();
  }

  public static Set<AbstractInputGuardrail> providers() {
    var project = IvyUtils.getSmartWorkflowPmv().project();
    return new SpiLoader(project).load(AbstractInputGuardrail.class);
  }
}
