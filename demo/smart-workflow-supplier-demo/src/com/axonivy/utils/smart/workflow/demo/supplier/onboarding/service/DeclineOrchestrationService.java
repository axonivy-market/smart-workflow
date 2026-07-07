package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.SupplierRiskScore;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.audit.AuditTrailEntry;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.audit.NotificationRecord;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AuditActorType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AuditEntryType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.FindingSeverity;

public class DeclineOrchestrationService {

  public record DeclineOrchestrationResult(
      String summary,
      String caseName,
      List<NotificationRecord> notificationRecords,
      AuditTrailEntry auditEntry) {}

  private DeclineOrchestrationService() {}

  public static DeclineOrchestrationResult buildDecline(
      OnboardingRequest req, SupplierAgentResponse resp,
      boolean isWithdrawal, String requestedBy) {

    String name = OnboardingRequestUtils.supplierName(req);
    String now = Instant.now().toString();

    String summary;
    if (isWithdrawal) {
      summary = "Request withdrawn by requester during clarification cycle.";
    } else {
      StringBuilder sb = new StringBuilder("Automatic decline.");
      if (resp != null && resp.getValidationFindings() != null) {
        int count = 0;
        for (ValidationFinding f : resp.getValidationFindings()) {
          if (f.getSeverity() == FindingSeverity.FAILURE) {
            sb.append(" ").append(f.getMessage()).append(";");
            if (++count >= 5) {
              break;
            }
          }
        }
      }
      summary = sb.toString();
    }

    Integer agg = Optional.ofNullable(resp)
        .map(SupplierAgentResponse::getRiskScore)
        .map(SupplierRiskScore::getAggregate)
        .orElse(0);
    String lvl = (resp != null && resp.getRiskScore() != null && resp.getRiskScore().getLevel() != null)
        ? resp.getRiskScore().getLevel().name() : "RED";

    List<String> reasons = new ArrayList<>();
    if (resp != null && resp.getValidationFindings() != null) {
      for (ValidationFinding f : resp.getValidationFindings()) {
        if (f.getSeverity() == FindingSeverity.FAILURE) {
          reasons.add(f.getMessage());
        }
        if (reasons.size() >= 5) {
          break;
        }
      }
    }
    if (reasons.isEmpty()) {
      reasons.add("Risk score below minimum threshold");
    }

    String recipientName = (requestedBy != null && !requestedBy.isBlank()) ? requestedBy : "Requester";
    List<NotificationRecord> notifications = new ArrayList<>();
    NotificationRecord nr1 = new NotificationRecord();
    nr1.setRecipientName(recipientName);
    nr1.setRecipientRole("Requester");
    nr1.setChannel("email");
    nr1.setSentAt(now);
    nr1.setStatus("SENT");
    notifications.add(nr1);
    NotificationRecord nr2 = new NotificationRecord();
    nr2.setRecipientName("Markus Schmidt");
    nr2.setRecipientRole("Supervisor");
    nr2.setChannel("email");
    nr2.setSentAt(now);
    nr2.setStatus("SENT");
    notifications.add(nr2);
    NotificationRecord nr3 = new NotificationRecord();
    nr3.setRecipientName("QM Manager");
    nr3.setRecipientRole("Quality Manager");
    nr3.setChannel("email");
    nr3.setSentAt(now);
    nr3.setStatus("SENT");
    notifications.add(nr3);

    AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(now);
    entry.setActor(isWithdrawal ? OnboardingRequestUtils.requesterName(req) : "Supplier Agent");
    entry.setActorType(isWithdrawal ? AuditActorType.USER : AuditActorType.AGENT);
    entry.setEntryType(AuditEntryType.DECLINE);
    entry.setAction(isWithdrawal
        ? "Request withdrawn by requester"
        : "Automatic decline: risk score " + agg + "/100 (" + lvl + ") - below threshold 40");
    entry.setDeclineReasons(reasons);

    return new DeclineOrchestrationResult(summary, "Supplier Onboarding Declined - " + name, notifications, entry);
  }
}
