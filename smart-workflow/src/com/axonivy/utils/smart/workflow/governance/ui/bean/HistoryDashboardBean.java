package com.axonivy.utils.smart.workflow.governance.ui.bean;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.axonivy.utils.smart.workflow.governance.ui.entity.CaseHistoryGroup;
import com.axonivy.utils.smart.workflow.governance.ui.entity.TaskHistoryGroup;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.internal.AgentHistoryTreeBuilder;
import com.axonivy.utils.smart.workflow.governance.history.internal.AgentHistoryTreeBuilder.AgentNode;
import com.axonivy.utils.smart.workflow.governance.history.storage.HistoryStorage;
import com.axonivy.utils.smart.workflow.governance.history.storage.internal.IvyRepoHistoryStorage;
import com.axonivy.utils.smart.workflow.governance.service.internal.CaseService;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import com.axonivy.utils.smart.workflow.model.ChatModelFactory;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;

@ManagedBean
@ViewScoped
public class HistoryDashboardBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private final HistoryStorage storage = new IvyRepoHistoryStorage();

  private String filterCase = "";
  private String filterModel = "";
  private String filterDateRange = "LAST_30_DAYS";

  private List<AgentConversationEntry> entries = List.of();
  private TreeNode<Object> historyTree;
  private AgentConversationEntry selectedEntry;

  @PostConstruct
  public void init() {
    applyFilter();
  }

  public void applyFilter() {
    entries = storage.findAll().stream()
        .filter(this::matchesCaseFilter)
        .filter(this::matchesModelFilter)
        .filter(this::matchesDateRangeFilter)
        .toList();
    buildTree();
  }

  private boolean matchesCaseFilter(AgentConversationEntry e) {
    if (filterCase == null || filterCase.isBlank()) return true;
    return CaseService.matchesSearch(e.getCaseUuid(), filterCase.trim());
  }

  private boolean matchesModelFilter(AgentConversationEntry e) {
    if (filterModel == null || filterModel.isBlank()) return true;
    String storedModel = e.getModelName();
    return storedModel != null && (storedModel.contains(filterModel) || filterModel.contains(storedModel));
  }

  private boolean matchesDateRangeFilter(AgentConversationEntry e) {
    if ("ALL".equals(filterDateRange)) return true;
    if (e.getLastUpdated() == null) return false;
    LocalDateTime updated;
    try {
      updated = LocalDateTime.parse(e.getLastUpdated());
    } catch (Exception ex) {
      return false;
    }
    LocalDateTime now = LocalDateTime.now();
    switch (filterDateRange) {
      case "TODAY":       return !updated.toLocalDate().isBefore(now.toLocalDate());
      case "LAST_7_DAYS": return updated.isAfter(now.minusDays(7));
      case "LAST_30_DAYS": return updated.isAfter(now.minusDays(30));
      default: return true;
    }
  }

  private void buildTree() {
    historyTree = new DefaultTreeNode<>("root", null, null);
    AgentHistoryTreeBuilder.buildTree(entries).forEach(caseNode -> {
      List<AgentConversationEntry> caseEntries = caseNode.tasks().stream()
          .flatMap(t -> t.agents().stream())
          .map(AgentNode::chat)
          .toList();
      CaseHistoryGroup caseGroup = new CaseHistoryGroup(caseNode.caseUuid(), caseEntries);
      TreeNode<Object> caseTreeNode = new DefaultTreeNode<>("case", caseGroup, historyTree);
      caseTreeNode.setExpanded(false);

      caseNode.tasks().forEach(taskNode -> {
        List<AgentConversationEntry> taskEntries = taskNode.agents().stream()
            .map(AgentNode::chat).toList();
        TaskHistoryGroup taskGroup = new TaskHistoryGroup(taskNode.taskUuid(), taskEntries);
        TreeNode<Object> taskTreeNode = new DefaultTreeNode<>("task", taskGroup, caseTreeNode);
        taskTreeNode.setExpanded(false);

        taskNode.agents().forEach(agentNode ->
            new DefaultTreeNode<>("agent", agentNode.chat(), taskTreeNode));
      });
    });
  }

  public int getEntryCount() {
    return entries.size();
  }

  public int getCaseCount() {
    return historyTree == null ? 0 : historyTree.getChildCount();
  }

  // Getters and setters

  public List<SelectItem> getAvailableModelItems() {
    return ChatModelFactory.providers().stream()
        .collect(java.util.stream.Collectors.toMap(
            ChatModelProvider::name, p -> p, (a, b) -> a))
        .values().stream()
        .filter(provider -> !provider.models().isEmpty())
        .sorted(Comparator.comparing(ChatModelProvider::name))
        .map(provider -> {
          SelectItemGroup group = new SelectItemGroup(provider.name());
          group.setSelectItems(provider.models().stream()
              .map(m -> new SelectItem(m, m))
              .toArray(SelectItem[]::new));
          return (SelectItem) group;
        })
        .toList();
  }

  public String getFilterCase() { return filterCase; }
  public void setFilterCase(String v) { this.filterCase = v; }

  public String getFilterModel() { return filterModel; }
  public void setFilterModel(String v) { this.filterModel = v; }

  public String getFilterDateRange() { return filterDateRange; }
  public void setFilterDateRange(String v) { this.filterDateRange = v; }

  public List<AgentConversationEntry> getEntries() { return entries; }

  public TreeNode<Object> getHistoryTree() { return historyTree; }

  public AgentConversationEntry getSelectedEntry() { return selectedEntry; }
  public void setSelectedEntry(AgentConversationEntry v) { this.selectedEntry = v; }

}
