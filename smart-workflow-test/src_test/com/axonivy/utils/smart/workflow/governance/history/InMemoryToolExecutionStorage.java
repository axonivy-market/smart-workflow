package com.axonivy.utils.smart.workflow.governance.history;

import java.util.ArrayList;
import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.entity.ToolExecutionEntry;
import com.axonivy.utils.smart.workflow.governance.history.storage.ToolExecutionStorage;

class InMemoryToolExecutionStorage implements ToolExecutionStorage {

  private final List<ToolExecutionEntry> entries = new ArrayList<>();

  @Override
  public List<ToolExecutionEntry> findAll() {
    return entries;
  }

  @Override
  public List<ToolExecutionEntry> findByCaseUuid(String caseUuid) {
    return entries.stream()
        .filter(entry -> caseUuid.equalsIgnoreCase(entry.getCaseUuid()))
        .toList();
  }

  @Override
  public void save(ToolExecutionEntry entry) {
    entries.add(entry);
  }
}
