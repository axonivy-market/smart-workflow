package com.axonivy.utils.smart.workflow.governance.ui.bean;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import com.axonivy.utils.smart.workflow.governance.history.ChatHistoryJsonParser;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.service.CaseService;
import com.axonivy.utils.smart.workflow.governance.history.storage.HistoryStorage;
import com.axonivy.utils.smart.workflow.governance.history.storage.IvyRepoHistoryStorage;
import com.axonivy.utils.smart.workflow.governance.ui.model.CaseTreeNode;
import com.axonivy.utils.smart.workflow.governance.utils.MessageViewModelParser;
import com.axonivy.utils.smart.workflow.governance.utils.MessageViewModelParser.MessageViewModel;
import com.axonivy.utils.smart.workflow.governance.utils.MessageViewModelParser.Role;
import com.axonivy.utils.smart.workflow.governance.utils.TimeCalculationUtils;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.viewer.api.ProcessViewer;
import ch.ivyteam.ivy.workflow.ICase;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named("conversationsBean")
@ViewScoped
public class ConversationsBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final String PROCESS_VIEWER_URL_ERROR = "Cannot get process viewer URL for case {0}";
  private static final String PROCESSING_TIME_ERROR = "Failed to compute processing time for case {0}: {1}";

  private final MessageViewModelParser messageParser = new MessageViewModelParser();
  private final HistoryStorage historyStorage = new IvyRepoHistoryStorage();

  private CaseTreeNode caseNode;
  private ICase selectedCase;
  private String processViewerUrl;

  public void preRender(String caseUuid) {
    if (caseUuid == null || caseNode != null) {
      return;
    }
    List<AgentConversationEntry> entries = historyStorage.findByCaseUuid(caseUuid);
    caseNode = CaseTreeNode.buildTree(entries).stream().findFirst().orElse(null);
    selectedCase = CaseService.findCase(caseUuid);
    processViewerUrl = computeProcessViewerUrl();
  }

  public String getCaseState() {
    return Optional.ofNullable(selectedCase)
        .map(ICase::getBusinessState)
        .map(Enum::name)
        .orElse("");
  }

  public String getProcessingTime() {
    if (selectedCase == null) {
      return "";
    }
    try {
      return TimeCalculationUtils.formatProcessingTime(selectedCase.getStartTimestamp(), selectedCase.getEndTimestamp());
    } catch (RuntimeException e) {
      Ivy.log().warn(PROCESSING_TIME_ERROR, e, selectedCase.getId());
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
    return selectedCase;
  }

  public String getProcessViewerUrl() {
    return processViewerUrl;
  }

  private String computeProcessViewerUrl() {
    if (selectedCase == null) {
      return null;
    }
    try {
      ProcessViewer viewer = ProcessViewer.of(selectedCase);
      if (!viewer.isViewAllowed()) {
        return null;
      }
      return viewer.url().toWebLink().getRelative();
    } catch (RuntimeException e) {
      Ivy.log().warn(PROCESS_VIEWER_URL_ERROR, e, selectedCase.getId());
      return null;
    }
  }

  public CaseTreeNode getCaseNode() {
    return caseNode;
  }
}
