package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.helper;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AuditTrailEntry;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.AuditActorType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.AuditEntryType;

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
    return entry.getActorType() != null ? entry.getActorType().bubbleClass : "so-tl-bubble-user";
  }

  /** Tabler icon — APPROVAL entries use user-check. */
  public String icon(AuditTrailEntry entry) {
    if (entry.getEntryType() == AuditEntryType.APPROVAL) return "ti-user-check";
    return entry.getActorType() != null ? entry.getActorType().icon : "ti-user";
  }

  /** CSS badge class for the actor type — APPROVAL entries always green. */
  public String actorTypeBadgeClass(AuditTrailEntry entry) {
    if (entry.getEntryType() == AuditEntryType.APPROVAL) return "so-badge-green";
    if (entry.getActorType() == null) return "so-badge-gray";
    return entry.getActorType().badgeClass;
  }

  /** Display label for actor role — APPROVAL entries show "Approver". */
  public String actorRoleLabel(AuditTrailEntry entry) {
    if (entry.getEntryType() == AuditEntryType.APPROVAL) return "Approver";
    if (entry.getActorType() == AuditActorType.AGENT) return "Agent";
    if (entry.getActorType() == AuditActorType.SYSTEM) return "System";
    return "User";
  }
}
