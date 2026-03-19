package com.axonivy.utils.smart.workflow.governance.history;

import java.util.ArrayList;
import java.util.List;

class InMemoryToolExecutionStorage implements ToolExecutionStorage {

  private final List<ToolExecutionEntry> entries = new ArrayList<>();

  @Override
  public List<ToolExecutionEntry> findAll() {
    return entries;
  }

  @Override
  public void save(ToolExecutionEntry entry) {
    entries.add(entry);
  }
}
