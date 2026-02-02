package com.axonivy.utils.smart.workflow.guardrails;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.axonivy.utils.smart.workflow.guardrails.adapter.InputGuardrailAdapter;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.provider.DefaultGuardrailProvider;
import com.axonivy.utils.smart.workflow.guardrails.provider.GuardrailProvider;
import com.axonivy.utils.smart.workflow.spi.internal.SpiLoader;
import com.axonivy.utils.smart.workflow.spi.internal.SpiProject;

public class GuardrailCollector {

  public static Set<GuardrailProvider> allProviders() {
    var project = SpiProject.getSmartWorkflowPmv().project();
    return new SpiLoader(project).load(GuardrailProvider.class);
  }

  public static List<InputGuardrailAdapter> inputGuardrailAdapters(List<String> filters) {
    List<SmartWorkflowInputGuardrail> inputGuardrails = new DefaultGuardrailProvider().getDefaultInputGuardrails();

    if (CollectionUtils.isEmpty(filters)) {
      return inputGuardrails.stream()
          .map(InputGuardrailAdapter::new)
          .distinct()
          .collect(Collectors.toList());
    }

    inputGuardrails.addAll(
        allProviders().stream()
          .flatMap(provider -> provider.getInputGuardrails().stream())
          .collect(Collectors.toList())
      );

    return inputGuardrails.stream()
        .map(InputGuardrailAdapter::new)
        .distinct()
        .collect(Collectors.toList());
  }
}
