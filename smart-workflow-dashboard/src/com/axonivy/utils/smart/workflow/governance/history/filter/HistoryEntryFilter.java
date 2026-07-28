package com.axonivy.utils.smart.workflow.governance.history.filter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.ChatHistoryJsonParser;
import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.service.CaseService;
import com.axonivy.utils.smart.workflow.governance.ui.enums.DateRange;

import ch.ivyteam.ivy.environment.Ivy;

public class HistoryEntryFilter {

  private static final String ERROR_PARSING_DATE = "Failed to parse lastUpdated: {0}";

  private HistoryEntryFilter() {}

  public static List<AgentConversationEntry> filterByDateRange(
      List<AgentConversationEntry> entries, LocalDate from, LocalDate to) {
    return entries.stream()
        .filter(e -> isWithinDateRange(e, from, to))
        .toList();
  }

  public static List<AgentConversationEntry> filter(
      List<AgentConversationEntry> entries,
      String caseSearch,
      String modelName,
      String dateRange) {
    return entries.stream()
        .filter(e -> matchesCaseFilter(e, caseSearch))
        .filter(e -> matchesModelFilter(e, modelName))
        .filter(e -> matchesDateRangeFilter(e, dateRange))
        .toList();
  }

  private static boolean matchesCaseFilter(AgentConversationEntry entry, String filterCase) {
    if (filterCase == null || filterCase.isBlank()) {
      return true;
    }
    return CaseService.matchesSearch(entry.getCaseUuid(), filterCase.trim());
  }

  private static boolean matchesModelFilter(AgentConversationEntry entry, String filterModel) {
    if (filterModel == null || filterModel.isBlank()) {
      return true;
    }
    String stored = ChatHistoryJsonParser.getModelName(entry);
    return stored != null && (stored.contains(filterModel) || filterModel.contains(stored));
  }

  private static boolean isWithinDateRange(AgentConversationEntry entry, LocalDate from, LocalDate to) {
    if (entry.getLastUpdated() == null) {
      return false;
    }
    try {
      LocalDate d = LocalDateTime.parse(entry.getLastUpdated()).toLocalDate();
      return !d.isBefore(from) && !d.isAfter(to);
    } catch (DateTimeParseException ex) {
      Ivy.log().warn(ERROR_PARSING_DATE, ex.getMessage());
      return false;
    }
  }

  private static boolean matchesDateRangeFilter(AgentConversationEntry entry, String filterDateRange) {
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
      Ivy.log().warn(ERROR_PARSING_DATE, ex.getMessage());
      return false;
    }
    LocalDateTime now = LocalDateTime.now();
    return switch (DateRange.valueOf(filterDateRange)) {
      case TODAY -> !updated.toLocalDate().isBefore(now.toLocalDate());
      case LAST_7_DAYS -> updated.isAfter(now.minusDays(7));
      case LAST_30_DAYS -> updated.isAfter(now.minusDays(30));
      case ALL -> true;
    };
  }
}
