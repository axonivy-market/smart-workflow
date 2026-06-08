package com.axonivy.utils.smart.workflow.demo.erp.supplier.processor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.axonivy.utils.smart.workflow.demo.erp.shared.Address;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AuditTrailEntry;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.NotificationRecord;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ResolvedClarificationItem;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.builder.ClarificationProblemTypeBuilder;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.builder.OnboardingRequestSummaryBuilder;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.AuditActorType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.AuditEntryType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.ClarificationProblemType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.FindingSeverity;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.OnboardingStatus;

public class SupplierOnboardingProcessService {

  public record ClarificationRetryResult(
      int newCount,
      String summary,
      AuditTrailEntry auditEntry) {}

  public record DeclineOrchestrationResult(
      String summary,
      String caseName,
      List<NotificationRecord> notificationRecords,
      AuditTrailEntry auditEntry) {}

  public record CompletionContext(
      String caseName,
      String summary,
      AuditTrailEntry auditEntry) {}

  private SupplierOnboardingProcessService() {}

  public static AuditTrailEntry buildRequestAuditEntry(OnboardingRequest req) {
    AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(Instant.now().toString());
    entry.setActor(requesterName(req));
    entry.setActorType(AuditActorType.USER);
    entry.setEntryType(AuditEntryType.REQUEST_SUBMITTED);
    entry.setAction("Supplier onboarding request submitted");
    entry.setRequestSummaryLines(req != null ? OnboardingRequestSummaryBuilder.build(req) : new ArrayList<>());
    return entry;
  }

  public static void initRequest(OnboardingRequest req) {
    if (req.getSupplier() == null) {
      req.setSupplier(new Supplier());
    }
    if (req.getSupplier().getBusinessAddress() == null) {
      req.getSupplier().setBusinessAddress(new Address());
    }
    req.setStatus(OnboardingStatus.REQUEST);
  }

  public static AuditTrailEntry buildQmIsmAuditEntry(int cycle, String notes) {
    String detail = notes != null ? notes : "";
    AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(Instant.now().toString());
    entry.setActor("QM/ISM Manager");
    entry.setActorType(AuditActorType.USER);
    entry.setEntryType(AuditEntryType.QM_ASSISTANCE);
    entry.setAction("QM/ISM clarification assistance provided — cycle " + cycle);
    entry.setTechnicalDetail(detail.length() > 200 ? detail.substring(0, 200) : detail);
    return entry;
  }

  public static ClarificationRetryResult processRetry(
      OnboardingRequest req, int currentCount, String lastNotes, boolean escalated) {

    int newCount = currentCount + 1;
    String submitter = requesterName(req);
    String now = Instant.now().toString();
    String notes = lastNotes != null ? lastNotes : "";

    List<ResolvedClarificationItem> resolved = new ArrayList<>();
    List<ValidationFinding> findings = req != null ? req.getPolicyValidationFindings() : null;
    if (findings != null) {
      for (ValidationFinding f : findings) {
        if (Boolean.TRUE.equals(f.getResolved())) {
          String resolutionType = ClarificationProblemTypeBuilder.resolve(
                  f.getDocumentTypeKey(), f.getRiskKind(), f.getSource(), f.getMessage())
              == ClarificationProblemType.DOCUMENT
              ? "Document uploaded" : "Explanation provided";
          ResolvedClarificationItem rci = new ResolvedClarificationItem();
          rci.setProblem(f.getMessage());
          rci.setResolutionType(resolutionType);
          rci.setUserExplanation(f.getUserExplanation());
          resolved.add(rci);
        }
      }
    }

    AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(now);
    entry.setActor(submitter);
    entry.setActorType(AuditActorType.USER);
    entry.setEntryType(AuditEntryType.CLARIFICATION_SUBMITTED);
    entry.setAction("Clarification cycle " + newCount + " submitted — re-evaluating");
    entry.setTechnicalDetail(notes.length() > 200 ? notes.substring(0, 200) : notes);
    if (!resolved.isEmpty()) {
      entry.setResolvedItems(resolved);
    }

    return new ClarificationRetryResult(newCount, "Clarification cycle " + newCount + " completed.", entry);
  }

  public static DeclineOrchestrationResult buildDecline(
      OnboardingRequest req, SupplierAgentResponse resp,
      boolean isWithdrawal, String requestedBy) {

    String name = supplierName(req);
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

    int agg = (resp != null && resp.getRiskScore() != null)
        ? Optional.ofNullable(resp.getRiskScore().getAggregate()).orElse(0) : 0;
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
    entry.setActor(isWithdrawal ? requesterName(req) : "Supplier Agent");
    entry.setActorType(isWithdrawal ? AuditActorType.USER : AuditActorType.AGENT);
    entry.setEntryType(AuditEntryType.DECLINE);
    entry.setAction(isWithdrawal
        ? "Request withdrawn by requester"
        : "Automatic decline: risk score " + agg + "/100 (" + lvl + ") — below threshold 40");
    entry.setDeclineReasons(reasons);

    return new DeclineOrchestrationResult(summary, "Supplier Onboarding Declined - " + name, notifications, entry);
  }

  public static AuditTrailEntry buildDuplicateCheckAuditEntry(OnboardingRequest req, SupplierAgentResponse resp) {
    int count = (req != null && req.getMatchedSuppliers() != null) ? req.getMatchedSuppliers().size() : 0;
    List<String> names = new ArrayList<>();
    if (req != null && req.getMatchedSuppliers() != null) {
      for (com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier s : req.getMatchedSuppliers()) {
        if (s.getBusinessName() != null) names.add(s.getBusinessName());
      }
    }
    AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(Instant.now().toString());
    entry.setActor("Supplier Validation Agent");
    entry.setActorType(AuditActorType.AGENT);
    entry.setEntryType(AuditEntryType.DUPLICATE_CHECK);
    entry.setAction("Duplicate check complete \u2014 " + count + " match(es) found");
    if (!names.isEmpty()) entry.setMatchedSupplierNames(names);
    return entry;
  }

  public static AuditTrailEntry buildDuplicateDecisionAuditEntry(OnboardingRequest req, boolean usedSuggested) {
    String name = supplierName(req);
    AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(Instant.now().toString());
    entry.setActor(requesterName(req));
    entry.setActorType(AuditActorType.USER);
    entry.setEntryType(AuditEntryType.DUPLICATE_DECISION);
    entry.setAction(usedSuggested
        ? "Duplicate review: proceeding with existing supplier \u2018" + name + "\u2019"
        : "Duplicate review: registering \u2018" + name + "\u2019 as new supplier");
    return entry;
  }

  public static AuditTrailEntry buildRegistrationAuditEntry(OnboardingRequest req) {
    AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(Instant.now().toString());
    entry.setActor(requesterName(req));
    entry.setActorType(AuditActorType.USER);
    entry.setEntryType(AuditEntryType.REGISTRATION_CAPTURED);
    entry.setAction("Supplier registration details captured \u2014 " + supplierName(req));
    entry.setRequestSummaryLines(req != null ? OnboardingRequestSummaryBuilder.build(req) : new ArrayList<>());
    return entry;
  }

  public static AuditTrailEntry buildAgentAnalysisAuditEntry(
      OnboardingRequest req, SupplierAgentResponse resp) {

    String now = Instant.now().toString();
    int agg = (resp != null && resp.getRiskScore() != null)
        ? Optional.ofNullable(resp.getRiskScore().getAggregate()).orElse(0) : 0;
    String lvl = (resp != null && resp.getRiskScore() != null
        && resp.getRiskScore().getLevel() != null)
        ? resp.getRiskScore().getLevel().name() : "UNKNOWN";
    String routing = (resp != null && resp.getRoutingDecision() != null)
        ? resp.getRoutingDecision().toUpperCase() : "UNKNOWN";

    AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(now);
    entry.setActor("Supplier Validation Agent");
    entry.setActorType(AuditActorType.AGENT);
    entry.setEntryType(AuditEntryType.AI_ANALYSIS);
    entry.setAction("AI analysis complete — Risk: " + agg + "/100 (" + lvl + ") → " + routing);
    if (req != null && req.getPolicyValidationFindings() != null) {
      entry.setFindings(new ArrayList<>(req.getPolicyValidationFindings()));
    }
    return entry;
  }

  public static CompletionContext completeRequest(OnboardingRequest req, String caseId) {
    String now = Instant.now().toString();
    String name = supplierName(req);

    req.setStatus(OnboardingStatus.COMPLETED);
    req.setCompletedAt(now);
    if (req.getProcessDuration() == null || req.getProcessDuration().isBlank()) {
      req.setProcessDuration("Completed via green approval path");
    }

    AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(now);
    entry.setActor("Supplier Agent");
    entry.setActorType(AuditActorType.SYSTEM);
    entry.setEntryType(AuditEntryType.COMPLETION);
    entry.setAction("Supplier onboarding completed for: " + name);

    return new CompletionContext(
        "Supplier Onboarding Completed - " + name,
        "Supplier onboarding completed: " + name,
        entry);
  }

  public static String applyRoutingState(OnboardingRequest req, SupplierAgentResponse resp, String routingDecision) {
    if (req != null && resp != null) {
      req.setRiskScore(resp.getRiskScore());
    }

    if ("APPROVAL".equalsIgnoreCase(routingDecision)) {
      if (req != null) req.setStatus(OnboardingStatus.APPROVAL_PENDING);
      return "Green path selected. Awaiting supervisor and QM/ISM approvals.";
    } else if ("CLARIFICATION".equalsIgnoreCase(routingDecision)) {
      if (req != null) req.setStatus(OnboardingStatus.CLARIFICATION_REQUIRED);
      return "Yellow path selected. Clarification required before re-evaluation.";
    } else if ("DECLINE".equalsIgnoreCase(routingDecision)) {
      if (req != null) req.setStatus(OnboardingStatus.DECLINED);
      return "Red path selected. Request prepared for decline.";
    } else {
      if (req != null) req.setStatus(OnboardingStatus.RISK_SCORING);
      return "Routing decision unavailable. Defaulting to risk scoring state.";
    }
  }

  public static String buildPostAgentCaseName(OnboardingRequest req, String routingDecision) {
    if (!"APPROVAL".equalsIgnoreCase(routingDecision)) {
      return null;
    }
    String supplierName = (req != null && req.getSupplier() != null
        && req.getSupplier().getBusinessName() != null
        && !req.getSupplier().getBusinessName().isBlank())
        ? req.getSupplier().getBusinessName() : "Supplier";
    return "Supplier Onboarding Pending Approval - " + supplierName;
  }

  public static <T> List<T> ensureAndAdd(List<T> list, T item) {
    if (list == null) {
      list = new ArrayList<>();
    }
    list.add(item);
    return list;
  }

  private static String supplierName(OnboardingRequest req) {
    if (req != null && req.getSupplier() != null
        && req.getSupplier().getBusinessName() != null
        && !req.getSupplier().getBusinessName().isBlank()) {
      return req.getSupplier().getBusinessName();
    }
    return "Supplier";
  }

  private static String requesterName(OnboardingRequest req) {
    if (req != null && req.getRequestedBy() != null && !req.getRequestedBy().isBlank()) {
      return req.getRequestedBy();
    }
    return "Requester";
  }
}
