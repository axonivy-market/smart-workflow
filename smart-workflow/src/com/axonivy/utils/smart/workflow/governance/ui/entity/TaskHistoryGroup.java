package com.axonivy.utils.smart.workflow.governance.ui.entity;

import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.service.TaskService;

public class TaskHistoryGroup implements HistoryGroupView {

  private final String taskUuid;
  private final List<AgentConversationEntry> agents;
  private final HistoryGroupView stats;
  private String taskDisplayName;

  public TaskHistoryGroup(String taskUuid, List<AgentConversationEntry> agents) {
    this.taskUuid = taskUuid;
    this.agents = agents;
    this.stats = HistoryGroupView.of(agents);
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

  @Override public String getLastUpdatedText() { return stats.getLastUpdatedText(); }
  @Override public int    getMessageCount()    { return stats.getMessageCount(); }
  @Override public int    getTotalTokens()     { return stats.getTotalTokens(); }
  @Override public String getModelName()       { return stats.getModelName(); }
}
