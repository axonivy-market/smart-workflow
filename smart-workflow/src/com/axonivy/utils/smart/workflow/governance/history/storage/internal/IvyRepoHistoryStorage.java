package com.axonivy.utils.smart.workflow.governance.history.storage.internal;

import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.storage.HistoryStorage;

import ch.ivyteam.ivy.environment.Ivy;

public class IvyRepoHistoryStorage implements HistoryStorage {

  private static final int MAX_QUERY_RESULTS = 100;

  @Override
  public List<AgentConversationEntry> findAll() {
    return Ivy.repo().search(AgentConversationEntry.class).limit(MAX_QUERY_RESULTS).execute().getAll();
  }

  @Override
  public List<AgentConversationEntry> findByCaseUuid(String caseUuid) {
    return Ivy.repo().search(AgentConversationEntry.class)
        .textField("caseUuid").isEqualToIgnoringCase(caseUuid)
        .execute().getAll();
  }

  @Override
  public void save(AgentConversationEntry entry) {
    Ivy.repo().save(entry);
  }

  @Override
  public void delete(AgentConversationEntry entry) {
    Ivy.repo().delete(entry);
  }
}
