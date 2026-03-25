package com.axonivy.utils.smart.workflow.governance.history;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.service.TaskService;
import com.axonivy.utils.smart.workflow.governance.utils.ChatHistoryJsonParser;

public class TaskHistoryGroup {

  private static final String DATE_TIME_FORMAT_PATTERN = "dd MMM yyyy HH:mm";
  private static final String NO_DATE = "—";

  private final String taskUuid;
  private final List<AgentConversationEntry> agents;
  private String taskDisplayName;

  public TaskHistoryGroup(String taskUuid, List<AgentConversationEntry> agents) {
    this.taskUuid = taskUuid;
    this.agents = agents;
  }

  public String getTaskUuid() { return taskUuid; }
  public List<AgentConversationEntry> getAgents() { return agents; }
  public int getAgentCount() { return agents.size(); }

  public String getTaskDisplayName() {
    if (taskDisplayName == null) {
      taskDisplayName = TaskService.getDisplayName(taskUuid);
    }
    return taskDisplayName;
  }

  public String getLastUpdatedText() {
    return agents.stream()
        .filter(a -> a.getLastUpdated() != null)
        .max(Comparator.comparing(AgentConversationEntry::getLastUpdated))
        .map(a -> {
          try {
            return LocalDateTime.parse(a.getLastUpdated())
                .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_PATTERN));
          } catch (Exception e) {
            return a.getLastUpdated();
          }
        })
        .orElse(NO_DATE);
  }

  public int getMessageCount() {
    return agents.stream().mapToInt(ChatHistoryJsonParser::getMessageCount).sum();
  }

  public int getTotalTokens() {
    return agents.stream().mapToInt(ChatHistoryJsonParser::getTotalTokens).sum();
  }

  public String getModelName() {
    return agents.isEmpty() ? "unknown" : ChatHistoryJsonParser.getModelName(agents.get(0));
  }
}
