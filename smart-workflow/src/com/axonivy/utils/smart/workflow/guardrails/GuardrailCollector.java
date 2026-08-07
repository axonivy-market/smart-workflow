package com.axonivy.utils.smart.workflow.guardrails;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
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
        InputGuardrailAdapter::new,
        true);
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
        OutputGuardrailAdapter::new,
        false);
  }

  /**
   * Names selectable in the agent editor's guardrail pickers. Always-on guardrails are excluded since
   * they cannot be deselected — they run for every agent call regardless of what is picked here.
   */
  private static <G extends SmartWorkflowGuardrail> List<String> allGuardrailNames(
      Function<GuardrailProvider, List<G>> providerExtractor) {
    Set<String> uniqueNames = allProviders().stream()
        .flatMap(p -> providerExtractor.apply(p).stream())
        .filter(Predicate.not(SmartWorkflowGuardrail::alwaysOn))
        .map(SmartWorkflowGuardrail::name)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    return List.copyOf(uniqueNames);
  }

  private static <G extends SmartWorkflowGuardrail, A extends AbstractGuardrailAdapter<G>> List<A> guardrailAdapters(
      Set<GuardrailProvider> providers,
      List<String> filters,
      String defaultVariableKey,
      Function<GuardrailProvider, List<G>> providerExtractor,
      Function<G, A> adapterFactory,
      boolean alwaysOnFirst) {

    Map<String, G> guardrailsByName = providers.stream()
        .flatMap(p -> providerExtractor.apply(p).stream())
        .collect(Collectors.toMap(SmartWorkflowGuardrail::name, g -> g, (existing, _) -> existing, LinkedHashMap::new));

    List<String> alwaysOnNames = guardrailsByName.values().stream()
        .filter(SmartWorkflowGuardrail::alwaysOn)
        .map(SmartWorkflowGuardrail::name)
        .toList();

    List<String> configuredNames = (filters != null && !filters.isEmpty()) ? filters : readVariableNames(defaultVariableKey);

    Set<String> requestedNames = new LinkedHashSet<>();
    if (alwaysOnFirst) {
      requestedNames.addAll(alwaysOnNames);
    }
    requestedNames.addAll(configuredNames);
    if (!alwaysOnFirst) {
      requestedNames.addAll(alwaysOnNames);
    }

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
