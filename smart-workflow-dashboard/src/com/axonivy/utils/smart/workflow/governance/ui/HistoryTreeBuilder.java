package com.axonivy.utils.smart.workflow.governance.ui;

import java.util.List;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.internal.AgentHistoryTreeBuilder;
import com.axonivy.utils.smart.workflow.governance.history.internal.AgentHistoryTreeBuilder.AgentNode;
import com.axonivy.utils.smart.workflow.governance.history.internal.AgentHistoryTreeBuilder.CaseNode;
import com.axonivy.utils.smart.workflow.governance.history.internal.AgentHistoryTreeBuilder.TaskNode;
import com.axonivy.utils.smart.workflow.governance.ui.entity.AgentConversationView;
import com.axonivy.utils.smart.workflow.governance.ui.entity.CaseHistoryGroup;
import com.axonivy.utils.smart.workflow.governance.ui.entity.TaskHistoryGroup;
import com.axonivy.utils.smart.workflow.governance.ui.enums.HistoryNodeType;

public class HistoryTreeBuilder {

  private HistoryTreeBuilder() {}

  public static TreeNode<Object> build(List<AgentConversationEntry> entries) {
    TreeNode<Object> root = new DefaultTreeNode<>(HistoryNodeType.ROOT.value(), null, null);
    AgentHistoryTreeBuilder.buildTree(entries).forEach(caseNode -> addCaseNode(caseNode, root));
    return root;
  }

  private static void addCaseNode(CaseNode caseNode, TreeNode<Object> root) {
    List<AgentConversationEntry> caseEntries = caseNode.tasks().stream()
        .flatMap(t -> t.agents().stream())
        .map(AgentNode::chat)
        .toList();
    TreeNode<Object> caseTreeNode = collapsed(
        HistoryNodeType.CASE.value(), new CaseHistoryGroup(caseNode.caseUuid(), caseEntries), root);
    caseNode.tasks().forEach(taskNode -> addTaskNode(taskNode, caseTreeNode));
  }

  private static void addTaskNode(TaskNode taskNode, TreeNode<Object> caseTreeNode) {
    List<AgentConversationEntry> taskEntries = taskNode.agents().stream()
        .map(AgentNode::chat).toList();
    TreeNode<Object> taskTreeNode = collapsed(
        HistoryNodeType.TASK.value(), new TaskHistoryGroup(taskNode.taskUuid(), taskEntries), caseTreeNode);
    taskNode.agents().forEach(agentNode -> addAgentNode(agentNode, taskTreeNode));
  }

  private static void addAgentNode(AgentNode agentNode, TreeNode<Object> taskTreeNode) {
    TreeNode<Object> node = new DefaultTreeNode<>(HistoryNodeType.AGENT.value(), new AgentConversationView(agentNode.chat()), taskTreeNode);
    node.setExpanded(false);
  }

  private static TreeNode<Object> collapsed(String type, Object data, TreeNode<Object> parent) {
    TreeNode<Object> node = new DefaultTreeNode<>(type, data, parent);
    node.setExpanded(false);
    return node;
  }
}
