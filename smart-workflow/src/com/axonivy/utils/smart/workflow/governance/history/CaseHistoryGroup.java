package com.axonivy.utils.smart.workflow.governance.history;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.service.CaseService;
import com.axonivy.utils.smart.workflow.governance.utils.ChatHistoryJsonParser;

public class CaseHistoryGroup {

  private static final String DATE_TIME_FORMAT_PATTERN = "dd MMM yyyy HH:mm";
  private static final String NO_DATE = "—";

  private final String caseUuid;
  private final List<AgentConversationEntry> tasks;
  private String caseDisplayName;

  public CaseHistoryGroup(String caseUuid, List<AgentConversationEntry> tasks) {
    this.caseUuid = caseUuid;
    this.tasks = tasks;
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

  public String getLastUpdatedText() {
    return tasks.stream()
        .filter(t -> t.getLastUpdated() != null)
        .max(Comparator.comparing(AgentConversationEntry::getLastUpdated))
        .map(t -> {
          try {
            return LocalDateTime.parse(t.getLastUpdated())
                .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_PATTERN));
          } catch (Exception e) {
            return t.getLastUpdated();
          }
        })
        .orElse(NO_DATE);
  }

  public int getMessageCount() {
    return tasks.stream().mapToInt(ChatHistoryJsonParser::getMessageCount).sum();
  }

  public int getTotalTokens() {
    return tasks.stream().mapToInt(ChatHistoryJsonParser::getTotalTokens).sum();
  }

  public String getModelName() {
    return tasks.isEmpty() ? "unknown" : ChatHistoryJsonParser.getModelName(tasks.get(0));
  }
}
