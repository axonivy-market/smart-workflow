package com.axonivy.utils.smart.workflow.guardrails.entity;

public class GuardrailResult {
  private Boolean allowed;
  private String reason;

  private GuardrailResult(boolean allowed, String reason) {
    this.allowed = allowed;
    this.reason = reason;
  }

  public static GuardrailResult allow() {
    return new GuardrailResult(true, null);
  }

  public static GuardrailResult block(String reason) {
    return new GuardrailResult(false, reason);
  }

  public Boolean isAllowed() {
    return allowed;
  }

  public String getReason() {
    return reason;
  }
}