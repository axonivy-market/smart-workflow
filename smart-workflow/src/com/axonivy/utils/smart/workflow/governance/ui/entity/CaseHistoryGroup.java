package com.axonivy.utils.smart.workflow.governance.ui.entity;

import java.util.Comparator;
import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.internal.CaseService;

import ch.ivyteam.ivy.workflow.ICase;

public class CaseHistoryGroup implements HistoryGroupView {

  private final String caseUuid;
  private final List<AgentConversationEntry> tasks;
  private final HistoryGroupView stats;
  private String caseDisplayName;

  public CaseHistoryGroup(String caseUuid, List<AgentConversationEntry> tasks) {
    this.caseUuid = caseUuid;
    this.tasks = tasks;
    this.stats = HistoryGroupView.of(tasks);
  }

  public String getCaseUuid() { return caseUuid; }
  public List<AgentConversationEntry> getTasks() { return tasks; }
  public int getTaskCount() { return tasks.size(); }

  public String getCaseDisplayName() {
    if (caseDisplayName == null) {
      caseDisplayName = CaseService.getDisplayName(caseUuid);
    }
    return caseDisplayName;
  }

  public String getLastUpdatedRaw() {
    return tasks.stream()
        .filter(e -> e.getLastUpdated() != null)
        .map(AgentConversationEntry::getLastUpdated)
        .max(Comparator.naturalOrder())
        .orElse("");
  }

  @Override
  public long getCaseId() {
    ICase ivyCase = CaseService.findCase(caseUuid);
    return ivyCase != null ? ivyCase.getId() : -1L;
  }

  @Override public String getLastUpdatedText() { return stats.getLastUpdatedText(); }
  @Override public int    getMessageCount()    { return stats.getMessageCount(); }
  @Override public long   getTotalTokens()     { return stats.getTotalTokens(); }
  @Override public String getModelName()       { return stats.getModelName(); }
}