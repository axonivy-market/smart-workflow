package com.axonivy.utils.smart.workflow.guardrails;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.guardrails.adapter.AbstractGuardrailAdapter;
import com.axonivy.utils.smart.workflow.guardrails.adapter.InputGuardrailAdapter;
import com.axonivy.utils.smart.workflow.guardrails.adapter.OutputGuardrailAdapter;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.entity.internal.SmartWorkflowInternalInputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.entity.internal.SmartWorkflowInternalOutputGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.provider.GuardrailProvider;
import com.axonivy.utils.smart.workflow.spi.internal.SpiLoader;
import com.axonivy.utils.smart.workflow.spi.internal.SpiProject;

import ch.ivyteam.ivy.environment.Ivy;

public class GuardrailCollector {
  public static final String DEFAULT_INPUT_GUARDRAILS = "AI.Guardrails.DefaultInput";
  public static final String DEFAULT_OUTPUT_GUARDRAILS = "AI.Guardrails.DefaultOutput";

  public static Set<GuardrailProvider> allProviders() {
    var pmv = SpiProject.getSmartWorkflowPmv();
    return new SpiLoader(pmv).load(GuardrailProvider.class);
  }

  public static List<String> allInputGuardrailNames() {
    return allGuardrailNames(GuardrailProvider::getInputGuardrails);
  }

  public static List<InputGuardrailAdapter> inputGuardrailAdapters(List<String> filters) {
    return inputGuardrailAdapters(allProviders(), filters);
  }

  public static List<InputGuardrailAdapter> inputGuardrailAdapters(Set<GuardrailProvider> providers, List<String> filters) {
    return guardrailAdapters(providers, filters,
        DEFAULT_INPUT_GUARDRAILS,
        GuardrailProvider::getInputGuardrails,
        InputGuardrailAdapter::new);
  }

  public static List<String> allOutputGuardrailNames() {
    return allGuardrailNames(GuardrailProvider::getOutputGuardrails);
  }

  public static List<OutputGuardrailAdapter> outputGuardrailAdapters(List<String> filters) {
    return outputGuardrailAdapters(allProviders(), filters);
  }

  public static List<OutputGuardrailAdapter> outputGuardrailAdapters(Set<GuardrailProvider> providers, List<String> filters) {
    return guardrailAdapters(providers, filters,
        DEFAULT_OUTPUT_GUARDRAILS,
        GuardrailProvider::getOutputGuardrails,
        OutputGuardrailAdapter::new);
  }

  private static <G extends SmartWorkflowGuardrail> List<String> allGuardrailNames(
      Function<GuardrailProvider, List<G>> providerExtractor) {
    Set<String> uniqueNames = allProviders().stream()
        .flatMap(p -> providerExtractor.apply(p).stream())
        .filter(Predicate.not(GuardrailCollector::isInternal))
        .map(SmartWorkflowGuardrail::name)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    return List.copyOf(uniqueNames);
  }

  private static <G extends SmartWorkflowGuardrail, A extends AbstractGuardrailAdapter<G>> List<A> guardrailAdapters(
      Set<GuardrailProvider> providers,
      List<String> filters,
      String defaultVariableKey,
      Function<GuardrailProvider, List<G>> providerExtractor,
      Function<G, A> adapterFactory) {

    Set<String> requestedNames = new LinkedHashSet<>(
        (filters != null && !filters.isEmpty()) ? filters : readVariableNames(defaultVariableKey));

    Map<String, G> guardrailsByName = new LinkedHashMap<>();

    providers.stream()
      .flatMap(p -> providerExtractor.apply(p).stream())
      .forEach(guardrail -> guardrailsByName.putIfAbsent(guardrail.name(), guardrail));
   
    LinkedHashSet<String> effectiveRequestedNames = internalGuardrailNames(guardrailsByName);
    effectiveRequestedNames.addAll(requestedNames);

    return effectiveRequestedNames.stream()
        .map(guardrailsByName::get)
        .filter(Objects::nonNull)
        .map(adapterFactory)
        .toList();
  }

  private static <G extends SmartWorkflowGuardrail> LinkedHashSet<String> internalGuardrailNames(Map<String, G> guardrailsByName) {
    return guardrailsByName.entrySet().stream()
        .filter(e -> isInternal(e.getValue()))
        .map(Map.Entry::getKey)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private static List<String> readVariableNames(String variableKey) {
    var variable = Ivy.var().get(variableKey);

    if(variable == null || variable.isBlank()) {
      return List.of();
    }

    return Arrays.stream(StringUtils.split(variable, ','))
        .map(String::strip)
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.toList());
  }

  private static boolean isInternal(SmartWorkflowGuardrail guardrail) {
    return guardrail instanceof SmartWorkflowInternalInputGuardrail || guardrail instanceof SmartWorkflowInternalOutputGuardrail;
  }
}