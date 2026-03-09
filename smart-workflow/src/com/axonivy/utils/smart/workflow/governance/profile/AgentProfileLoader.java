package com.axonivy.utils.smart.workflow.governance.profile;

import java.util.List;

import com.axonivy.utils.smart.workflow.utils.JsonUtils;

import ch.ivyteam.ivy.environment.Ivy;

public class AgentProfileLoader {
  public static final String VARIABLE_KEY = "AI.AgentProfiles";

  public static List<AgentProfile> loadAll() {
    return JsonUtils.jsonValueToEntities(Ivy.var().get(VARIABLE_KEY), AgentProfile.class);
  }

  public static AgentProfile loadByName(String name) {
    return loadAll().stream()
        .filter(p -> name.equals(p.getName()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No agent profile found: " + name));
  }

  public static List<String> profileNames() {
    return loadAll().stream()
        .map(AgentProfile::getName)
        .toList();
  }
}
