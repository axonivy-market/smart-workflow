package com.axonivy.utils.smart.workflow.guardrails;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.guardrails.adapter.AbstractGuardrailAdapter;
import com.axonivy.utils.smart.workflow.guardrails.adapter.InputGuardrailAdapter;
import com.axonivy.utils.smart.workflow.guardrails.adapter.OutputGuardrailAdapter;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.provider.GuardrailProvider;
import com.axonivy.utils.smart.workflow.spi.internal.SpiLoader;
import com.axonivy.utils.smart.workflow.spi.internal.SpiProject;

import ch.ivyteam.ivy.environment.Ivy;

public class GuardrailCollector {
  public static final String DEFAULT_INPUT_GUARDRAILS = "AI.Guardrails.DefaultInput";
  public static final String DEFAULT_OUTPUT_GUARDRAILS = "AI.Guardrails.DefaultOutput";

  public static Set<GuardrailProvider> allProviders() {
    var project = SpiProject.getSmartWorkflowPmv().project();
    return new SpiLoader(project).load(GuardrailProvider.class);
  }

  public static List<String> allInputGuardrailNames() {
    return allGuardrailNames(GuardrailProvider::getInputGuardrails);
  }

  public static List<InputGuardrailAdapter> inputGuardrailAdapters(List<String> filters) {
    return guardrailAdapters(filters,
        DEFAULT_INPUT_GUARDRAILS,
        GuardrailProvider::getInputGuardrails,
        InputGuardrailAdapter::new);
  }

  public static List<String> allOutputGuardrailNames() {
    return allGuardrailNames(GuardrailProvider::getOutputGuardrails);
  }

  public static List<OutputGuardrailAdapter> outputGuardrailAdapters(List<String> filters) {
    return guardrailAdapters(filters,
        DEFAULT_OUTPUT_GUARDRAILS,
        GuardrailProvider::getOutputGuardrails,
        OutputGuardrailAdapter::new);
  }

  private static <G extends SmartWorkflowGuardrail> List<String> allGuardrailNames(
      Function<GuardrailProvider, List<G>> providerExtractor) {
    Set<String> uniqueNames = allProviders().stream()
        .flatMap(p -> providerExtractor.apply(p).stream())
        .map(SmartWorkflowGuardrail::name)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    return List.copyOf(uniqueNames);
  }

  private static <G extends SmartWorkflowGuardrail, A extends AbstractGuardrailAdapter<G>> List<A> guardrailAdapters(
      List<String> filters,
      String defaultVariableKey,
      Function<GuardrailProvider, List<G>> providerExtractor,
      Function<G, A> adapterFactory) {

    Set<String> requestedNames = new LinkedHashSet<>(
        (filters != null && !filters.isEmpty()) ? filters : readVariableNames(defaultVariableKey));

    if (requestedNames.isEmpty()) {
      return List.of();
    }

    Map<String, G> guardrailsByName = allProviders().stream()
        .flatMap(p -> providerExtractor.apply(p).stream())
        .collect(Collectors.toMap(SmartWorkflowGuardrail::name, g -> g, (existing, dup) -> existing));

    return requestedNames.stream()
        .filter(guardrailsByName::containsKey)
        .map(guardrailsByName::get)
        .map(adapterFactory)
        .collect(Collectors.toList());
  }

  private static List<String> readVariableNames(String variableKey) {
    var configuredValue = StringUtils.defaultString(Ivy.var().get(variableKey));
    return Arrays.stream(StringUtils.split(configuredValue, ','))
        .map(String::strip)
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.toList());
  }
}
