package com.axonivy.utils.smart.workflow.governance.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.history.internal.HistoryEntryFilter;
import com.axonivy.utils.smart.workflow.governance.ui.enums.DateRange;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestHistoryEntryFilter {

  @ParameterizedTest(name = "{argumentSetNameOrArgumentsWithNames}")
  @MethodSource("blankCaseSearchArgs")
  void blankCaseSearch_filter_passesThrough(String caseSearch) {
    var entry = chatEntry("agent-1", "case-1", "task-1", LocalDateTime.now());
    var result = HistoryEntryFilter.filter(List.of(entry), caseSearch, "", DateRange.ALL.name());
    assertThat(result).containsExactly(entry);
  }

  @SuppressWarnings("unused")
  static Stream<Arguments> blankCaseSearchArgs() {
    return Stream.of(
        Arguments.argumentSet("null_caseSearch_passesThrough",  (String) null),
        Arguments.argumentSet("empty_caseSearch_passesThrough", ""),
        Arguments.argumentSet("blank_caseSearch_passesThrough", " ")
    );
  }

  @ParameterizedTest(name = "{argumentSetNameOrArgumentsWithNames}")
  @MethodSource("modelFilterArgs")
  void modelName_filter_matchesExpected(String filterModel, String storedModel, boolean expectedIncluded) {
    var entry = entryWithModel(storedModel);
    var result = HistoryEntryFilter.filter(List.of(entry), "", filterModel, DateRange.ALL.name());
    assertThat(result.contains(entry)).isEqualTo(expectedIncluded);
  }

  @SuppressWarnings("unused")
  static Stream<Arguments> modelFilterArgs() {
    return Stream.of(
        Arguments.argumentSet("blank_modelFilter_included",           "",       "gpt-4",       true),
        Arguments.argumentSet("exactMatch_modelFilter_included",       "gpt-4",  "gpt-4",       true),
        Arguments.argumentSet("storedContainsFilter_modelFilter_included", "gpt", "gpt-4-turbo", true),
        Arguments.argumentSet("filterContainsStored_modelFilter_included", "gpt-4", "gpt",      true),
        Arguments.argumentSet("mismatch_modelFilter_excluded",        "claude", "gpt-4",       false),
        Arguments.argumentSet("nullModel_modelFilter_excluded",        "gpt-4",  null,          false)
    );
  }

  @ParameterizedTest(name = "{argumentSetNameOrArgumentsWithNames}")
  @MethodSource("dateRangeArgs")
  void dateRange_filter_matchesExpected(String dateRange, String lastUpdated, boolean expectedIncluded) {
    var entry = chatEntry("agent-1", "case-1", "task-1", null);
    entry.setLastUpdated(lastUpdated);
    var result = HistoryEntryFilter.filter(List.of(entry), "", "", dateRange);
    assertThat(result.contains(entry)).isEqualTo(expectedIncluded);
  }

  @SuppressWarnings("unused")
  static Stream<Arguments> dateRangeArgs() {
    LocalDateTime now = LocalDateTime.now();
    return Stream.of(
        Arguments.argumentSet("nullDate_ALL_included",              DateRange.ALL.name(),          null,                             true),
        Arguments.argumentSet("invalidDate_ALL_included",           DateRange.ALL.name(),          "not-a-date",                     true),
        Arguments.argumentSet("today_TODAY_included",               DateRange.TODAY.name(),        now.toString(),                   true),
        Arguments.argumentSet("yesterday_TODAY_excluded",           DateRange.TODAY.name(),        now.minusDays(1).toString(),      false),
        Arguments.argumentSet("sixDaysAgo_LAST7DAYS_included",      DateRange.LAST_7_DAYS.name(),  now.minusDays(6).toString(),      true),
        Arguments.argumentSet("sevenDaysAgo_LAST7DAYS_excluded",    DateRange.LAST_7_DAYS.name(),  now.minusDays(7).toString(),      false),
        Arguments.argumentSet("twentyNineDaysAgo_LAST30DAYS_included", DateRange.LAST_30_DAYS.name(), now.minusDays(29).toString(), true),
        Arguments.argumentSet("thirtyDaysAgo_LAST30DAYS_excluded",  DateRange.LAST_30_DAYS.name(), now.minusDays(30).toString(),    false),
        Arguments.argumentSet("nullDate_TODAY_excluded",            DateRange.TODAY.name(),        null,                             false),
        Arguments.argumentSet("invalidDate_TODAY_excluded",         DateRange.TODAY.name(),        "not-a-date",                     false)
    );
  }

  @ParameterizedTest(name = "{argumentSetNameOrArgumentsWithNames}")
  @MethodSource("filterByDateRangeArgs")
  void filterByDateRange_matchesExpected(String lastUpdated, LocalDate from, LocalDate to, boolean expectedIncluded) {
    var entry = chatEntry("agent-1", "case-1", "task-1", null);
    entry.setLastUpdated(lastUpdated);
    var result = HistoryEntryFilter.filterByDateRange(List.of(entry), from, to);
    assertThat(result.contains(entry)).isEqualTo(expectedIncluded);
  }

  @SuppressWarnings("unused")
  static Stream<Arguments> filterByDateRangeArgs() {
    LocalDate today = LocalDate.now();
    LocalDateTime now = LocalDateTime.now();
    return Stream.of(
        Arguments.argumentSet("today_withinRange_included",
            now.toString(), today.minusDays(6), today, true),
        Arguments.argumentSet("boundaryFrom_withinRange_included",
            now.minusDays(6).toString(), today.minusDays(6), today, true),
        Arguments.argumentSet("oneDayBeforeFrom_excluded",
            now.minusDays(7).toString(), today.minusDays(6), today, false),
        Arguments.argumentSet("nullDate_excluded",
            null, today.minusDays(6), today, false),
        Arguments.argumentSet("invalidDate_excluded",
            "not-a-date", today.minusDays(6), today, false)
    );
  }

  private static AgentConversationEntry chatEntry(String agentId, String caseUuid, String taskUuid,
      LocalDateTime lastUpdated) {
    var e = new AgentConversationEntry();
    e.setAgentId(agentId);
    e.setCaseUuid(caseUuid);
    e.setTaskUuid(taskUuid);
    if (lastUpdated != null) {
      e.setLastUpdated(lastUpdated.toString());
    }
    return e;
  }

  private static AgentConversationEntry entryWithModel(String model) {
    var e = new AgentConversationEntry();
    e.setCaseUuid("case-1");
    e.setTaskUuid("task-1");
    if (model != null) {
      e.setTokenUsageJson("[{\"modelName\":\"" + model + "\",\"totalTokens\":0}]");
    }
    return e;
  }
}
