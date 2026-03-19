package com.axonivy.utils.smart.workflow.governance.history;

import java.util.List;

public interface ToolExecutionStorage {
  List<ToolExecutionEntry> findAll();

  void save(ToolExecutionEntry entry);
}
