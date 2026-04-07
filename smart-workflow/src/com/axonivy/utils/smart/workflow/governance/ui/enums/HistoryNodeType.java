package com.axonivy.utils.smart.workflow.governance.ui.enums;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

public enum HistoryNodeType {

  ROOT("root", false),
  CASE("case", false),
  TASK("task", false),
  AGENT("agent", false);

  private final String key;
  private final boolean expandedByDefault;

  HistoryNodeType(String key, boolean expandedByDefault) {
    this.key = key;
    this.expandedByDefault = expandedByDefault;
  }

  public String getKey() {
    return key;
  }

  public boolean isExpandedByDefault() {
    return expandedByDefault;
  }

  public TreeNode<Object> createNode(Object data, TreeNode<Object> parent) {
    TreeNode<Object> node = new DefaultTreeNode<>(key, data, parent);
    node.setExpanded(expandedByDefault);
    return node;
  }
}