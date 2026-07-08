package com.axonivy.utils.smart.workflow.governance.ui.bean;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.internal.CaseService;
import com.axonivy.utils.smart.workflow.governance.history.internal.ChatHistoryJsonParser;
import com.axonivy.utils.smart.workflow.governance.history.storage.HistoryStorage;
import com.axonivy.utils.smart.workflow.governance.history.storage.internal.IvyRepoHistoryStorage;
import com.axonivy.utils.smart.workflow.governance.ui.model.CaseTreeNode;
import com.axonivy.utils.smart.workflow.governance.utils.MessageViewModelParser;
import com.axonivy.utils.smart.workflow.governance.utils.MessageViewModelParser.MessageViewModel;
import com.axonivy.utils.smart.workflow.governance.utils.MessageViewModelParser.Role;
import com.axonivy.utils.smart.workflow.governance.utils.TimeCalculationUtils;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.viewer.api.ProcessViewer;
import ch.ivyteam.ivy.workflow.ICase;

@ManagedBean
@ViewScoped
public class ConversationsBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final String PROCESS_VIEWER_URL_ERROR = "Cannot get process viewer URL for case {0}";
  private static final String PROCESSING_TIME_ERROR = "Failed to compute processing time for case {0}: {1}";

  private MessageViewModelParser messageParser;

  private CaseTreeNode caseNode;
  private List<AgentConversationEntry> entries;
  private ICase ivyCase;

  @PostConstruct
  public void init() {
    messageParser = new MessageViewModelParser();
  }

  public void preRender(String caseUuid) {
    if (caseUuid != null && caseNode == null) {
      HistoryStorage storage = new IvyRepoHistoryStorage();
      entries = storage.findByCaseUuid(caseUuid);
      List<CaseTreeNode> tree = CaseTreeNode.buildTree(entries);
      caseNode = tree.isEmpty() ? null : tree.get(0);
      ivyCase = CaseService.findCase(caseUuid);
    }
  }

  public String getCaseState() {
    return Optional.ofNullable(ivyCase)
        .map(ICase::getBusinessState)
        .map(Enum::name)
        .orElse("");
  }

  public String getProcessingTime() {
    if (ivyCase == null) {
      return "";
    }
    try {
      return TimeCalculationUtils.formatProcessingTime(ivyCase.getStartTimestamp(), ivyCase.getEndTimestamp());
    } catch (RuntimeException e) {
      Ivy.log().warn(PROCESSING_TIME_ERROR, ivyCase.getId(), e.getMessage());
      return "";
    }
  }

  public List<MessageViewModel> getSystemMessages(AgentConversationEntry entry) {
    return messageParser.parseByRole(entry).getOrDefault(Role.SYSTEM, List.of());
  }

  public List<MessageViewModel> getUserMessages(AgentConversationEntry entry) {
    return messageParser.parseByRole(entry).getOrDefault(Role.USER, List.of());
  }

  public List<MessageViewModel> getAssistantMessages(AgentConversationEntry entry) {
    return messageParser.parseByRole(entry).getOrDefault(Role.ASSISTANT, List.of());
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
      Ivy.log().warn(PROCESS_VIEWER_URL_ERROR, e, ivyCase.getId());
      return null;
    }
  }

  public CaseTreeNode getCaseNode() {
    return caseNode;
  }
}
