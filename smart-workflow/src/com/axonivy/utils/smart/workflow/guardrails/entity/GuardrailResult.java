package com.axonivy.utils.smart.workflow.guardrails.entity;

import java.util.Optional;

public class GuardrailResult {
  private Boolean allowed;
  private String reason;
  private String rewrittenMessage;
  private Throwable cause;

  private GuardrailResult(boolean allowed, String reason, String rewrittenMessage, Throwable cause) {
    this.allowed = allowed;
    this.reason = reason;
    this.rewrittenMessage = rewrittenMessage;
    this.cause = cause;
  }

  public static GuardrailResult allow() {
    return new GuardrailResult(true, null, null, null);
  }

  public static GuardrailResult allowWithRewrite(String rewrittenMessage) {
    return new GuardrailResult(true, null, rewrittenMessage, null);
  }

  public static GuardrailResult block(String reason) {
    return new GuardrailResult(false, reason, null, null);
  }

  /**
   * Blocks with a typed cause. The cause travels through langchain4j's guardrail exception, letting callers
   * distinguish <em>which</em> guardrail blocked without inspecting the human-readable reason.
   */
  public static GuardrailResult block(String reason, Throwable cause) {
    return new GuardrailResult(false, reason, null, cause);
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

  public Optional<Throwable> getCause() {
    return Optional.ofNullable(cause);
  }
}
