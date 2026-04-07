package com.axonivy.utils.smart.workflow.governance.ui.bean;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.storage.HistoryStorage;
import com.axonivy.utils.smart.workflow.governance.history.storage.internal.IvyRepoHistoryStorage;
import com.axonivy.utils.smart.workflow.governance.service.CaseService;
import com.axonivy.utils.smart.workflow.governance.ui.model.CaseTreeNode;
import com.axonivy.utils.smart.workflow.governance.ui.model.TaskTreeNode;
import com.axonivy.utils.smart.workflow.governance.utils.ChatHistoryJsonParser;
import com.axonivy.utils.smart.workflow.governance.utils.MessageViewModelParser;
import com.axonivy.utils.smart.workflow.governance.utils.MessageViewModelParser.MessageViewModel;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.viewer.api.ProcessViewer;
import ch.ivyteam.ivy.workflow.ICase;

@ManagedBean
@ViewScoped
public class ConversationsBean implements Serializable {

  private static final long   serialVersionUID = 1L;
  private static final long   MS_PER_MIN        = TimeUnit.MINUTES.toMillis(1);
  private static final String IN_PROGRESS       = "In progress"; // TODO create CMS
  private static final String MINUTES_FORMAT       = "%d min";
  private static final String HOURS_FORMAT         = "%d hour%s";
  private static final String HOURS_MINUTES_FORMAT = "%d hour%s %d min";

  private HistoryStorage storage;
  private final MessageViewModelParser messageParser = new MessageViewModelParser();

  private CaseTreeNode caseNode;
  private ICase ivyCase;
  private String selectedTaskUuid;

  public void preRender(String caseUuid) {
    storage = new IvyRepoHistoryStorage();
    if (caseUuid != null && caseNode == null) {
      loadForCase(caseUuid);
    }
  }

  private void loadForCase(String caseUuid) {
    List<AgentConversationEntry> entries = storage.findByCaseUuid(caseUuid);
    List<CaseTreeNode> tree = CaseTreeNode.buildTree(entries);
    caseNode = tree.isEmpty() ? null : tree.get(0);
    if (caseNode != null && !caseNode.getTasks().isEmpty()) {
      selectedTaskUuid = caseNode.getTasks().get(0).getTaskUuid();
    }
    ivyCase = CaseService.findCase(caseUuid);
  }

  public void selectTask(String taskUuid) {
    this.selectedTaskUuid = taskUuid;
  }

  public TaskTreeNode getSelectedTaskNode() {
    if (caseNode == null || selectedTaskUuid == null) {
      return null;
    }
    return caseNode.getTasks().stream()
        .filter(task -> selectedTaskUuid.equals(task.getTaskUuid()))
        .findFirst()
        .orElse(null);
  }

  public String getCaseState() {
    if (ivyCase == null) {
      return "";
    }
    return Optional.ofNullable(ivyCase.getBusinessState())
        .map(Enum::name)
        .orElse("");
  }

  public String getProcessingTime() {
    if (ivyCase == null) {
      return "";
    }
    try {
      Date end = ivyCase.getEndTimestamp();
      if (end == null) {
        return IN_PROGRESS;
      }
      Date start = ivyCase.getStartTimestamp();
      long durationInMinutes = (end.getTime() - (start.getTime())) / MS_PER_MIN;
      return formatDuration(durationInMinutes);
    } catch (RuntimeException e) {
      Ivy.log().warn("Failed to compute processing time for case {0}: {1}",
          ivyCase.getId(), e.getMessage());
      return "";
    }
  }

  private String formatDuration(long minutes) {
    if (minutes < 60) {
      return String.format(MINUTES_FORMAT, minutes);
    }
    long hours = minutes / 60;
    long remainingMinutes = minutes % 60;
    String plural = hours > 1 ? "s" : "";
    return remainingMinutes > 0 ? String.format(HOURS_MINUTES_FORMAT, hours, plural, remainingMinutes)
                                : String.format(HOURS_FORMAT, hours, plural);
  }

  public List<MessageViewModel> parseMessages(AgentConversationEntry entry) {
    return messageParser.parse(entry);
  }

  public List<MessageViewModel> getSystemMessages(AgentConversationEntry entry) {
    return messageParser.getSystemMessages(entry);
  }

  public List<MessageViewModel> getUserMessages(AgentConversationEntry entry) {
    return messageParser.getUserMessages(entry);
  }

  public List<MessageViewModel> getAssistantMessages(AgentConversationEntry entry) {
    return messageParser.getAssistantMessages(entry);
  }

  public long getCaseAvgDurationMs() {
    if (caseNode == null) {
      return 0L;
    }
    return (long) caseNode.getTasks().stream()
        .mapToLong(task -> ChatHistoryJsonParser.getAvgDurationMs(task.getEntry()))
        .filter(duration -> duration > 0)
        .average()
        .orElse(0);
  }

  public long getInputTokens(AgentConversationEntry entry) {
    return ChatHistoryJsonParser.getInputTokens(entry);
  }

  public long getOutputTokens(AgentConversationEntry entry) {
    return ChatHistoryJsonParser.getOutputTokens(entry);
  }

  public ICase getIvyCase() {
    return ivyCase;
  }

  public String getProcessViewerUrl() {
    if (ivyCase == null) {
      return null;
    }
    try {
      if (!ProcessViewer.of(ivyCase).isViewAllowed()) {
        return null;
      }
      return ProcessViewer.of(ivyCase).url().toWebLink().getRelative();
    } catch (Exception e) {
      Ivy.log().warn("Cannot get process viewer URL for case {0}", e,
          caseNode.getCaseUuid());
      return null;
    }
  }

  public CaseTreeNode getCaseNode() {
    return caseNode;
  }

  public String getSelectedTaskUuid() {
    return selectedTaskUuid;
  }

  public void setSelectedTaskUuid(String selectedTaskUuid) {
    this.selectedTaskUuid = selectedTaskUuid;
  }
}
