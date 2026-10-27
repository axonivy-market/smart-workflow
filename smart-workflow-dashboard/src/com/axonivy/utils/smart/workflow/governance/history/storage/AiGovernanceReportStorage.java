package com.axonivy.utils.smart.workflow.governance.history.storage;

import java.util.Optional;

import com.axonivy.utils.smart.workflow.governance.history.entity.AiGovernanceReportEntry;

public interface AiGovernanceReportStorage {
  Optional<AiGovernanceReportEntry> findByCaseUuid(String caseUuid);
  void save(AiGovernanceReportEntry entry);
  void delete(AiGovernanceReportEntry entry);
}
