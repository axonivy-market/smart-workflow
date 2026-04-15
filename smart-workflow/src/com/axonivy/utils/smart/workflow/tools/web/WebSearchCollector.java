package com.axonivy.utils.smart.workflow.tools.web;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.axonivy.utils.smart.workflow.spi.internal.SpiLoader;
import com.axonivy.utils.smart.workflow.spi.internal.SpiProject;

public class WebSearchCollector {

  public static List<SmartWebSearchEngine> allEngines() {
    var project = SpiProject.getSmartWorkflowPmv().project();
    return new SpiLoader(project).load(SmartWebSearchEngineProvider.class)
        .stream()
        .flatMap(provider -> provider.getEngines().stream())
        .collect(Collectors.toList());
  }

  public static Optional<SmartWebSearchEngine> findEngine() {
    return allEngines().stream().findFirst();
  }
}
