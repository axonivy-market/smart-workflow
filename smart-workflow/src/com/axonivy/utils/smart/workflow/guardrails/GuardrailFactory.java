package com.axonivy.utils.smart.workflow.guardrails;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.guardrails.entity.AbstractInputGuardrail;
import com.axonivy.utils.smart.workflow.spi.internal.SpiLoader;
import com.axonivy.utils.smart.workflow.spi.internal.SpiProject;

public class GuardrailFactory {

  public static final String USE_GUARDRAIL = "AI.UseGuardrails";

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
        .filter(impl -> Objects.equals(impl.getClass().getSimpleName(), name))
        .findFirst();
  }

  public static List<AbstractInputGuardrail> providers() {
    var project = SpiProject.getSmartWorkflowPmv().project();
    return new SpiLoader(project).load(AbstractInputGuardrail.class).stream().collect(Collectors.toList());
  }

  public static List<AbstractInputGuardrail> providersList(List<String> filters) {
    if (CollectionUtils.isEmpty(filters)) {
      return providers();
    }

    return filters.stream().distinct()
        .map(GuardrailFactory::create)
        .map(Optional::get)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }
}
