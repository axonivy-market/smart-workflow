package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums;

import dev.langchain4j.model.output.structured.Description;

@Description("Approval stage in the green happy path")
public enum ApprovalStage {
  SUPERVISOR,
  QM_ISM
}
