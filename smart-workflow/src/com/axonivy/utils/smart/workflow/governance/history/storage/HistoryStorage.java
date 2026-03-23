package com.axonivy.utils.smart.workflow.governance.history.storage;

import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;

public interface HistoryStorage {
  List<AgentConversationEntry> findAll();

  List<AgentConversationEntry> findByCaseUuid(String caseUuid);

  void save(AgentConversationEntry entry);

  void delete(AgentConversationEntry entry);
}
