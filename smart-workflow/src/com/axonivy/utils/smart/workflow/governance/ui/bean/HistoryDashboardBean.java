package com.axonivy.utils.smart.workflow.governance.ui.bean;

import java.io.Serializable;
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

@ManagedBean
@ViewScoped
public class HistoryDashboardBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private final HistoryStorage storage = new IvyRepoHistoryStorage();

  private String filterCase = "";
  private String filterTaskUuid = "";
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
    entries = storage.findAll();
    buildTree();
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

        List<AgentNode> agentNodes = taskNode.agents();
        for (int i = 0; i < agentNodes.size(); i++) {
          AgentConversationEntry chat = agentNodes.get(i).chat();
          chat.setSequenceInTask(i + 1);
          new DefaultTreeNode<>("agent", chat, taskTreeNode);
        }
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

  public String getFilterCase() { return filterCase; }
  public void setFilterCase(String v) { this.filterCase = v; }

  public String getFilterTaskUuid() { return filterTaskUuid; }
  public void setFilterTaskUuid(String v) { this.filterTaskUuid = v; }

  public String getFilterModel() { return filterModel; }
  public void setFilterModel(String v) { this.filterModel = v; }

  public String getFilterDateRange() { return filterDateRange; }
  public void setFilterDateRange(String v) { this.filterDateRange = v; }

  public List<AgentConversationEntry> getEntries() { return entries; }

  public TreeNode<Object> getHistoryTree() { return historyTree; }

  public AgentConversationEntry getSelectedEntry() { return selectedEntry; }
  public void setSelectedEntry(AgentConversationEntry v) { this.selectedEntry = v; }

}
