package com.axonivy.utils.smart.workflow.governance.ui.bean;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import ch.ivyteam.ivy.environment.Ivy;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.internal.AgentHistoryTreeBuilder;
import com.axonivy.utils.smart.workflow.governance.history.internal.AgentHistoryTreeBuilder.AgentNode;
import com.axonivy.utils.smart.workflow.governance.history.storage.HistoryStorage;
import com.axonivy.utils.smart.workflow.governance.history.storage.internal.IvyRepoHistoryStorage;
import com.axonivy.utils.smart.workflow.governance.service.CaseService;
import com.axonivy.utils.smart.workflow.governance.ui.entity.CaseHistoryGroup;
import com.axonivy.utils.smart.workflow.governance.ui.entity.DateRange;
import com.axonivy.utils.smart.workflow.governance.ui.entity.HistoryNodeType;
import com.axonivy.utils.smart.workflow.governance.ui.entity.TaskHistoryGroup;
import com.axonivy.utils.smart.workflow.model.ChatModelFactory;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;

@ManagedBean
@ViewScoped
public class HistoryDashboardBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private HistoryStorage storage;

  private String filterCase = "";
  private String filterModel = "";
  private String filterDateRange = DateRange.LAST_30_DAYS.name();

  private List<AgentConversationEntry> entries = List.of();
  private TreeNode<Object> historyTree;
  private AgentConversationEntry selectedEntry;

  @PostConstruct
  public void init() {
    storage = new IvyRepoHistoryStorage();
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

  private boolean matchesCaseFilter(AgentConversationEntry entry) {
    if (filterCase == null || filterCase.isBlank()) {
      return true;
    }
    return CaseService.matchesSearch(entry.getCaseUuid(), filterCase.trim());
  }

  private boolean matchesModelFilter(AgentConversationEntry entry) {
    if (filterModel == null || filterModel.isBlank()) {
      return true;
    }
    String storedModel = entry.getModelName();
    return storedModel != null && (storedModel.contains(filterModel) || filterModel.contains(storedModel));
  }

  private boolean matchesDateRangeFilter(AgentConversationEntry entry) {
    if (DateRange.ALL.name().equals(filterDateRange)) {
      return true;
    }
    if (entry.getLastUpdated() == null) {
      return false;
    }
    LocalDateTime updated;
    try {
      updated = LocalDateTime.parse(entry.getLastUpdated());
    } catch (DateTimeParseException ex) {
      Ivy.log().warn("Failed to parse lastUpdated: {0}", ex.getMessage());
      return false;
    }
    LocalDateTime now = LocalDateTime.now();
    return switch (DateRange.valueOf(filterDateRange)) {
      case TODAY -> !updated.toLocalDate().isBefore(now.toLocalDate());
      case LAST_7_DAYS -> updated.isAfter(now.minusDays(7));
      case LAST_30_DAYS -> updated.isAfter(now.minusDays(30));
      default -> true;
    };
  }

  private void buildTree() {
    historyTree = new DefaultTreeNode<>(HistoryNodeType.ROOT.value(), null, null);
    AgentHistoryTreeBuilder.buildTree(entries).forEach(caseNode -> {
      List<AgentConversationEntry> caseEntries = caseNode.tasks().stream()
          .flatMap(taskNode -> taskNode.agents().stream())
          .map(AgentNode::chat)
          .toList();
      CaseHistoryGroup caseGroup = new CaseHistoryGroup(caseNode.caseUuid(), caseEntries);
      TreeNode<Object> caseTreeNode = new DefaultTreeNode<>(HistoryNodeType.CASE.value(), caseGroup, historyTree);
      caseTreeNode.setExpanded(false);

      caseNode.tasks().forEach(taskNode -> {
        List<AgentConversationEntry> taskEntries = taskNode.agents().stream()
            .map(AgentNode::chat).toList();
        TaskHistoryGroup taskGroup = new TaskHistoryGroup(taskNode.taskUuid(), taskEntries);
        TreeNode<Object> taskTreeNode = new DefaultTreeNode<>(HistoryNodeType.TASK.value(), taskGroup, caseTreeNode);
        taskTreeNode.setExpanded(false);

        taskNode.agents().forEach(agentNode ->
            new DefaultTreeNode<>(HistoryNodeType.AGENT.value(), agentNode.chat(), taskTreeNode));
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
        .collect(Collectors.toCollection(
            () -> new TreeSet<>(Comparator.comparing(ChatModelProvider::name))))
        .stream()
        .filter(provider -> !provider.models().isEmpty())
        .map(provider -> {
          SelectItemGroup group = new SelectItemGroup(provider.name());
          group.setSelectItems(provider.models().stream()
              .map(model -> new SelectItem(model, model))
              .toArray(SelectItem[]::new));
          return (SelectItem) group;
        })
        .toList();
  }

  public String getFilterCase() {
    return filterCase;
  }

  public void setFilterCase(String filterCase) {
    this.filterCase = filterCase;
  }

  public String getFilterModel() {
    return filterModel;
  }

  public void setFilterModel(String filterModel) {
    this.filterModel = filterModel;
  }

  public String getFilterDateRange() {
    return filterDateRange;
  }

  public void setFilterDateRange(String filterDateRange) {
    this.filterDateRange = filterDateRange;
  }

  public List<AgentConversationEntry> getEntries() {
    return entries;
  }

  public TreeNode<Object> getHistoryTree() {
    return historyTree;
  }

  public AgentConversationEntry getSelectedEntry() {
    return selectedEntry;
  }

  public void setSelectedEntry(AgentConversationEntry selectedEntry) {
    this.selectedEntry = selectedEntry;
  }

}
