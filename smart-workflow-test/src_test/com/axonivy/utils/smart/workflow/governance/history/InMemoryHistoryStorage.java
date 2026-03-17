package com.axonivy.utils.smart.workflow.governance.history;

import java.util.ArrayList;
import java.util.List;

class InMemoryHistoryStorage implements HistoryStorage {

  private final List<ChatHistoryEntry> entries = new ArrayList<>();

  @Override
  public List<ChatHistoryEntry> findAll() {
    return entries;
  }

  @Override
  public void save(ChatHistoryEntry entry) {
    if (!entries.contains(entry)) entries.add(entry);
  }

  @Override
  public void delete(ChatHistoryEntry entry) {
    entries.remove(entry);
  }
}
