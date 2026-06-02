package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding;

import dev.langchain4j.model.output.structured.Description;

@Description("Actor category for audit timeline entries")
public enum AuditActorType {
  USER  ("so-tl-bubble-user",    "ti-user"),
  AGENT ("so-tl-bubble-agent",   "ti-robot"),
  SYSTEM("so-tl-bubble-pending", "ti-settings");

  public final String bubbleClass;
  public final String icon;

  AuditActorType(String bubbleClass, String icon) {
    this.bubbleClass = bubbleClass;
    this.icon = icon;
  }
}