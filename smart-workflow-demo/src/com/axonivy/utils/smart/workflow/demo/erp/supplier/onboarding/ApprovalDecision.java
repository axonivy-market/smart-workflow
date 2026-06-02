package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding;

import dev.langchain4j.model.output.structured.Description;

@Description("Decision captured in an approval stage")
public enum ApprovalDecision {
  APPROVED,
  REJECTED,
  CHANGES_REQUESTED
}
