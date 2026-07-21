package com.axonivy.utils.smart.workflow.governance.ui.bean;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.analyze.service.CaseAnalysisService;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.entity.CaseStatistics;
import com.axonivy.utils.smart.workflow.governance.history.analytic.report.statistic.service.CaseStatisticsService;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.entity.AiGovernanceReport;
import com.axonivy.utils.smart.workflow.governance.history.entity.AiGovernanceReportEntry;
import com.axonivy.utils.smart.workflow.governance.history.internal.CaseService;
import com.axonivy.utils.smart.workflow.governance.history.internal.ChatHistoryJsonParser;
import com.axonivy.utils.smart.workflow.governance.history.storage.AiGovernanceReportStorage;
import com.axonivy.utils.smart.workflow.governance.history.storage.HistoryStorage;
import com.axonivy.utils.smart.workflow.governance.history.storage.internal.IvyRepoAiReportStorage;
import com.axonivy.utils.smart.workflow.governance.history.storage.internal.IvyRepoHistoryStorage;
import com.axonivy.utils.smart.workflow.governance.ui.model.CaseTreeNode;
import com.axonivy.utils.smart.workflow.governance.utils.MessageViewModelParser;
import com.axonivy.utils.smart.workflow.governance.utils.MessageViewModelParser.MessageViewModel;
import com.axonivy.utils.smart.workflow.governance.utils.MessageViewModelParser.Role;
import com.axonivy.utils.smart.workflow.governance.utils.TimeCalculationUtils;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.viewer.api.ProcessViewer;
import ch.ivyteam.ivy.workflow.ICase;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named("conversationsBean")
@ViewScoped
public class ConversationsBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final String PROCESS_VIEWER_URL_ERROR = "Cannot get process viewer URL for case {0}";
  private static final String PROCESSING_TIME_ERROR = "Failed to compute processing time for case {0}: {1}";

  private MessageViewModelParser messageParser;

  private CaseTreeNode caseNode;
  private List<AgentConversationEntry> entries;
  private ICase ivyCase;
  private CaseStatistics caseStatistics;
  private AiGovernanceReport aiGovernanceReport;
  private String aiReportGeneratedAt;
  private boolean aiAnalyzing;

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
      if (caseNode != null && entries != null && !entries.isEmpty()) {
        caseStatistics = CaseStatisticsService.compute(
            caseNode.getCaseUuid(), ivyCase != null ? ivyCase.getName() : null,
            CaseAnalysisService.buildSummaries(entries));
      }
      AiGovernanceReportStorage reportStorage = new IvyRepoAiReportStorage();
      reportStorage.findByCaseUuid(caseUuid).ifPresent(e -> {
        aiGovernanceReport = e.getReport();
        aiReportGeneratedAt = e.getGeneratedAt();
      });
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

  public void generateAiRecommendation() {
    if (caseNode == null) {
      return;
    }
    aiAnalyzing = true;
    try {
      var result = CaseAnalysisService.analyze(caseNode.getCaseUuid(), ivyCase, entries);
      aiGovernanceReport = result.aiReport();
      if (aiGovernanceReport != null) {
        var entry = new AiGovernanceReportEntry();
        entry.setCaseUuid(caseNode.getCaseUuid());
        entry.setGeneratedAt(LocalDateTime.now().toString());
        entry.setReport(aiGovernanceReport);
        AiGovernanceReportStorage reportStorage = new IvyRepoAiReportStorage();
        reportStorage.findByCaseUuid(caseNode.getCaseUuid()).ifPresent(reportStorage::delete);
        reportStorage.save(entry);
        aiReportGeneratedAt = entry.getGeneratedAt();
      }
    } catch (Exception e) {
      Ivy.log().error("Failed to generate AI recommendation for case {0}: {1}", caseNode.getCaseUuid(), e.getMessage());
      aiGovernanceReport = null;
    } finally {
      aiAnalyzing = false;
    }
  }

  public CaseStatistics getCaseStatistics() {
    return caseStatistics;
  }

  public boolean isReportGenerated() {
    return caseStatistics != null;
  }

  public List<String[]> getAgentInfoRows(CaseStatistics.AgentStats as) {
    var s = as.getSummary();
    var rows = new ArrayList<String[]>();
    if (s.getAgentName() != null && !s.getAgentName().isBlank()) {
      rows.add(new String[]{"ID", s.getAgentId()});
    }
    rows.add(new String[]{"Model",         s.getModel() != null ? s.getModel() : "N/A"});
    rows.add(new String[]{"Finish reason", s.getFinishReason() != null ? s.getFinishReason() : "N/A"});
    return rows;
  }

  public List<String[]> getAgentTokenRows(CaseStatistics.AgentStats as) {
    var s = as.getSummary();
    var rows = new ArrayList<String[]>();
    rows.add(new String[]{"Tokens",        fmtLong(s.getTotalTokens())});
    if (as.getTokensPerMsg() > 0) {
      rows.add(new String[]{"Tokens/message", fmtDec(as.getTokensPerMsg())});
    }
    if (as.getThroughputTokensPerSec() > 0) {
      rows.add(new String[]{"Throughput", String.format("%.1f tokens/sec", as.getThroughputTokensPerSec())});
    }
    if (isReportGenerated() && caseStatistics.getTotalTokens() > 0) {
      rows.add(new String[]{"Token share", as.getTokenSharePct() + "% of case total"});
    }
    return rows;
  }

  public List<String[]> getAgentActivityRows(CaseStatistics.AgentStats as) {
    var s = as.getSummary();
    var rows = new ArrayList<String[]>();
    rows.add(new String[]{"Messages",   String.valueOf(s.getMessageCount())});
    rows.add(new String[]{"Tool calls", String.valueOf(s.getToolCallCount())});
    rows.add(new String[]{"Duration",   s.getDurationMs() + " ms"});
    if (isReportGenerated() && caseStatistics.getTotalDurationMs() > 0) {
      rows.add(new String[]{"Time share", as.getTimeSharePct() + "% of case total"});
    }
    return rows;
  }

  public List<String[]> getOrderedRiskItems(AiGovernanceReport.RiskAssessment ra) {
    if (ra == null) return List.of();
    var base  = "/Dialogs/com/axonivy/utils/ai/Conversations/Conversations/";
    var items = new ArrayList<String[]>();
    addRisk(items, Ivy.cms().co(base + "AiRiskOperational"), ra.getOperational());
    addRisk(items, Ivy.cms().co(base + "AiRiskCompliance"),  ra.getCompliance());
    addRisk(items, Ivy.cms().co(base + "AiRiskCost"),        ra.getCost());
    addRisk(items, Ivy.cms().co(base + "AiRiskReliability"), ra.getReliability());
    items.sort((a, b) -> riskLevelOrder(a[1]) - riskLevelOrder(b[1]));
    return items;
  }

  private static void addRisk(List<String[]> list, String label, AiGovernanceReport.RiskEntry entry) {
    if (entry != null) {
      list.add(new String[]{label,
          entry.getLevel()  != null ? entry.getLevel()  : "",
          entry.getDetail() != null ? entry.getDetail() : ""});
    }
  }

  private static int riskLevelOrder(String level) {
    if ("High".equalsIgnoreCase(level))     return 0;
    if ("Moderate".equalsIgnoreCase(level)) return 1;
    return 2;
  }

  private static String fmtLong(long n) {
    return String.format("%,d", n);
  }

  private static String fmtDec(double n) {
    return String.format("%.0f", n);
  }

  public AiGovernanceReport getAiGovernanceReport() {
    return aiGovernanceReport;
  }

  public String getAiReportGeneratedAt() {
    return aiReportGeneratedAt;
  }

  public boolean isAiAnalyzing() {
    return aiAnalyzing;
  }

  public boolean isInternalSmartWorkflow() {
    if (ivyCase == null) {
      return false;
    }
    return ivyCase.customFields().stringField("internalSmartWorkflow").get()
        .filter("true"::equals)
        .isPresent();
  }
}
