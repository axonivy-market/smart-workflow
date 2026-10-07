package com.axonivy.utils.smart.workflow.governance.ui.model;

import java.time.LocalDateTime;
import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.ChatHistoryJsonParser;
import com.axonivy.utils.smart.workflow.governance.history.ChatHistoryJsonParser.ArgumentEntry;
import com.axonivy.utils.smart.workflow.governance.history.ChatHistoryJsonParser.TokenUsage;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.ToolExecution;
import com.axonivy.utils.smart.workflow.governance.utils.DatePatternUtils;

public class AgentTreeNode {

  private static final String DEFAULT_AGENT_LABEL = "Agent";

  private final AgentConversationEntry entry;
  private final List<ToolExecution> tools;
  private final int toolCount;
  private final TokenUsage tokenUsage;

  public AgentTreeNode(AgentConversationEntry entry) {
    this.entry = entry;
    this.tools = entry.getToolExecutions();
    this.toolCount = tools.size();
    this.tokenUsage = ChatHistoryJsonParser.parseTokenUsage(entry);
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
    return tokenUsage.totalTokens();
  }

  public String getModelName() {
    return tokenUsage.modelName();
  }

  public int getToolCount() {
    return toolCount;
  }

  public long getAvgDurationMs() {
    return tokenUsage.avgDurationMs();
  }

  public LocalDateTime getLastUpdated() {
    return DatePatternUtils.parseLastUpdated(entry.getLastUpdated());
  }

  public String getLastUpdatedText() {
    LocalDateTime dt = getLastUpdated();
    return dt != null ? dt.format(DatePatternUtils.DISPLAY_FMT) : "—";
  }

  public List<ToolView> getTools() {
    return tools.stream().map(ToolView::new).toList();
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
