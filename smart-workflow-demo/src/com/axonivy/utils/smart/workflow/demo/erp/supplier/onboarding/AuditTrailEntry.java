package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding;

import java.util.List;

import dev.langchain4j.model.output.structured.Description;

public class AuditTrailEntry {

  @Description("Event timestamp in ISO-8601 text format")
  private String timestamp;

  @Description("Display name of the actor")
  private String actor;

  @Description("Actor type such as USER, AGENT, APPROVER, SYSTEM")
  private AuditActorType actorType;

  @Description("Functional action summary shown to users")
  private String action;

  @Description("Optional technical detail, tool, or source reference")
  private String technicalDetail;

  /** Resolved clarification items attached to this entry. Runtime-only, not part of LLM extraction. */
  private List<ResolvedClarificationItem> resolvedItems;

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public String getActor() {
    return actor;
  }

  public void setActor(String actor) {
    this.actor = actor;
  }

  public AuditActorType getActorType() {
    return actorType;
  }

  public void setActorType(AuditActorType actorType) {
    this.actorType = actorType;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getTechnicalDetail() {
    return technicalDetail;
  }

  public void setTechnicalDetail(String technicalDetail) {
    this.technicalDetail = technicalDetail;
  }

  public List<ResolvedClarificationItem> getResolvedItems() {
    return resolvedItems;
  }

  public void setResolvedItems(List<ResolvedClarificationItem> resolvedItems) {
    this.resolvedItems = resolvedItems;
  }
}
