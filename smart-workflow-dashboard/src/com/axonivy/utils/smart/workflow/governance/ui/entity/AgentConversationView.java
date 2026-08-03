package com.axonivy.utils.smart.workflow.governance.ui.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.GuardrailExecution;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.ToolExecution;
import com.axonivy.utils.smart.workflow.governance.history.ChatHistoryJsonParser;
import com.axonivy.utils.smart.workflow.governance.history.ChatHistoryJsonParser.TokenUsage;
import com.axonivy.utils.smart.workflow.governance.utils.DatePatternUtils;

public class AgentConversationView {

  private static final String NO_DATE = "—";

  private final AgentConversationEntry entry;
  private TokenUsage tokenUsage;
  private List<ToolExecution> toolExecutions;
  private List<GuardrailExecution> guardrailExecutions;

  public AgentConversationView(AgentConversationEntry entry) {
    this.entry = entry;
  }

  public AgentConversationEntry getEntry() { return entry; }

  public String getCaseUuid()    { return entry.getCaseUuid(); }
  public String getTaskUuid()    { return entry.getTaskUuid(); }
  public String getAgentId()     { return entry.getAgentId(); }
  public String getAgentName()   { return entry.getAgentName(); }
  public String getProcessName() { return entry.getProcessName(); }

  public String getLastUpdatedText() {
    if (entry.getLastUpdated() == null) return NO_DATE;
    try {
      return LocalDateTime.parse(entry.getLastUpdated())
          .format(DatePatternUtils.dateTimeFormatter());
    } catch (Exception e) {
      return entry.getLastUpdated();
    }
  }

  public int getMessageCount() {
    return ChatHistoryJsonParser.getMessageCount(entry);
  }

  public int getTotalTokens() {
    return tokenUsage().totalTokens();
  }

  public String getModelName() {
    return tokenUsage().modelName();
  }

  public List<ToolExecution> getToolExecutions() {
    if (toolExecutions == null) {
      toolExecutions = List.copyOf(entry.getToolExecutions());
    }
    return toolExecutions;
  }

  public List<GuardrailExecution> getGuardrailExecutions() {
    if (guardrailExecutions == null) {
      guardrailExecutions = List.copyOf(entry.getGuardrailExecutions());
    }
    return guardrailExecutions;
  }

  private TokenUsage tokenUsage() {
    if (tokenUsage == null) {
      tokenUsage = ChatHistoryJsonParser.parseTokenUsage(entry);
    }
    return tokenUsage;
  }
}
