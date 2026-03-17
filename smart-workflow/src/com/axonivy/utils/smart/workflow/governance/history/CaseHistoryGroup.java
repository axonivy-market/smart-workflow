package com.axonivy.utils.smart.workflow.governance.history;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import com.axonivy.utils.smart.workflow.governance.service.CaseService;

public class CaseHistoryGroup {

  private static final String DATE_TIME_FORMAT_PATTERN = "dd MMM yyyy HH:mm";
  private static final String NO_DATE = "—";

  private final String caseUuid;
  private final List<ChatHistoryEntry> tasks;
  private String caseDisplayName;

  public CaseHistoryGroup(String caseUuid, List<ChatHistoryEntry> tasks) {
    this.caseUuid = caseUuid;
    this.tasks = tasks;
  }

  public String getCaseUuid() { return caseUuid; }
  public List<ChatHistoryEntry> getTasks() { return tasks; }
  public int getTaskCount() { return tasks.size(); }

  public String getCaseDisplayName() {
    if (caseDisplayName == null) {
      caseDisplayName = CaseService.getDisplayName(caseUuid);
    }
    return caseDisplayName;
  }

  public String getLastUpdatedText() {
    return tasks.stream()
        .filter(t -> t.getLastUpdated() != null)
        .max(Comparator.comparing(ChatHistoryEntry::getLastUpdated))
        .map(t -> t.getLastUpdated().format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_PATTERN)))
        .orElse(NO_DATE);
  }

  public int getMessageCount() {
    return tasks.stream().mapToInt(ChatHistoryEntry::getMessageCount).sum();
  }

  public int getTotalTokens() {
    return tasks.stream().mapToInt(ChatHistoryEntry::getTotalTokens).sum();
  }

  public String getModelName() {
    return tasks.isEmpty() ? "unknown" : tasks.get(0).getModelName();
  }
}
