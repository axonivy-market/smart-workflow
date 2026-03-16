package com.axonivy.utils.smart.workflow.governance.history;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.axonivy.utils.smart.workflow.governance.service.CaseService;
import com.axonivy.utils.smart.workflow.governance.utils.ChatHistoryJsonParser;

public interface HistoryStorage {

  List<ChatHistoryEntry> findAll();

  void save(ChatHistoryEntry entry);

  void delete(ChatHistoryEntry entry);

  static HistoryStorage create() {
    return new IvyRepoHistoryStorage();
  }

  default List<ChatHistoryEntry> query(HistoryFilter filter) {
    return findAll(filter).stream()
        .sorted(Comparator.comparing(
            e -> e.getLastUpdated() != null ? e.getLastUpdated() : java.time.LocalDateTime.MIN,
            Comparator.reverseOrder()))
        .collect(Collectors.toList());
  }

  default List<ChatHistoryEntry> findAll(HistoryFilter filter) {
    return findAll().stream()
        .filter(e -> matches(e, filter))
        .collect(Collectors.toList());
  }

  private static boolean matches(ChatHistoryEntry e, HistoryFilter f) {
    if (f.caseSearch() != null && !CaseService.matchesSearch(e.getCaseUuid(), f.caseSearch())) {
      return false;
    }
    if (f.taskUuid() != null && !e.getTaskUuid().contains(f.taskUuid())) {
      return false;
    }
    if (f.modelName() != null && !f.modelName().equals(ChatHistoryJsonParser.getModelName(e))) {
      return false;
    }
    if (f.dateFrom() != null && e.getLastUpdated().toLocalDate().isBefore(f.dateFrom())) {
      return false;
    }
    if (f.dateTo() != null && e.getLastUpdated().toLocalDate().isAfter(f.dateTo())) {
      return false;
    }
    return true;
  }
}
