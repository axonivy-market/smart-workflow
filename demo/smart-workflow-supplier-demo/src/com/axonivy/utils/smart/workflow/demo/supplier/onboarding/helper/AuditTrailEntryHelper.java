package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.audit.AuditTrailEntry;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.ApprovalDecision;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AuditActorType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AuditEntryType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.FindingSeverity;

import ch.ivyteam.ivy.environment.Ivy;

@ManagedBean(name = "auditTrailHelper")
@ApplicationScoped
public class AuditTrailEntryHelper {

  // bubble classes
  private static final String BUBBLE_COMPLETED = "so-tl-bubble-completed";
  private static final String BUBBLE_AGENT     = "so-tl-bubble-agent";
  private static final String BUBBLE_PENDING   = "so-tl-bubble-pending";
  private static final String BUBBLE_USER      = "so-tl-bubble-user";

  // entry icons
  private static final String ICON_USER_CHECK = "ti-user-check";
  private static final String ICON_ROBOT      = "ti-robot";
  private static final String ICON_SETTINGS   = "ti-settings";
  private static final String ICON_USER       = "ti-user";

  // badge classes
  private static final String BADGE_GREEN  = "so-badge-green";
  private static final String BADGE_RED    = "so-badge-red";
  private static final String BADGE_YELLOW = "so-badge-yellow";
  private static final String BADGE_GRAY   = "so-badge-gray";
  private static final String BADGE_PURPLE = "so-badge-purple";
  private static final String BADGE_BLUE   = "so-badge-blue";

  // finding row classes
  private static final String FINDING_GREEN  = "so-finding-green";
  private static final String FINDING_YELLOW = "so-finding-yellow";
  private static final String FINDING_RED    = "so-finding-red";

  // finding icons
  private static final String ICON_CIRCLE_CHECK   = "ti-circle-check";
  private static final String ICON_ALERT_TRIANGLE = "ti-alert-triangle";
  private static final String ICON_CIRCLE_X       = "ti-circle-x";

  // log line classes
  private static final String LOG_LINE_OK      = "so-log-line-ok";
  private static final String LOG_LINE_WARNING = "so-log-line-warning";
  private static final String LOG_LINE_ERROR   = "so-log-line-error";

  // actor role labels
  private static final String LABEL_APPROVER = "Approver";
  private static final String LABEL_AGENT    = "Agent";
  private static final String LABEL_SYSTEM   = "System";
  private static final String LABEL_USER     = "User";

  // resolved item
  private static final String RESOLUTION_TYPE_DOCUMENT = "Document uploaded";
  private static final String ICON_FILE_CERTIFICATE    = "ti-file-certificate";
  private static final String ICON_MESSAGE             = "ti-message";

  public String formatTimestamp(String timestamp) {
    if (timestamp == null || timestamp.isBlank()) {
      return "";
    }
    try {
      return DateTimeFormatter.ISO_LOCAL_DATE_TIME
          .withZone(ZoneOffset.UTC)
          .format(Instant.parse(timestamp));
    } catch (DateTimeException e) {
      Ivy.log().warn("Failed to parse timestamp '" + timestamp + "': " + e.getMessage());
      return timestamp;
    }
  }

  public String bubbleClass(AuditTrailEntry entry) {
    if (entry.getEntryType() == AuditEntryType.APPROVAL) {
      return BUBBLE_COMPLETED;
    }
    if (entry.getActorType() == null) {
      return BUBBLE_USER;
    }
    return switch (entry.getActorType()) {
      case AGENT  -> BUBBLE_AGENT;
      case SYSTEM -> BUBBLE_PENDING;
      default     -> BUBBLE_USER;
    };
  }

  public String icon(AuditTrailEntry entry) {
    if (entry.getEntryType() == AuditEntryType.APPROVAL) {
      return ICON_USER_CHECK;
    }
    if (entry.getActorType() == null) {
      return ICON_USER;
    }
    return switch (entry.getActorType()) {
      case AGENT  -> ICON_ROBOT;
      case SYSTEM -> ICON_SETTINGS;
      default     -> ICON_USER;
    };
  }

  public String actorTypeBadgeClass(AuditTrailEntry entry) {
    if (entry.getEntryType() == AuditEntryType.APPROVAL) {
      return BADGE_GREEN;
    }
    if (entry.getActorType() == null) {
      return BADGE_GRAY;
    }
    return switch (entry.getActorType()) {
      case AGENT  -> BADGE_PURPLE;
      case SYSTEM -> BADGE_GRAY;
      default     -> BADGE_BLUE;
    };
  }

  public String actorRoleLabel(AuditTrailEntry entry) {
    if (entry.getEntryType() == AuditEntryType.APPROVAL) {
      return LABEL_APPROVER;
    }
    if (entry.getActorType() == AuditActorType.AGENT) {
      return LABEL_AGENT;
    }
    if (entry.getActorType() == AuditActorType.SYSTEM) {
      return LABEL_SYSTEM;
    }
    return LABEL_USER;
  }

  public String decisionBadgeClass(ApprovalDecision decision) {
    if (decision == null) {
      return "";
    }
    return switch (decision) {
      case APPROVED          -> BADGE_GREEN;
      case REJECTED          -> BADGE_RED;
      case CHANGES_REQUESTED -> BADGE_YELLOW;
      default                -> "";
    };
  }

  public String findingRowClass(ValidationFinding finding) {
    FindingSeverity s = finding != null ? finding.getSeverity() : null;
    if (s == null) {
      return FINDING_GREEN;
    }
    return switch (s) {
      case WARNING -> FINDING_YELLOW;
      case FAILURE -> FINDING_RED;
      default      -> FINDING_GREEN;
    };
  }

  public String findingIcon(ValidationFinding finding) {
    FindingSeverity s = finding != null ? finding.getSeverity() : null;
    if (s == null) {
      return ICON_CIRCLE_CHECK;
    }
    return switch (s) {
      case WARNING -> ICON_ALERT_TRIANGLE;
      case FAILURE -> ICON_CIRCLE_X;
      default      -> ICON_CIRCLE_CHECK;
    };
  }

  public String findingBadgeClass(ValidationFinding finding) {
    FindingSeverity s = finding != null ? finding.getSeverity() : null;
    if (s == null) {
      return BADGE_GREEN;
    }
    return switch (s) {
      case WARNING -> BADGE_YELLOW;
      case FAILURE -> BADGE_RED;
      default      -> BADGE_GREEN;
    };
  }

  public String findingLogClass(ValidationFinding finding) {
    FindingSeverity s = finding != null ? finding.getSeverity() : null;
    if (s == null) {
      return LOG_LINE_OK;
    }
    return switch (s) {
      case WARNING -> LOG_LINE_WARNING;
      case FAILURE -> LOG_LINE_ERROR;
      default      -> LOG_LINE_OK;
    };
  }

  public String resolvedItemIcon(String resolutionType) {
    return RESOLUTION_TYPE_DOCUMENT.equals(resolutionType) ? ICON_FILE_CERTIFICATE : ICON_MESSAGE;
  }
}
