package com.axonivy.utils.smart.workflow.guardrails;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.axonivy.utils.smart.workflow.guardrails.adapter.AbstractGuardrailAdapter;
import com.axonivy.utils.smart.workflow.guardrails.adapter.InputGuardrailAdapter;
import com.axonivy.utils.smart.workflow.guardrails.adapter.OutputGuardrailAdapter;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowGuardrail;
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
    return allGuardrailNames(
        new DefaultGuardrailProvider().getInputGuardrails(),
        GuardrailProvider::getInputGuardrails);
  }

  public static List<InputGuardrailAdapter> inputGuardrailAdapters(List<String> filters) {
    return guardrailAdapters(filters,
        new DefaultGuardrailProvider().getFilteredDefaultInputGuardrails(),
        GuardrailProvider::getInputGuardrails,
        InputGuardrailAdapter::new);
  }

  public static List<String> allOutputGuardrailNames() {
    return allGuardrailNames(
        new DefaultGuardrailProvider().getOutputGuardrails(),
        GuardrailProvider::getOutputGuardrails);
  }

  public static List<OutputGuardrailAdapter> outputGuardrailAdapters(List<String> filters) {
    return guardrailAdapters(filters,
        new DefaultGuardrailProvider().getFilteredDefaultOutputGuardrails(),
        GuardrailProvider::getOutputGuardrails,
        OutputGuardrailAdapter::new);
  }

  private static <G extends SmartWorkflowGuardrail> List<String> allGuardrailNames(
      List<G> defaults,
      Function<GuardrailProvider, List<G>> providerExtractor) {
    List<G> guardrails = new ArrayList<>(defaults);
    allProviders().stream()
        .flatMap(p -> providerExtractor.apply(p).stream())
        .forEach(guardrails::add);
    return new ArrayList<>(guardrails.stream()
        .map(SmartWorkflowGuardrail::name)
        .collect(Collectors.toCollection(LinkedHashSet::new)));
  }

  private static <G extends SmartWorkflowGuardrail, A extends AbstractGuardrailAdapter<G>> List<A> guardrailAdapters(
      List<String> filters,
      List<G> defaults,
      Function<GuardrailProvider, List<G>> providerExtractor,
      Function<G, A> adapterFactory) {
    List<G> guardrails = new ArrayList<>(defaults);

    if (CollectionUtils.isEmpty(filters)) {
      return new ArrayList<>(guardrails.stream()
          .map(adapterFactory)
          .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    allProviders().stream()
        .flatMap(p -> providerExtractor.apply(p).stream())
        .forEach(guardrails::add);

    return new ArrayList<>(guardrails.stream()
        .filter(g -> filters.contains(g.name()))
        .map(adapterFactory)
        .collect(Collectors.toCollection(LinkedHashSet::new)));
  }
}
