package com.axonivy.utils.smart.workflow.governance.ui.bean;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.smart.workflow.governance.history.ChatHistoryEntry;
import com.axonivy.utils.smart.workflow.governance.history.HistoryStorage;
import com.axonivy.utils.smart.workflow.governance.service.CaseService;
import com.axonivy.utils.smart.workflow.governance.ui.model.CaseTreeNode;
import com.axonivy.utils.smart.workflow.governance.ui.model.TaskTreeNode;
import com.axonivy.utils.smart.workflow.governance.utils.ChatHistoryJsonParser;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.viewer.api.ProcessViewer;
import ch.ivyteam.ivy.workflow.ICase;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;

@ManagedBean
@ViewScoped
public class ConversationsBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private CaseTreeNode caseNode;
  private CaseDetails caseDetails;
  private String selectedTaskUuid;

  public void preRender(String caseUuid) {
    if (caseUuid != null && caseNode == null) {
      loadForCase(caseUuid);
    }
  }

  private void loadForCase(String caseUuid) {
    HistoryStorage storage = HistoryStorage.create();
    List<ChatHistoryEntry> entries = storage.findAll().stream()
        .filter(e -> caseUuid.equalsIgnoreCase(e.getCaseUuid()))
        .toList();
    List<CaseTreeNode> tree = CaseTreeNode.buildTree(entries);
    caseNode = tree.isEmpty() ? null : tree.get(0);
    if (caseNode != null && !caseNode.getTasks().isEmpty()) {
      selectedTaskUuid = caseNode.getTasks().get(0).getTaskUuid();
    }
    ICase ivyCase = CaseService.findCase(caseUuid);
    caseDetails = ivyCase != null ? CaseDetails.from(ivyCase) : null;
  }

  public void selectTask(String taskUuid) {
    this.selectedTaskUuid = taskUuid;
  }

  public TaskTreeNode getSelectedTaskNode() {
    if (caseNode == null || selectedTaskUuid == null) return null;
    return caseNode.getTasks().stream()
        .filter(t -> selectedTaskUuid.equals(t.getTaskUuid()))
        .findFirst()
        .orElse(null);
  }

  /** Parses messagesJson into a list of MessageViewModels for rendering chat bubbles. */
  public List<MessageViewModel> parseMessages(ChatHistoryEntry entry) {
    if (entry == null || entry.getMessagesJson() == null) return List.of();
    try {
      List<ChatMessage> messages = ChatMessageDeserializer.messagesFromJson(entry.getMessagesJson());
      List<MessageViewModel> result = new ArrayList<>();
      for (ChatMessage msg : messages) {
        if (msg instanceof SystemMessage sm) {
          result.add(new MessageViewModel("system", sm.text()));
        } else if (msg instanceof UserMessage um) {
          String text = um.contents().stream()
              .filter(c -> c instanceof TextContent)
              .map(c -> ((TextContent) c).text())
              .collect(Collectors.joining("\n"));
          result.add(new MessageViewModel("user", text));
        } else if (msg instanceof AiMessage am) {
          result.add(new MessageViewModel("assistant", am.text()));
        } else if (msg instanceof ToolExecutionResultMessage tm) {
          result.add(new MessageViewModel("tool", "Tool: " + tm.toolName() + "\n" + tm.text()));
        }
      }
      return result;
    } catch (Exception e) {
      Ivy.log().warn("parseMessages failed for entry {0}: {1}", entry.getTaskUuid(), e.getMessage());
      return List.of();
    }
  }

  public long getCaseAvgDurationMs() {
    if (caseNode == null) return 0L;
    return (long) caseNode.getTasks().stream()
        .mapToLong(t -> ChatHistoryJsonParser.getAvgDurationMs(t.getEntry()))
        .filter(d -> d > 0)
        .average()
        .orElse(0);
  }

  public long getInputTokens(ChatHistoryEntry entry) {
    return ChatHistoryJsonParser.getInputTokens(entry);
  }

  public long getOutputTokens(ChatHistoryEntry entry) {
    return ChatHistoryJsonParser.getOutputTokens(entry);
  }

  /** Returns the ICase for the loaded case, or null if unavailable. */
  public ICase getIvyCase() {
    if (caseNode == null) return null;
    return CaseService.findCase(caseNode.getCaseUuid());
  }

  /** Returns the Ivy process viewer URL, or null if unavailable or not permitted. */
  public String getProcessViewerUrl() {
    ICase ivyCase = getIvyCase();
    if (ivyCase == null) return null;
    try {
      if (!ProcessViewer.of(ivyCase).isViewAllowed()) return null;
      return ProcessViewer.of(ivyCase).url().toWebLink().getRelative();
    } catch (Exception e) {
      Ivy.log().warn("ConversationsBean: cannot get process viewer URL for case {0}", e,
          caseNode.getCaseUuid());
      return null;
    }
  }

  /** Downloads all tasks for the current case as a JSON array. */
  public StreamedContent exportCase() {
    if (caseNode == null) return DefaultStreamedContent.builder().build();
    String json = buildCaseExportJson(caseNode);
    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
    return DefaultStreamedContent.builder()
        .name("case-" + caseNode.getCaseUuid() + "-history.json")
        .contentType("application/json")
        .stream(() -> new ByteArrayInputStream(bytes))
        .build();
  }

  private String buildCaseExportJson(CaseTreeNode cn) {
    StringBuilder sb = new StringBuilder("[\n");
    List<TaskTreeNode> tasks = cn.getTasks();
    for (int i = 0; i < tasks.size(); i++) {
      sb.append(buildTaskJsonObject(tasks.get(i)));
      if (i < tasks.size() - 1) sb.append(",");
      sb.append("\n");
    }
    sb.append("]");
    return sb.toString();
  }

  private String buildTaskJsonObject(TaskTreeNode taskNode) {
    ChatHistoryEntry entry = taskNode.getEntry();
    List<MessageViewModel> messages = parseMessages(entry);
    StringBuilder sb = new StringBuilder("  {\n");
    sb.append("    \"taskUuid\": ").append(jsonStr(entry.getTaskUuid())).append(",\n");
    sb.append("    \"caseUuid\": ").append(jsonStr(entry.getCaseUuid())).append(",\n");
    sb.append("    \"model\": ").append(jsonStr(taskNode.getModelName())).append(",\n");
    sb.append("    \"inputTokens\": ").append(ChatHistoryJsonParser.getInputTokens(entry)).append(",\n");
    sb.append("    \"outputTokens\": ").append(ChatHistoryJsonParser.getOutputTokens(entry)).append(",\n");
    sb.append("    \"totalTokens\": ").append(taskNode.getTotalTokens()).append(",\n");
    sb.append("    \"messages\": [\n");
    for (int i = 0; i < messages.size(); i++) {
      MessageViewModel msg = messages.get(i);
      sb.append("      { \"role\": ").append(jsonStr(msg.getRole()))
        .append(", \"content\": ").append(jsonStr(msg.getText())).append(" }");
      if (i < messages.size() - 1) sb.append(",");
      sb.append("\n");
    }
    sb.append("    ]\n");
    sb.append("  }");
    return sb.toString();
  }

  private static String jsonStr(String value) {
    if (value == null) return "null";
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
        .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
  }

  // ── Getters ───────────────────────────────────────────────────────────────

  public CaseTreeNode getCaseNode() { return caseNode; }
  public CaseDetails getCaseDetails() { return caseDetails; }
  public String getSelectedTaskUuid() { return selectedTaskUuid; }
  public void setSelectedTaskUuid(String v) { this.selectedTaskUuid = v; }

  // ── Inner classes ─────────────────────────────────────────────────────────

  public static class CaseDetails {
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("MMM dd, yyyy");

    private final long id;
    private final String state;
    private final String category;
    private final String creator;
    private final String created;
    private final String finished;
    private final String processingTime;
    private final String description;

    private CaseDetails(long id, String state, String category,
                        String creator, String created, String finished,
                        String processingTime, String description) {
      this.id = id;
      this.state = state;
      this.category = category;
      this.creator = creator;
      this.created = created;
      this.finished = finished;
      this.processingTime = processingTime;
      this.description = description;
    }

    public static CaseDetails from(ICase c) {
      String state = c.getBusinessState() != null ? c.getBusinessState().name() : "—";

      String category = "N/A";
      try {
        if (c.getCategory() != null && c.getCategory().getName() != null) {
          category = c.getCategory().getName();
        }
      } catch (Exception ignored) { /* category unavailable */ }

      String creator = "—";
      try {
        if (c.getCreatorUserName() != null) creator = c.getCreatorUserName();
      } catch (Exception ignored) { /* creator unavailable */ }

      String created = "—";
      String finished = "—";
      String processingTime = "In progress";
      try {
        Date start = c.getStartTimestamp();
        Date end = c.getEndTimestamp();
        if (start != null) created = DATE_FMT.format(start);
        if (end != null) {
          finished = DATE_FMT.format(end);
          long diffMin = (end.getTime() - (start != null ? start.getTime() : end.getTime())) / 60000;
          if (diffMin < 60) {
            processingTime = diffMin + " min";
          } else {
            long h = diffMin / 60;
            long m = diffMin % 60;
            processingTime = h + " hour" + (h > 1 ? "s" : "") + (m > 0 ? " " + m + " min" : "");
          }
        }
      } catch (Exception ignored) { /* timestamps unavailable */ }

      String description = "No description";
      try {
        String desc = c.getDescription();
        if (desc != null && !desc.isBlank()) description = desc;
      } catch (Exception ignored) { /* description unavailable */ }

      return new CaseDetails(c.getId(), state, category, creator,
          created, finished, processingTime, description);
    }

    public long getId() { return id; }
    public String getState() { return state; }
    public String getCategory() { return category; }
    public String getCreator() { return creator; }
    public String getCreated() { return created; }
    public String getFinished() { return finished; }
    public String getProcessingTime() { return processingTime; }
    public String getDescription() { return description; }
  }

  public static class MessageViewModel {
    private final String role;
    private final String text;

    public MessageViewModel(String role, String text) {
      this.role = role;
      this.text = text != null ? text : "";
    }

    public String getRole() { return role; }
    public String getText() { return text; }
  }
}
