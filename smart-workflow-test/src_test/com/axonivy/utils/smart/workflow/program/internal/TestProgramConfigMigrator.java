package com.axonivy.utils.smart.workflow.program.internal;

import java.util.Map;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestProgramConfigMigrator {
  
  @Test
  void v2_ivyScriptList() {
    var config = migrate(Map.of("tools", "[\"Weather\", \"Whoami\"]"));
    assertThat(config.get("tools")).isEqualTo("Weather,Whoami");
  }

  @Test
  void v2_ivyScriptList_empty() {
    var config = migrate(Map.of("tools", "[]"));
    assertThat(config.get("tools")).isEqualTo("");
  }

  @Test
  void v2_ivyScriptList_alreadyMigrated() {
    var config = migrate(Map.of("tools", "Weather,Whoami")); 
    assertThat(config.get("tools"))
      .as("Resilient to already migrated config")
      .isEqualTo("Weather,Whoami");
  }

  @Test
  void v2_ivyScriptList_keepUnchangedKeys() {
    var config = migrate(Map.of("other", "[\"a\", \"b\"]"));
    assertThat(config.get("other")).isEqualTo("[\"a\", \"b\"]");
  }

  @Test
  void v2_ivyScriptList_evaluated() {
    var config = migrate(Map.of("tools", "in.myTools"));
    assertThat(config.get("tools"))
        .as("Migration of evaluated fields is no longer supported nor migratable; keep value as reference")
        .isEqualTo("in.myTools");
  }

  private static Map<String, String> migrate(Map<String, String> config) {
    return new AgentConfigMigrator().migrateConfig(config);
  }

}
