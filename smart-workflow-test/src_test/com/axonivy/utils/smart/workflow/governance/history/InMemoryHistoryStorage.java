package com.axonivy.utils.smart.workflow.governance.history;

import java.util.ArrayList;
import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.storage.HistoryStorage;

class InMemoryHistoryStorage implements HistoryStorage {

  private final List<AgentConversationEntry> entries = new ArrayList<>();

  @Override
  public List<AgentConversationEntry> findAll() {
    return entries;
  }

  @Override
  public List<AgentConversationEntry> findByCaseUuid(String caseUuid) {
    return entries.stream()
        .filter(entry -> caseUuid.equalsIgnoreCase(entry.getCaseUuid()))
        .toList();
  }

  @Override
  public void save(AgentConversationEntry entry) {
    if (!entries.contains(entry)) entries.add(entry);
  }

  @Override
  public void delete(AgentConversationEntry entry) {
    entries.remove(entry);
  }
}
