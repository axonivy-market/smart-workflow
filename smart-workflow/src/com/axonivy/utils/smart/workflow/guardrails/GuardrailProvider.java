package com.axonivy.utils.smart.workflow.guardrails;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.axonivy.utils.smart.workflow.guardrails.adapter.InputGuardrailAdapter;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.spi.internal.SpiLoader;
import com.axonivy.utils.smart.workflow.spi.internal.SpiProject;

public class GuardrailProvider {

  public static final String USE_GUARDRAIL = "AI.UseGuardrails";

  private static Optional<SmartWorkflowInputGuardrail> create(String name) {
    return providers().stream().filter(impl -> Objects.equals(impl.getClass().getSimpleName(), name)).findFirst();
  }

  public static List<SmartWorkflowInputGuardrail> providers() {
    var project = SpiProject.getSmartWorkflowPmv().project();
    return new SpiLoader(project).load(SmartWorkflowInputGuardrail.class).stream().collect(Collectors.toList());
  }

  public static List<InputGuardrailAdapter> providersList(List<String> filters) {
    if (CollectionUtils.isEmpty(filters)) {
      return providers().stream().map(InputGuardrailAdapter::new).collect(Collectors.toList());
    }

    return filters.stream().distinct()
        .map(GuardrailProvider::create)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(InputGuardrailAdapter::new)
        .collect(Collectors.toList());
  }
}