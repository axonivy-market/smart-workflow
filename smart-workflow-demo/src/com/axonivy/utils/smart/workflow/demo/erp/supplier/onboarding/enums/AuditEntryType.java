package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums;

import dev.langchain4j.model.output.structured.Description;

@Description("Type of audit trail entry — drives which fields and rendering apply")
public enum AuditEntryType {
  REQUEST_SUBMITTED,
  DUPLICATE_CHECK,
  DUPLICATE_DECISION,
  REGISTRATION_CAPTURED,
  AI_ANALYSIS,
  CLARIFICATION_REQUIRED,
  CLARIFICATION_SUBMITTED,
  QM_ASSISTANCE,
  APPROVAL,
  DECLINE,
  COMPLETION
}
