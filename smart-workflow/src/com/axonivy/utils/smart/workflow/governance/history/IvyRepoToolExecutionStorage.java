package com.axonivy.utils.smart.workflow.governance.history;

import java.util.List;

import ch.ivyteam.ivy.environment.Ivy;

class IvyRepoToolExecutionStorage implements ToolExecutionStorage {

  @Override
  public List<ToolExecutionEntry> findAll() {
    return Ivy.repo().search(ToolExecutionEntry.class).execute().getAll();
  }

  @Override
  public void save(ToolExecutionEntry entry) {
    Ivy.repo().save(entry);
  }
}
