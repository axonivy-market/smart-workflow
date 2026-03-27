package com.axonivy.utils.smart.workflow.governance.ui.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry.ToolExecution;
import com.axonivy.utils.smart.workflow.governance.utils.ChatHistoryJsonParser;
import com.axonivy.utils.smart.workflow.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;

public class AgentTreeNode {

  private final AgentConversationEntry entry;
  private final int toolCount;
  private transient boolean isPretty = true;

  public AgentTreeNode(AgentConversationEntry entry) {
    this.entry = entry;
    List<ToolExecution> tools = entry.getToolExecutions();
    this.toolCount = tools != null ? tools.size() : 0;
  }

  public AgentConversationEntry getEntry() { return entry; }

  public String getAgentId() {
    String id = entry.getAgentId();
    return (id != null && !id.isBlank()) ? id : "Agent";
  }

  public String getDisplayName() {
    String name = entry.getAgentName();
    return (name != null && !name.isBlank()) ? name : getAgentId();
  }
  public int getMessageCount() { return ChatHistoryJsonParser.getMessageCount(entry); }
  public int getTotalTokens() { return ChatHistoryJsonParser.getTotalTokens(entry); }
  public String getModelName() { return ChatHistoryJsonParser.getModelName(entry); }
  public int getToolCount() { return toolCount; }
  public boolean getIsPretty()                 { return isPretty; }
  public void    setIsPretty(boolean isPretty) { this.isPretty = isPretty; }
  public long getAvgDurationMs() { return ChatHistoryJsonParser.getAvgDurationMs(entry); }
  public String getStartTime()   { return ChatHistoryJsonParser.getStartTime(entry); }
  public String getLastUpdatedText() { return entry.getLastUpdatedText(); }

  public LocalDateTime getLastUpdated() {
    String s = entry.getLastUpdated();
    if (s == null) return null;
    try { return LocalDateTime.parse(s); } catch (Exception e) { return null; }
  }

  public List<ToolView> getTools() {
    List<ToolExecution> executions = entry.getToolExecutions();
    if (executions == null) return List.of();
    return executions.stream().map(ToolView::new).toList();
  }

  /** Key-value pair for a single tool argument, used by the per-argument tab view. */
  public static class ArgumentEntry {
    private final String key;
    private final String value;
    public ArgumentEntry(String key, String value) { this.key = key; this.value = value; }
    public String getKey()   { return key; }
    public String getValue() { return value; }
  }

  /** JSF-friendly wrapper around {@link ToolExecution} record (records lack getX() accessors). */
  public static class ToolView {
    private final ToolExecution exec;
    public ToolView(ToolExecution exec) { this.exec = exec; }
    public String getToolName()   { return exec.toolName(); }
    public String getArguments()  { return exec.arguments(); }
    public String getResultText() { return exec.resultText(); }
    public String getExecutedAt() { return exec.executedAt(); }

    public List<ArgumentEntry> getArgumentEntries() {
      String args = exec.arguments();
      if (args == null || args.isBlank()) return List.of();
      try {
        JsonNode node = JsonUtils.getObjectMapper().readTree(args);
        if (!node.isObject()) return List.of();
        List<ArgumentEntry> entries = new ArrayList<>();
        var fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
          var key = fieldNames.next();
          var val = node.get(key);
          entries.add(new ArgumentEntry(key,
              val.isTextual() ? val.asText() : val.toPrettyString()));
        }
        return entries;
      } catch (Exception ex) {
        return List.of();
      }
    }
  }
}
