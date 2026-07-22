package com.axonivy.utils.smart.workflow.governance.ui.bean;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.filter.HistoryEntryFilter;
import com.axonivy.utils.smart.workflow.governance.history.storage.HistoryStorage;
import com.axonivy.utils.smart.workflow.governance.history.storage.IvyRepoHistoryStorage;
import com.axonivy.utils.smart.workflow.governance.ui.HistoryTreeBuilder;
import com.axonivy.utils.smart.workflow.governance.ui.enums.DateRange;
import com.axonivy.utils.smart.workflow.model.ChatModelFactory;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;

import ch.ivyteam.ivy.environment.Ivy;
import jakarta.annotation.PostConstruct;
import jakarta.faces.model.SelectItem;
import jakarta.faces.model.SelectItemGroup;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named
@ViewScoped
public class GovernanceDashboardBean implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final String ERROR_LOADING_HISTORY = "Failed to load governance history";

  private static List<SelectItem> availableModelItemsCache;

  private HistoryStorage storage;

  private String filterCase = "";
  private String filterModel = "";
  private String filterDateRange = DateRange.LAST_30_DAYS.name();

  private List<AgentConversationEntry> entries = List.of();
  private TreeNode<Object> historyTree = new DefaultTreeNode<>();
  private AgentConversationEntry selectedEntry;

  @PostConstruct
  public void init() {
    storage = new IvyRepoHistoryStorage();
    applyFilter();
  }

  public void applyFilter() {
    try {
      entries = HistoryEntryFilter.filter(storage.findAll(), filterCase, filterModel, filterDateRange);
    } catch (Exception e) {
      Ivy.log().error(ERROR_LOADING_HISTORY, e);
      entries = List.of();
    }
    historyTree = HistoryTreeBuilder.build(entries);
  }

  public int getEntryCount() {
    return entries.size();
  }

  public int getCaseCount() {
    return historyTree.getChildCount();
  }

  public List<SelectItem> getAvailableModelItems() {
    if (availableModelItemsCache == null) {
      availableModelItemsCache = ChatModelFactory.providers().stream()
          .sorted(Comparator.comparing(ChatModelProvider::name))
          .filter(provider -> !provider.models().isEmpty())
          .map(GovernanceDashboardBean::toSelectItemGroup)
          .toList();
    }
    return availableModelItemsCache;
  }

  private static SelectItem toSelectItemGroup(ChatModelProvider provider) {
    SelectItemGroup group = new SelectItemGroup(provider.name());
    group.setSelectItems(provider.models().stream()
        .map(model -> new SelectItem(model, model))
        .toArray(SelectItem[]::new));
    return group;
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
