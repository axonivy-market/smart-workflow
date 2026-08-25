package com.axonivy.utils.smart.workflow.program.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.process.program.migrate.ProgramConfigMigrator;

public class AgentConfigMigrator implements ProgramConfigMigrator {

  @Override
  public Map<String, String> migrateConfig(Map<String, String> config) {
    return toMultiSelectPicker(config);
  }

  private Map<String, String> toMultiSelectPicker(Map<String, String> config) {
    if (config.isEmpty()) {
      return config;
    }
    var latest = new HashMap<>(config);
    toComma(latest, "tools");
    toComma(latest, "inputGuardrails");
    toComma(latest, "outputGuardrails");
    Optional.ofNullable(config.get("provider"))
      .filter(Predicate.not(String::isBlank))
      .map(provider -> StringUtils.substringBetween(provider, "\""))
      .ifPresent(newProvider -> latest.put("provider", newProvider));
    return latest;
  }

  private void toComma(HashMap<String, String> config, String what) {
    ivyListToComma(config.get(what))
        .ifPresent(newTools -> config.put(what, newTools));
  }

  private static Optional<String> ivyListToComma(String list) {
    if (list == null || list.isBlank()) {
      return Optional.empty();
    }
    list = list.trim();
    if (list.startsWith("[") && list.endsWith("]")) {
      list = StringUtils.substringBetween(list, "[", "]");
      if (list.isEmpty()) {
        return Optional.of("");
      }
      var newTools = Arrays.stream(list.split(","))
          .map(String::trim)
          .map(tool -> StringUtils.substringBetween(tool, "\""))
          .collect(Collectors.joining(","));
      return Optional.of(newTools);
    }
    return Optional.empty();
  }

}
