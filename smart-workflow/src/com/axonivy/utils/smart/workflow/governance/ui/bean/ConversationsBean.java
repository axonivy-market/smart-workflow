package com.axonivy.utils.smart.workflow.governance.ui.bean;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import ch.ivyteam.ivy.process.call.SubProcessCallStartEvent;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter.SearchScope;
import ch.ivyteam.ivy.security.exec.Sudo;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.axonivy.utils.smart.workflow.governance.history.analytic.report.CaseHistoryAnalyzer;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.storage.HistoryStorage;
import com.axonivy.utils.smart.workflow.governance.history.storage.internal.IvyRepoHistoryStorage;
import com.axonivy.utils.smart.workflow.governance.service.internal.CaseService;
import com.axonivy.utils.smart.workflow.governance.ui.model.AgentTreeNode;
import com.axonivy.utils.smart.workflow.governance.ui.model.CaseTreeNode;
import com.axonivy.utils.smart.workflow.governance.ui.model.TaskTreeNode;
import com.axonivy.utils.smart.workflow.governance.utils.ChatHistoryJsonParser;
import com.axonivy.utils.smart.workflow.governance.utils.MessageViewModelParser;
import com.axonivy.utils.smart.workflow.governance.utils.MessageViewModelParser.MessageViewModel;
import com.axonivy.utils.smart.workflow.utils.JsonUtils;

import org.primefaces.model.menu.DefaultMenuModel;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.DefaultSubMenu;
import org.primefaces.model.menu.MenuModel;

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
  private String aiReport;
  private boolean aiAnalyzing;

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
    ivyCase = CaseService.findCase(caseUuid);
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

  public MenuModel getNavMenuModel() {
    DefaultMenuModel model = new DefaultMenuModel();
    if (caseNode == null) {
      return model;
    }
    List<TaskTreeNode> tasks = caseNode.getTasks();
    for (int t = 0; t < tasks.size(); t++) {
      TaskTreeNode task = tasks.get(t);
      DefaultSubMenu submenu = DefaultSubMenu.builder()
          .label((t + 1) + ". " + task.getDisplayName())
          .expanded(true)
          .build();
      List<AgentTreeNode> agents = task.getAgents();
      for (int a = 0; a < agents.size(); a++) {
        AgentTreeNode agent = agents.get(a);
        String agentId = "cv-agent-" + t + "-" + a;
        String onclick = "CvNav.select('" + agentId + "', this); return false;";
        String label = Ivy.cms().co(
            "/Dialogs/com/axonivy/utils/ai/Conversations/Conversations/AgentLabel",
            java.util.Arrays.asList(agent.getDisplayName()));
        DefaultMenuItem item = DefaultMenuItem.builder()
            .value(label)
            .onclick(onclick)
            .build();
        submenu.getElements().add(item);
      }
      model.getElements().add(submenu);
    }
    return model;
  }

  public CaseTreeNode getCaseNode() {
    return caseNode;
  }

  public void generateAiReport() {
    if (caseNode == null) {
      return;
    }
    aiAnalyzing = true;
    try {
      String caseUuid = caseNode.getCaseUuid();
      var summaries = CaseHistoryAnalyzer.analyze(caseUuid);
      var entries = CaseHistoryAnalyzer.getEntries(caseUuid);
      String prompt = CaseHistoryAnalyzer.buildAiPrompt(caseUuid, summaries, entries);
      Map<String, Object> result = Sudo.get(() -> {
        var filter = SubProcessSearchFilter.create()
            .setSearchScope(SearchScope.SECURITY_CONTEXT)
            .setSignature("analyzeAgentHistoryByCase(String, String)")
            .toFilter();
        var startList = SubProcessCallStartEvent.find(filter);
        if (startList.isEmpty()) {
          return Map.of();
        }
        return startList.get(0).withParam("caseUuid", caseUuid).withParam("prompt", prompt).call().asMap();
      });
      aiReport = (String) result.get("aiReport");
    } catch (Exception e) {
      Ivy.log().error("Failed to generate AI report for case {0}: {1}", caseNode.getCaseUuid(), e.getMessage());
      aiReport = null;
    } finally {
      aiAnalyzing = false;
    }
  }

  public String getAiReport() {
    return aiReport;
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

  public void export() {
    FacesContext fc = FacesContext.getCurrentInstance();
    if (fc == null || caseNode == null) {
      return;
    }
    try {
      byte[] bytes = buildExportJson().getBytes(StandardCharsets.UTF_8);
      String filename = "case-" + caseNode.getCaseUuid() + ".json";
      ExternalContext ec = fc.getExternalContext();
      ec.responseReset();
      ec.setResponseContentType("application/json");
      ec.setResponseCharacterEncoding("UTF-8");
      ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
      ec.setResponseContentLength(bytes.length);
      try (OutputStream out = ec.getResponseOutputStream()) {
        out.write(bytes);
      }
      fc.responseComplete();
    } catch (Exception e) {
      Ivy.log().error("Failed to export case {0}: {1}", caseNode.getCaseUuid(), e.getMessage());
    }
  }

  private String buildExportJson() throws IOException {
    var mapper = JsonUtils.getObjectMapper();
    var root = new LinkedHashMap<String, Object>();
    root.put("exportedAt", LocalDateTime.now().toString());
    root.put("caseUuid", caseNode.getCaseUuid());
    root.put("caseName", caseNode.getDisplayName());
    root.put("processName", caseNode.getProcessName());
    root.put("totalMessages", caseNode.getTotalMessages());
    root.put("totalTokens", caseNode.getTotalTokens());
    if (ivyCase != null) {
      root.put("caseState", getCaseState());
      root.put("processingTime", getProcessingTime());
    }
    if (aiReport != null) {
      root.put("aiReport", aiReport);
    }

    var tasksList = new ArrayList<Map<String, Object>>();
    for (TaskTreeNode task : caseNode.getTasks()) {
      var taskMap = new LinkedHashMap<String, Object>();
      taskMap.put("taskUuid", task.getTaskUuid());
      taskMap.put("taskName", task.getDisplayName());
      taskMap.put("totalMessages", task.getMessageCount());
      taskMap.put("totalTokens", task.getTotalTokens());

      var agentsList = new ArrayList<Map<String, Object>>();
      for (AgentTreeNode agent : task.getAgents()) {
        var agentMap = new LinkedHashMap<String, Object>();
        agentMap.put("agentId", agent.getAgentId());
        agentMap.put("agentName", agent.getDisplayName());
        agentMap.put("modelName", agent.getModelName());
        agentMap.put("totalTokens", agent.getTotalTokens());
        agentMap.put("avgDurationMs", agent.getAvgDurationMs());
        agentMap.put("startTime", agent.getStartTime());

        AgentConversationEntry entry = agent.getEntry();
        agentMap.put("systemMessages",
            messageParser.getSystemMessages(entry).stream().map(MessageViewModel::getText).toList());
        agentMap.put("inputMessages",
            messageParser.getUserMessages(entry).stream().map(MessageViewModel::getText).toList());
        agentMap.put("responseMessages",
            messageParser.getAssistantMessages(entry).stream().map(MessageViewModel::getText).toList());

        var toolsList = new ArrayList<Map<String, Object>>();
        for (AgentTreeNode.ToolView tool : agent.getTools()) {
          var toolMap = new LinkedHashMap<String, Object>();
          toolMap.put("toolName", tool.getToolName());
          String argsJson = tool.getArguments();
          if (argsJson != null && !argsJson.isBlank()) {
            try {
              toolMap.put("arguments", mapper.readTree(argsJson));
            } catch (Exception ex) {
              toolMap.put("arguments", argsJson);
            }
          }
          toolMap.put("result", tool.getResultText());
          toolMap.put("executedAt", tool.getExecutedAt());
          toolsList.add(toolMap);
        }
        agentMap.put("toolCalls", toolsList);

        var guardrailsList = new ArrayList<Map<String, Object>>();
        for (AgentTreeNode.GuardrailView gr : agent.getGuardrails()) {
          var grMap = new LinkedHashMap<String, Object>();
          grMap.put("name", gr.getGuardrailName());
          grMap.put("type", gr.getType());
          grMap.put("passed", gr.isPassed());
          if (gr.getMessage() != null) grMap.put("message", gr.getMessage());
          if (gr.getFailureMessage() != null) grMap.put("failureMessage", gr.getFailureMessage());
          grMap.put("durationMs", gr.getDurationMs());
          grMap.put("executedAt", gr.getExecutedAt());
          guardrailsList.add(grMap);
        }
        agentMap.put("guardrails", guardrailsList);

        agentsList.add(agentMap);
      }
      taskMap.put("agents", agentsList);
      tasksList.add(taskMap);
    }
    root.put("tasks", tasksList);
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
  }
}
