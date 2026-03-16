package com.axonivy.utils.smart.workflow.governance.history;

import java.time.LocalDate;

public record HistoryFilter(
    String caseSearch,
    String taskUuid,
    String modelName,
    LocalDate dateFrom,
    LocalDate dateTo) {

  public static HistoryFilter empty() {
    return new HistoryFilter(null, null, null, null, null);
  }
}
