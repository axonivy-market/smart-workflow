package com.axonivy.utils.smart.workflow.memory.id;

import java.util.Objects;
import java.util.Optional;

import ch.ivyteam.ivy.process.program.script.ScriptingService;

public class ProcessDataField implements IdStore {

  private final ScriptingService scripting;
  private String fieldName;

  public ProcessDataField(ScriptingService scripting) {
    this(scripting, "aiMemoryId");
  }

  private ProcessDataField(ScriptingService scripting, String fieldName){
    this.scripting = scripting;
    this.fieldName = fieldName;
  }

  @Override
  public Optional<String> id() {
    try {
      return scripting.executeExpression("in." + fieldName, String.class)
        .filter(Objects::nonNull)
        .filter(id -> !id.isEmpty());
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  @Override
  public void id(String id) {
    try {
      scripting.executeScript("in." + fieldName + " = \"" + id + "\";");
    } catch (Exception ex) {//ignore
    }

  }
}
