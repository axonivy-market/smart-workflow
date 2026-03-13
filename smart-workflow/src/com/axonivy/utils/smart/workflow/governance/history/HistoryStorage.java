package com.axonivy.utils.smart.workflow.governance.history;

import java.util.List;

public interface HistoryStorage {
  List<ChatHistoryEntry> findAll();

  void save(ChatHistoryEntry entry);

  void delete(ChatHistoryEntry entry);
}
