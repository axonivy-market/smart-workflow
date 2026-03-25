package com.axonivy.utils.smart.workflow.governance.ui.entity;

import java.util.Comparator;
import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;

public interface HistoryGroupView {

  String DATE_TIME_FORMAT_PATTERN = "dd MMM yyyy HH:mm"; // TODO make this user locale specific
  String NO_DATE = "—";

  String getLastUpdatedText();

  int getMessageCount();

  int getTotalTokens();

  String getModelName();

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
        .map(AgentConversationEntry::getLastUpdatedText)
        .orElse(NO_DATE);
  }

  @Override
  public int getMessageCount() {
    return entries.stream().mapToInt(AgentConversationEntry::getMessageCount).sum();
  }

  @Override
  public int getTotalTokens() {
    return entries.stream().mapToInt(AgentConversationEntry::getTotalTokens).sum();
  }

  @Override
  public String getModelName() {
    return entries.isEmpty() ? "unknown" : entries.get(0).getModelName();
  }
}
