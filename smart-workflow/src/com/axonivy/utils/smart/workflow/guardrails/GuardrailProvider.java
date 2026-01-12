package com.axonivy.utils.smart.workflow.guardrails;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.guardrails.adapter.InputGuardrailAdapter;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.spi.internal.SpiLoader;
import com.axonivy.utils.smart.workflow.spi.internal.SpiProject;

import ch.ivyteam.ivy.environment.Ivy;

public class GuardrailProvider {

  public static final String DEFAULT_INPUT_GUARDRAILS = "AI.Guardrails.DefaultInput";

  private static Optional<SmartWorkflowInputGuardrail> create(String name) {
    return allProviders().stream().filter(impl -> impl.name().equals(name)).findFirst();
  }

  public static Set<SmartWorkflowInputGuardrail> allProviders() {
    var project = SpiProject.getSmartWorkflowPmv().project();
    return new SpiLoader(project).load(SmartWorkflowInputGuardrail.class);
  }

  private static List<SmartWorkflowInputGuardrail> defaultProviders() {
    String defaultGuardrails = Ivy.var().get(DEFAULT_INPUT_GUARDRAILS);
    if (StringUtils.isBlank(defaultGuardrails)) {
      return new ArrayList<>();
    }

    List<String> guardrailNames = List.of(StringUtils.split(defaultGuardrails, ",")).stream()
        .distinct().filter(StringUtils::isNotBlank).map(String::strip).toList();
    return allProviders().stream()
        .filter(g -> guardrailNames.contains(g.name())).collect(Collectors.toList());
  }


  public static List<InputGuardrailAdapter> providers(List<String> filters) {
    if (CollectionUtils.isEmpty(filters)) {
      return defaultProviders().stream().map(InputGuardrailAdapter::new).collect(Collectors.toList());
    }

    return filters.stream().distinct()
        .map(GuardrailProvider::create)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(InputGuardrailAdapter::new)
        .collect(Collectors.toList());
  }
}