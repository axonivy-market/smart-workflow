package com.axonivy.utils.smart.workflow.governance.history.storage.internal;

import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.entity.ToolExecutionEntry;
import com.axonivy.utils.smart.workflow.governance.history.storage.ToolExecutionStorage;

import ch.ivyteam.ivy.environment.Ivy;

public class IvyRepoToolExecutionStorage implements ToolExecutionStorage {

  private static final int MAX_QUERY_RESULTS = 100;

  @Override
  public List<ToolExecutionEntry> findAll() {
    return Ivy.repo().search(ToolExecutionEntry.class).limit(MAX_QUERY_RESULTS).execute().getAll();
  }

  @Override
  public List<ToolExecutionEntry> findByCaseUuid(String caseUuid) {
    return Ivy.repo().search(ToolExecutionEntry.class)
        .textField("caseUuid").isEqualToIgnoringCase(caseUuid)
        .execute().getAll();
  }

  @Override
  public void save(ToolExecutionEntry entry) {
    Ivy.repo().save(entry);
  }
}
