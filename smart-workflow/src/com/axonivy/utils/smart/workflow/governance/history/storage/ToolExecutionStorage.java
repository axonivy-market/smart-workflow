package com.axonivy.utils.smart.workflow.governance.history.storage;

import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.entity.ToolExecutionEntry;

public interface ToolExecutionStorage {
  List<ToolExecutionEntry> findAll();

  List<ToolExecutionEntry> findByCaseUuid(String caseUuid);

  void save(ToolExecutionEntry entry);
}
