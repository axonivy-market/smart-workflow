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

  interface Bubble {
    String COMPLETED = "so-tl-bubble-completed";
    String AGENT     = "so-tl-bubble-agent";
    String PENDING   = "so-tl-bubble-pending";
    String USER      = "so-tl-bubble-user";
  }

  interface Icon {
    String USER_CHECK      = "ti-user-check";
    String ROBOT           = "ti-robot";
    String SETTINGS        = "ti-settings";
    String USER            = "ti-user";
    String CIRCLE_CHECK    = "ti-circle-check";
    String ALERT_TRIANGLE  = "ti-alert-triangle";
    String CIRCLE_X        = "ti-circle-x";
    String FILE_CERTIFICATE = "ti-file-certificate";
    String MESSAGE         = "ti-message";
  }

  interface Badge {
    String GREEN  = "so-badge-green";
    String RED    = "so-badge-red";
    String YELLOW = "so-badge-yellow";
    String GRAY   = "so-badge-gray";
    String PURPLE = "so-badge-purple";
    String BLUE   = "so-badge-blue";
  }

  interface Finding {
    String GREEN  = "so-finding-green";
    String YELLOW = "so-finding-yellow";
    String RED    = "so-finding-red";
  }

  interface LogLine {
    String OK      = "so-log-line-ok";
    String WARNING = "so-log-line-warning";
    String ERROR   = "so-log-line-error";
  }

  interface Label {
    String APPROVER            = "Approver";
    String AGENT               = "Agent";
    String SYSTEM              = "System";
    String USER                = "User";
    String RESOLUTION_DOCUMENT = "Document uploaded";
  }

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
      return Bubble.COMPLETED;
    }
    if (entry.getActorType() == null) {
      return Bubble.USER;
    }
    return switch (entry.getActorType()) {
      case AGENT  -> Bubble.AGENT;
      case SYSTEM -> Bubble.PENDING;
      default     -> Bubble.USER;
    };
  }

  public String icon(AuditTrailEntry entry) {
    if (entry.getEntryType() == AuditEntryType.APPROVAL) {
      return Icon.USER_CHECK;
    }
    if (entry.getActorType() == null) {
      return Icon.USER;
    }
    return switch (entry.getActorType()) {
      case AGENT  -> Icon.ROBOT;
      case SYSTEM -> Icon.SETTINGS;
      default     -> Icon.USER;
    };
  }

  public String actorTypeBadgeClass(AuditTrailEntry entry) {
    if (entry.getEntryType() == AuditEntryType.APPROVAL) {
      return Badge.GREEN;
    }
    if (entry.getActorType() == null) {
      return Badge.GRAY;
    }
    return switch (entry.getActorType()) {
      case AGENT  -> Badge.PURPLE;
      case SYSTEM -> Badge.GRAY;
      default     -> Badge.BLUE;
    };
  }

  public String actorRoleLabel(AuditTrailEntry entry) {
    if (entry.getEntryType() == AuditEntryType.APPROVAL) {
      return Label.APPROVER;
    }
    if (entry.getActorType() == AuditActorType.AGENT) {
      return Label.AGENT;
    }
    if (entry.getActorType() == AuditActorType.SYSTEM) {
      return Label.SYSTEM;
    }
    return Label.USER;
  }

  public String decisionBadgeClass(ApprovalDecision decision) {
    if (decision == null) {
      return "";
    }
    return switch (decision) {
      case APPROVED          -> Badge.GREEN;
      case REJECTED          -> Badge.RED;
      case CHANGES_REQUESTED -> Badge.YELLOW;
      default                -> "";
    };
  }

  public String findingRowClass(ValidationFinding finding) {
    FindingSeverity s = finding != null ? finding.getSeverity() : null;
    if (s == null) {
      return Finding.GREEN;
    }
    return switch (s) {
      case WARNING -> Finding.YELLOW;
      case FAILURE -> Finding.RED;
      default      -> Finding.GREEN;
    };
  }

  public String findingIcon(ValidationFinding finding) {
    FindingSeverity s = finding != null ? finding.getSeverity() : null;
    if (s == null) {
      return Icon.CIRCLE_CHECK;
    }
    return switch (s) {
      case WARNING -> Icon.ALERT_TRIANGLE;
      case FAILURE -> Icon.CIRCLE_X;
      default      -> Icon.CIRCLE_CHECK;
    };
  }

  public String findingBadgeClass(ValidationFinding finding) {
    FindingSeverity s = finding != null ? finding.getSeverity() : null;
    if (s == null) {
      return Badge.GREEN;
    }
    return switch (s) {
      case WARNING -> Badge.YELLOW;
      case FAILURE -> Badge.RED;
      default      -> Badge.GREEN;
    };
  }

  public String findingLogClass(ValidationFinding finding) {
    FindingSeverity s = finding != null ? finding.getSeverity() : null;
    if (s == null) {
      return LogLine.OK;
    }
    return switch (s) {
      case WARNING -> LogLine.WARNING;
      case FAILURE -> LogLine.ERROR;
      default      -> LogLine.OK;
    };
  }

  public String resolvedItemIcon(String resolutionType) {
    return Label.RESOLUTION_DOCUMENT.equals(resolutionType) ? Icon.FILE_CERTIFICATE : Icon.MESSAGE;
  }
}
