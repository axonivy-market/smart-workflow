package com.axonivy.utils.smart.workflow.guardrails.entity;

import java.util.Optional;

public class GuardrailResult {
  private Boolean allowed;
  private String reason;
  private String rewrittenMessage;

  private GuardrailResult(boolean allowed, String reason, String rewrittenMessage) {
    this.allowed = allowed;
    this.reason = reason;
    this.rewrittenMessage = rewrittenMessage;
  }

  public static GuardrailResult allow() {
    return new GuardrailResult(true, null, null);
  }

  public static GuardrailResult allowWithRewrite(String rewrittenMessage) {
    return new GuardrailResult(true, null, rewrittenMessage);
  }

  public static GuardrailResult block(String reason) {
    return new GuardrailResult(false, reason, null);
  }

  public Boolean isAllowed() {
    return allowed;
  }

  public String getReason() {
    return reason;
  }

  public Optional<String> getRewrittenMessage() {
    return Optional.ofNullable(rewrittenMessage);
  }
}