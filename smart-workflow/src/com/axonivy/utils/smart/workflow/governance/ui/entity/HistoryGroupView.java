package com.axonivy.utils.smart.workflow.governance.ui.entity;

import java.util.Comparator;
import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.internal.ChatHistoryJsonParser;

public interface HistoryGroupView {

  String NO_DATE = "—";

  String getLastUpdatedText();

  int getMessageCount();

  int getTotalTokens();

  String getModelName();

  default long getCaseId() { return 0L; }

  static HistoryGroupView of(List<AgentConversationEntry> entries) {
    return new HistoryEntryStats(entries);
  }
}

class HistoryEntryStats implements HistoryGroupView {


  private final List<AgentConversationEntry> entries;

  HistoryEntryStats(List<AgentConversationEntry> entries) {
    this.entries = entries;
  }

  @Override
  public String getLastUpdatedText() {
    return entries.stream()
        .filter(entry -> entry.getLastUpdated() != null)
        .max(Comparator.comparing(AgentConversationEntry::getLastUpdated))
        .map(e -> new AgentConversationView(e))
        .map(AgentConversationView::getLastUpdatedText)
        .orElse(NO_DATE);
  }

  @Override
  public int getMessageCount() {
    return entries.stream().mapToInt(e -> ChatHistoryJsonParser.getMessageCount(e)).sum();
  }

  @Override
  public int getTotalTokens() {
    return entries.stream().mapToInt(e -> ChatHistoryJsonParser.getTotalTokens(e)).sum();
  }

  @Override
  public String getModelName() {
    return entries.isEmpty() ? "unknown" : ChatHistoryJsonParser.getModelName(entries.get(0));
  }
}