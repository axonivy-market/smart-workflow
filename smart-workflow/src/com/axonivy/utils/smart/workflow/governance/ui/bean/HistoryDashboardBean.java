package com.axonivy.utils.smart.workflow.governance.ui.bean;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.TreeNode;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.storage.HistoryStorage;
import com.axonivy.utils.smart.workflow.governance.history.storage.internal.IvyRepoHistoryStorage;
import com.axonivy.utils.smart.workflow.governance.ui.entity.CaseHistoryGroup;
import com.axonivy.utils.smart.workflow.governance.ui.entity.TaskHistoryGroup;
import com.axonivy.utils.smart.workflow.governance.ui.enums.DateRangeFilter;
import com.axonivy.utils.smart.workflow.governance.ui.enums.HistoryNodeType;
import com.axonivy.utils.smart.workflow.governance.ui.model.AgentTreeNode;
import com.axonivy.utils.smart.workflow.governance.ui.model.CaseTreeNode;
import com.axonivy.utils.smart.workflow.governance.ui.model.TaskTreeNode;
import com.axonivy.utils.smart.workflow.governance.utils.ChatHistoryJsonParser;

@ManagedBean
@ViewScoped
public class HistoryDashboardBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private HistoryStorage storage;

  private String filterCase = "";
  private String filterTaskUuid = "";
  private String filterModel = "";
  private DateRangeFilter filterDateRange = DateRangeFilter.LAST_30_DAYS;

  private List<AgentConversationEntry> entries = List.of();
  private TreeNode<Object> historyTree;
  private AgentConversationEntry selectedEntry;

  @PostConstruct
  public void init() {
    storage = new IvyRepoHistoryStorage();
    applyFilter();
  }

  public void applyFilter() {
    List<AgentConversationEntry> all = storage.findAll();
    entries = applyFilters(all);
    buildTree();
  }

  private List<AgentConversationEntry> applyFilters(List<AgentConversationEntry> all) {
    LocalDate dateFrom = filterDateRange.toDateFrom();
    return all.stream()
        .filter(entry -> filterCase.isBlank() || entry.getCaseUuid().contains(filterCase))
        .filter(entry -> filterTaskUuid.isBlank() || entry.getTaskUuid().contains(filterTaskUuid))
        .filter(entry -> filterModel.isBlank() || filterModel.equals(ChatHistoryJsonParser.getModelName(entry)))
        .filter(entry -> dateFrom == null || isOnOrAfter(entry.getLastUpdated(), dateFrom))
        .toList();
  }

  private boolean isOnOrAfter(String lastUpdated, LocalDate dateFrom) {
    if (lastUpdated == null) {
      return false;
    }
    try {
      return !LocalDateTime.parse(lastUpdated).toLocalDate().isBefore(dateFrom);
    } catch (Exception e) {
      return true;
    }
  }

  private void buildTree() {
    historyTree = HistoryNodeType.ROOT.createNode(null, null);
    CaseTreeNode.buildTree(entries).forEach(caseNode -> buildCaseNode(caseNode, historyTree));
  }

  private void buildCaseNode(CaseTreeNode caseNode, TreeNode<Object> parent) {
    List<AgentConversationEntry> caseEntries = caseNode.getTasks().stream()
        .flatMap(task -> task.getAgents().stream())
        .map(AgentTreeNode::getEntry)
        .toList();
    CaseHistoryGroup caseGroup = new CaseHistoryGroup(caseNode.getCaseUuid(), caseEntries);
    TreeNode<Object> caseTreeNode = HistoryNodeType.CASE.createNode(caseGroup, parent);
    caseNode.getTasks().forEach(taskNode -> buildTaskNode(taskNode, caseTreeNode));
  }

  private void buildTaskNode(TaskTreeNode taskNode, TreeNode<Object> parent) {
    List<AgentConversationEntry> taskEntries = taskNode.getAgents().stream()
        .map(AgentTreeNode::getEntry)
        .toList();
    TaskHistoryGroup taskGroup = new TaskHistoryGroup(taskNode.getTaskUuid(), taskEntries);
    TreeNode<Object> taskTreeNode = HistoryNodeType.TASK.createNode(taskGroup, parent);
    List<AgentTreeNode> agentNodes = taskNode.getAgents();
    IntStream.range(0, agentNodes.size())
        .forEach(i -> buildAgentNode(agentNodes.get(i), i + 1, taskTreeNode));
  }

  private void buildAgentNode(AgentTreeNode agentNode, int sequence, TreeNode<Object> parent) {
    AgentConversationEntry conversation = agentNode.getEntry();
    conversation.setSequenceInTask(sequence);
    HistoryNodeType.AGENT.createNode(conversation, parent);
  }

  public int getEntryCount() {
    return entries.size();
  }

  public int getCaseCount() {
    return historyTree == null ? 0 : historyTree.getChildCount();
  }

  public String getFilterCase() {
    return filterCase;
  }

  public void setFilterCase(String filterCase) {
    this.filterCase = filterCase;
  }

  public String getFilterTaskUuid() {
    return filterTaskUuid;
  }

  public void setFilterTaskUuid(String filterTaskUuid) {
    this.filterTaskUuid = filterTaskUuid;
  }

  public String getFilterModel() {
    return filterModel;
  }

  public void setFilterModel(String filterModel) {
    this.filterModel = filterModel;
  }

  public DateRangeFilter getFilterDateRange() {
    return filterDateRange;
  }

  public void setFilterDateRange(DateRangeFilter filterDateRange) {
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
