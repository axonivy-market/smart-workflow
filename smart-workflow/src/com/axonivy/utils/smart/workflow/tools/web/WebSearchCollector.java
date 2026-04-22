package com.axonivy.utils.smart.workflow.tools.web;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.spi.internal.SpiLoader;
import com.axonivy.utils.smart.workflow.spi.internal.SpiProject;

import ch.ivyteam.ivy.environment.Ivy;

public class WebSearchCollector {

  public static List<SmartWebSearchEngine> allEngines() {
    var project = SpiProject.getSmartWorkflowPmv().project();
    return new SpiLoader(project).load(SmartWebSearchEngineProvider.class)
        .stream()
        .flatMap(provider -> getEngines(provider).stream())
        .collect(Collectors.toList());
  }

  public static Optional<SmartWebSearchEngine> findEngine() {
    var configured = StringUtils.trimToEmpty(Ivy.var().get(WebSearchConf.ENGINE));
    var engines = allEngines();
    if (!configured.isEmpty()) {
      return engines.stream()
          .filter(e -> e.name().equalsIgnoreCase(configured))
          .findFirst();
    }
    return engines.stream().findFirst();
  }

  private static List<SmartWebSearchEngine> getEngines(SmartWebSearchEngineProvider provider) {
    try {
      return provider.getEngines();
    } catch (Exception e) {
      Ivy.log().error("Failed to load engines from provider: " + provider.name(), e);
      return List.of();
    }
  }
}
