package com.axonivy.utils.smart.workflow.governance.history.storage.internal;

import java.util.Optional;

import com.axonivy.utils.smart.workflow.governance.history.entity.AiGovernanceReportEntry;
import com.axonivy.utils.smart.workflow.governance.history.storage.AiGovernanceReportStorage;

import ch.ivyteam.ivy.environment.Ivy;

public class IvyRepoAiReportStorage implements AiGovernanceReportStorage {

  @Override
  public Optional<AiGovernanceReportEntry> findByCaseUuid(String caseUuid) {
    return Ivy.repo().search(AiGovernanceReportEntry.class)
        .textField("caseUuid").isEqualToIgnoringCase(caseUuid)
        .execute().getAll().stream().findFirst();
  }

  @Override
  public void save(AiGovernanceReportEntry entry) {
    Ivy.repo().save(entry);
  }

  @Override
  public void delete(AiGovernanceReportEntry entry) {
    Ivy.repo().delete(entry);
  }
}
