package com.axonivy.utils.smart.workflow.governance.history;

import java.util.List;

import ch.ivyteam.ivy.environment.Ivy;

class IvyRepoHistoryStorage implements HistoryStorage {

  @Override
  public List<ChatHistoryEntry> findAll() {
    return Ivy.repo().search(ChatHistoryEntry.class).execute().getAll();
  }

  @Override
  public void save(ChatHistoryEntry entry) {
    Ivy.repo().save(entry);
  }

  @Override
  public void delete(ChatHistoryEntry entry) {
    Ivy.repo().delete(entry);
  }
}
