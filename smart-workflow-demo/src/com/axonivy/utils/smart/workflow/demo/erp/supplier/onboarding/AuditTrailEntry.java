package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.ApprovalDecision;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.ApprovalStage;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.AuditActorType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.AuditEntryType;

import dev.langchain4j.model.output.structured.Description;

public class AuditTrailEntry {

  @Description("Event timestamp in ISO-8601 text format")
  private String timestamp;

  @Description("Display name of the actor")
  private String actor;

  @Description("Actor type such as USER, AGENT, SYSTEM")
  private AuditActorType actorType;

  @Description("Functional action summary shown to users")
  private String action;

  @Description("Optional technical detail, tool, or source reference")
  private String technicalDetail;

  /** Decline reasons taken from FAILURE-severity findings. Set only on decline entries. */
  private List<String> declineReasons;

  /** Typed approval decision. Set only on approval entries. */
  private ApprovalDecision decision;

  /** Approval stage (SUPERVISOR / QM_ISM). Set only on approval entries. */
  private ApprovalStage stage;

  /** Free-text approval comment. Set only on approval entries. */
  private String comment;

  /** Resolved clarification items attached to this entry. Runtime-only. */
  private List<ResolvedClarificationItem> resolvedItems;

  /** Explicit type of this audit entry — drives rendering and field ownership. */
  private AuditEntryType entryType;

  /** Populated only when entryType == REQUEST_SUBMITTED or REGISTRATION_CAPTURED. */
  private List<RequestSummaryLine> requestSummaryLines;

  /** Snapshot of ValidationFindings captured when an AI analysis completes. */
  private List<ValidationFinding> findings;

  /** Names of matched/duplicate suppliers found during duplicate check. */
  private List<String> matchedSupplierNames;

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public String getFormattedTimestamp() {
    if (timestamp == null || timestamp.isBlank()) return "";
    try {
      return DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm")
          .withZone(ZoneOffset.UTC)
          .format(Instant.parse(timestamp));
    } catch (Exception e) {
      return timestamp;
    }
  }

  public String getActorTypeBadgeClass() {
    return effectiveActorType().badgeClass;
  }

  public String getBubbleClass() {
    return effectiveActorType().bubbleClass;
  }

  public String getIcon() {
    return effectiveActorType().icon;
  }

  public String getActorRoleLabel() {
    return effectiveActorType().roleLabel;
  }

  private AuditActorType effectiveActorType() {
    return actorType != null ? actorType : AuditActorType.USER;
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

  public AuditEntryType getEntryType() {
    return entryType;
  }

  public void setEntryType(AuditEntryType entryType) {
    this.entryType = entryType;
  }

  public List<RequestSummaryLine> getRequestSummaryLines() {
    return requestSummaryLines;
  }

  public void setRequestSummaryLines(List<RequestSummaryLine> requestSummaryLines) {
    this.requestSummaryLines = requestSummaryLines;
  }

  public String getDecisionBadgeClass() {
    return decision != null ? decision.badgeClass : "";
  }

  public List<String> getDeclineReasons() {
    return declineReasons;
  }

  public void setDeclineReasons(List<String> declineReasons) {
    this.declineReasons = declineReasons;
  }

  public ApprovalDecision getDecision() {
    return decision;
  }

  public void setDecision(ApprovalDecision decision) {
    this.decision = decision;
  }

  public ApprovalStage getStage() {
    return stage;
  }

  public void setStage(ApprovalStage stage) {
    this.stage = stage;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public List<ResolvedClarificationItem> getResolvedItems() {
    return resolvedItems;
  }

  public void setResolvedItems(List<ResolvedClarificationItem> resolvedItems) {
    this.resolvedItems = resolvedItems;
  }

  public List<ValidationFinding> getFindings() {
    return findings;
  }

  public void setFindings(List<ValidationFinding> findings) {
    this.findings = findings;
  }

  public List<String> getMatchedSupplierNames() {
    return matchedSupplierNames;
  }

  public void setMatchedSupplierNames(List<String> matchedSupplierNames) {
    this.matchedSupplierNames = matchedSupplierNames;
  }
}
