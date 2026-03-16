package com.axonivy.utils.smart.workflow.guardrails;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.axonivy.utils.smart.workflow.guardrails.adapter.InputGuardrailAdapter;
import com.axonivy.utils.smart.workflow.guardrails.adapter.OutputGuardrailAdapter;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowOutputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.provider.DefaultGuardrailProvider;
import com.axonivy.utils.smart.workflow.guardrails.provider.GuardrailProvider;
import com.axonivy.utils.smart.workflow.spi.internal.SpiLoader;
import com.axonivy.utils.smart.workflow.spi.internal.SpiProject;

public class GuardrailCollector {

  public static Set<GuardrailProvider> allProviders() {
    var project = SpiProject.getSmartWorkflowPmv().project();
    return new SpiLoader(project).load(GuardrailProvider.class);
  }

  public static List<String> allInputGuardrailNames() {
    List<SmartWorkflowInputGuardrail> inputGuardrails = new ArrayList<>(new DefaultGuardrailProvider().getInputGuardrails());

    inputGuardrails.addAll(
        allProviders().stream()
          .flatMap(provider -> provider.getInputGuardrails().stream())
          .collect(Collectors.toList())
      );

    return inputGuardrails.stream()
        .map(InputGuardrailAdapter::new)
        .distinct()
        .map(mapper -> mapper.getDelegate().name())
        .collect(Collectors.toList());
  }

  public static List<InputGuardrailAdapter> inputGuardrailAdapters(List<String> filters) {
    List<SmartWorkflowInputGuardrail> inputGuardrails = new DefaultGuardrailProvider().getFilteredDefaultInputGuardrails();

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
        .filter(adapter -> filters.contains(adapter.getDelegate().name()))
        .distinct()
        .collect(Collectors.toList());
  }

  public static List<String> allOutputGuardrailNames() {
    List<SmartWorkflowOutputGuardrail> outputGuardrails = new ArrayList<>(new DefaultGuardrailProvider().getOutputGuardrails());

    outputGuardrails.addAll(
        allProviders().stream()
          .flatMap(provider -> provider.getOutputGuardrails().stream())
          .collect(Collectors.toList())
      );

    return outputGuardrails.stream()
        .map(OutputGuardrailAdapter::new)
        .distinct()
        .map(mapper -> mapper.getDelegate().name())
        .collect(Collectors.toList());
  }

  public static List<OutputGuardrailAdapter> outputGuardrailAdapters(List<String> filters) {
    List<SmartWorkflowOutputGuardrail> outputGuardrails = new DefaultGuardrailProvider().getFilteredDefaultOutputGuardrails();

    if (CollectionUtils.isEmpty(filters)) {
      return outputGuardrails.stream()
          .map(OutputGuardrailAdapter::new)
          .distinct()
          .collect(Collectors.toList());
    }

    outputGuardrails.addAll(
        allProviders().stream()
          .flatMap(provider -> provider.getOutputGuardrails().stream())
          .collect(Collectors.toList())
      );

    return outputGuardrails.stream()
        .map(OutputGuardrailAdapter::new)
        .filter(adapter -> filters.contains(adapter.getDelegate().name()))
        .distinct()
        .collect(Collectors.toList());
  }
}
