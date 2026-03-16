package com.axonivy.utils.smart.workflow.governance.ui.bean;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.axonivy.utils.smart.workflow.governance.history.ChatHistoryEntry;
import com.axonivy.utils.smart.workflow.governance.history.HistoryFilter;
import com.axonivy.utils.smart.workflow.governance.history.HistoryStorage;

@ManagedBean
@ViewScoped
public class HistoryDashboardBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private final HistoryStorage storage = HistoryStorage.create();

  private String filterCase = "";
  private String filterTaskUuid = "";
  private String filterModel = "";
  private String filterDateRange = "LAST_30_DAYS";

  private List<ChatHistoryEntry> entries = List.of();
  private ChatHistoryEntry selectedEntry;

  @PostConstruct
  public void init() {
    applyFilter();
  }

  public void applyFilter() {
    HistoryFilter filter = new HistoryFilter(
        nullIfEmpty(filterCase),
        nullIfEmpty(filterTaskUuid),
        nullIfEmpty(filterModel),
        resolveDateFrom(filterDateRange),
        resolveDateTo(filterDateRange));
    entries = storage.query(filter);
  }

  public int getEntryCount() {
    return entries.size();
  }

  private String nullIfEmpty(String value) {
    return (value == null || value.isBlank()) ? null : value;
  }

  private LocalDate resolveDateFrom(String range) {
    return switch (range) {
      case "TODAY" -> LocalDate.now();
      case "LAST_7_DAYS" -> LocalDate.now().minusDays(6);
      case "LAST_30_DAYS" -> LocalDate.now().minusDays(29);
      default -> null;
    };
  }

  private LocalDate resolveDateTo(String range) {
    return switch (range) {
      case "TODAY", "LAST_7_DAYS", "LAST_30_DAYS" -> LocalDate.now();
      default -> null;
    };
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

  public List<ChatHistoryEntry> getEntries() { return entries; }

  public ChatHistoryEntry getSelectedEntry() { return selectedEntry; }
  public void setSelectedEntry(ChatHistoryEntry v) { this.selectedEntry = v; }

}
