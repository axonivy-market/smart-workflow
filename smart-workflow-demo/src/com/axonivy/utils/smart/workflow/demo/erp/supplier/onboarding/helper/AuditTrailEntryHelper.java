package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.helper;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AuditTrailEntry;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.ApprovalDecision;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.AuditActorType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.AuditEntryType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.FindingSeverity;

@ManagedBean(name = "auditTrailHelper")
@ApplicationScoped
public class AuditTrailEntryHelper {

  /** Human-readable timestamp: "19 May 2026 · 09:10". Falls back to raw value on parse error. */
  public String formatTimestamp(String timestamp) {
    if (timestamp == null || timestamp.isBlank()) return "";
    try {
      return DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm")
          .withZone(ZoneOffset.UTC)
          .format(Instant.parse(timestamp));
    } catch (Exception e) {
      return timestamp;
    }
  }

  /** CSS bubble class — APPROVAL entries use the completed style. */
  public String bubbleClass(AuditTrailEntry entry) {
    if (entry.getEntryType() == AuditEntryType.APPROVAL) return "so-tl-bubble-completed";
    if (entry.getActorType() == null) return "so-tl-bubble-user";
    switch (entry.getActorType()) {
      case AGENT:  return "so-tl-bubble-agent";
      case SYSTEM: return "so-tl-bubble-pending";
      default:     return "so-tl-bubble-user";
    }
  }

  /** Tabler icon — APPROVAL entries use user-check. */
  public String icon(AuditTrailEntry entry) {
    if (entry.getEntryType() == AuditEntryType.APPROVAL) return "ti-user-check";
    if (entry.getActorType() == null) return "ti-user";
    switch (entry.getActorType()) {
      case AGENT:  return "ti-robot";
      case SYSTEM: return "ti-settings";
      default:     return "ti-user";
    }
  }

  /** CSS badge class for the actor type — APPROVAL entries always green. */
  public String actorTypeBadgeClass(AuditTrailEntry entry) {
    if (entry.getEntryType() == AuditEntryType.APPROVAL) return "so-badge-green";
    if (entry.getActorType() == null) return "so-badge-gray";
    switch (entry.getActorType()) {
      case AGENT:  return "so-badge-purple";
      case SYSTEM: return "so-badge-gray";
      default:     return "so-badge-blue";
    }
  }

  /** Display label for actor role — APPROVAL entries show "Approver". */
  public String actorRoleLabel(AuditTrailEntry entry) {
    if (entry.getEntryType() == AuditEntryType.APPROVAL) return "Approver";
    if (entry.getActorType() == AuditActorType.AGENT) return "Agent";
    if (entry.getActorType() == AuditActorType.SYSTEM) return "System";
    return "User";
  }

  /** CSS badge class for an approval decision. */
  public String decisionBadgeClass(ApprovalDecision decision) {
    if (decision == null) return "";
    switch (decision) {
      case APPROVED:           return "so-badge-green";
      case REJECTED:           return "so-badge-red";
      case CHANGES_REQUESTED:  return "so-badge-yellow";
      default:                 return "";
    }
  }

  /** CSS row class for a validation finding. */
  public String findingRowClass(ValidationFinding finding) {
    FindingSeverity s = finding != null ? finding.getSeverity() : null;
    if (s == null) return "so-finding-green";
    switch (s) {
      case WARNING: return "so-finding-yellow";
      case FAILURE: return "so-finding-red";
      default:      return "so-finding-green";
    }
  }

  /** Tabler icon for a validation finding. */
  public String findingIcon(ValidationFinding finding) {
    FindingSeverity s = finding != null ? finding.getSeverity() : null;
    if (s == null) return "ti-circle-check";
    switch (s) {
      case WARNING: return "ti-alert-triangle";
      case FAILURE: return "ti-circle-x";
      default:      return "ti-circle-check";
    }
  }

  /** CSS badge class for a validation finding. */
  public String findingBadgeClass(ValidationFinding finding) {
    FindingSeverity s = finding != null ? finding.getSeverity() : null;
    if (s == null) return "so-badge-green";
    switch (s) {
      case WARNING: return "so-badge-yellow";
      case FAILURE: return "so-badge-red";
      default:      return "so-badge-green";
    }
  }

  /** CSS log-line class for a validation finding. */
  public String findingLogClass(ValidationFinding finding) {
    FindingSeverity s = finding != null ? finding.getSeverity() : null;
    if (s == null) return "so-log-line-ok";
    switch (s) {
      case WARNING: return "so-log-line-warning";
      case FAILURE: return "so-log-line-error";
      default:      return "so-log-line-ok";
    }
  }

  /** Tabler icon for a resolved audit item based on its resolution type. */
  public String resolvedItemIcon(String resolutionType) {
    return "Document uploaded".equals(resolutionType) ? "ti-file-certificate" : "ti-message";
  }
}
