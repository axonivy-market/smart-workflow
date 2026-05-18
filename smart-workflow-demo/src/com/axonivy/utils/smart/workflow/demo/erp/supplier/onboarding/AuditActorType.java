package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding;

import dev.langchain4j.model.output.structured.Description;

@Description("Actor category for audit timeline entries")
public enum AuditActorType {
  USER,
  AGENT,
  APPROVER,
  SYSTEM
}
