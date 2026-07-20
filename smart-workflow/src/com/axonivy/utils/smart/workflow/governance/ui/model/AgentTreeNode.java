package com.axonivy.utils.smart.workflow.governance.ui.model;

import java.time.LocalDateTime;
import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.ToolExecution;
import com.axonivy.utils.smart.workflow.governance.history.internal.ChatHistoryJsonParser;
import com.axonivy.utils.smart.workflow.governance.history.internal.ChatHistoryJsonParser.ArgumentEntry;
import com.axonivy.utils.smart.workflow.governance.utils.DatePatternUtils;

public class AgentTreeNode {

  private static final String DEFAULT_AGENT_LABEL = "Agent";

  private final AgentConversationEntry entry;
  private final int toolCount;

  public AgentTreeNode(AgentConversationEntry entry) {
    this.entry = entry;
    List<ToolExecution> tools = entry.getToolExecutions();
    this.toolCount = tools != null ? tools.size() : 0;
  }

  public AgentConversationEntry getEntry() {
    return entry;
  }

  public String getAgentId() {
    String id = entry.getAgentId();
    return (id != null && !id.isBlank()) ? id : DEFAULT_AGENT_LABEL;
  }

  public String getDisplayName() {
    return entry.getDisplayName();
  }

  public int getMessageCount() {
    return ChatHistoryJsonParser.getMessageCount(entry);
  }

  public long getTotalTokens() {
    return ChatHistoryJsonParser.getTotalTokens(entry);
  }

  public String getModelName() {
    return ChatHistoryJsonParser.getModelName(entry);
  }

  public int getToolCount() {
    return toolCount;
  }

  public long getAvgDurationMs() {
    return ChatHistoryJsonParser.getAvgDurationMs(entry);
  }

  public LocalDateTime getLastUpdated() {
    return DatePatternUtils.parseLastUpdated(entry.getLastUpdated());
  }

  public String getLastUpdatedText() {
    LocalDateTime dt = getLastUpdated();
    return dt != null ? dt.format(DatePatternUtils.DISPLAY_FMT) : "—";
  }

  public List<ToolView> getTools() {
    List<ToolExecution> executions = entry.getToolExecutions();
    if (executions == null) {
      return List.of();
    }
    return executions.stream().map(ToolView::new).toList();
  }

  public record ToolView(ToolExecution exec) {
    public String getToolName() {
      return exec.toolName();
    }

    public String getArguments() {
      return exec.arguments();
    }

    public String getResultText() {
      return exec.resultText();
    }

    public String getExecutedAt() {
      return exec.executedAt();
    }

    public List<ArgumentEntry> getArgumentEntries() {
      return ChatHistoryJsonParser.getArgumentEntries(exec);
    }
  }
}
