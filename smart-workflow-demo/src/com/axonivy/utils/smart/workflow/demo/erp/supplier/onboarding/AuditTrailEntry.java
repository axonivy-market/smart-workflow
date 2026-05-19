package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ApprovalDecision;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ApprovalStage;
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

  /** Decline reasons taken from FAILURE-severity findings. Set only on decline entries. */
  private List<String> declineReasons;

  /** Typed approval decision. Set only on approval entries. */
  private ApprovalDecision decision;

  /** Approval stage (SUPERVISOR / QM_ISM). Set only on approval entries. */
  private ApprovalStage stage;

  /** Free-text approval comment. Set only on approval entries. */
  private String comment;

  /** Resolved clarification items attached to this entry. Runtime-only, not part of LLM extraction. */
  private List<ResolvedClarificationItem> resolvedItems;

  /** Semantic kind: AI for agent/system entries, USER for human-authored entries. */
  private AuditEntryKind kind;

  /** For USER entries: APPROVAL (decision) or REQUEST (initial submission summary). */
  private AuditUserItemType itemType;

  /** Populated only when itemType == REQUEST. Structured summary lines from the onboarding form. */
  private List<RequestSummaryLine> requestSummaryLines;

  /** Snapshot of ValidationFindings captured when an AI analysis completes. */
  private List<ValidationFinding> findings;

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  /** Human-readable timestamp: "19 May 2026 · 09:10". Falls back to raw value on parse error. */
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

  /** CSS badge class for the decision outcome: green / red / yellow. */
  public String getDecisionBadgeClass() {
    if (decision == null) return "";
    return switch (decision) {
      case APPROVED           -> "so-badge-green";
      case REJECTED           -> "so-badge-red";
      case CHANGES_REQUESTED  -> "so-badge-yellow";
    };
  }

  /** CSS badge class matching the actor-type bubble colour. */
  public String getActorTypeBadgeClass() {
    if (actorType == null) return "so-badge-gray";
    return switch (actorType) {
      case APPROVER -> "so-badge-green";
      case AGENT    -> "so-badge-purple";
      case SYSTEM   -> "so-badge-gray";
      case USER     -> "so-badge-blue";
    };
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

  public AuditEntryKind getKind() {
    return kind;
  }

  public void setKind(AuditEntryKind kind) {
    this.kind = kind;
  }

  public AuditUserItemType getItemType() {
    return itemType;
  }

  public void setItemType(AuditUserItemType itemType) {
    this.itemType = itemType;
  }

  public List<RequestSummaryLine> getRequestSummaryLines() {
    return requestSummaryLines;
  }

  public void setRequestSummaryLines(List<RequestSummaryLine> requestSummaryLines) {
    this.requestSummaryLines = requestSummaryLines;
  }

  public List<ValidationFinding> getFindings() {
    return findings;
  }

  public void setFindings(List<ValidationFinding> findings) {
    this.findings = findings;
  }
}
