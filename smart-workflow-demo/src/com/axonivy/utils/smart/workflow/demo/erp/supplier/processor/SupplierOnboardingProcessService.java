package com.axonivy.utils.smart.workflow.demo.erp.supplier.processor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.erp.shared.Address;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AuditActorType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AuditTrailEntry;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ClarificationItem;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ClarificationProblemType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ClarificationRecord;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.DeclineRecord;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.NotificationRecord;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingStatus;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ResolvedClarificationItem;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ValidationFinding;

public class SupplierOnboardingProcessService {

  public record ClarificationContextResult(
      List<ClarificationItem> clarificationItems,
      int cycleDisplay,
      String caseName,
      AuditTrailEntry auditEntry) {}

  public record ClarificationRetryResult(
      int newCount,
      String summary,
      ClarificationRecord clarificationRecord,
      AuditTrailEntry auditEntry) {}

  public record DeclineOrchestrationResult(
      String summary,
      String caseName,
      DeclineRecord declineRecord,
      List<NotificationRecord> notificationRecords,
      AuditTrailEntry auditEntry) {}

  public record CompletionContext(
      String caseName,
      String summary,
      AuditTrailEntry auditEntry) {}

  private SupplierOnboardingProcessService() {}

  public static void initRequest(OnboardingRequest req) {
    if (req.getSupplier() == null) {
      req.setSupplier(new Supplier());
    }
    if (req.getSupplier().getBusinessAddress() == null) {
      req.getSupplier().setBusinessAddress(new Address());
    }
    req.setStatus(OnboardingStatus.REQUEST);
  }

  public static String suggestedSupplierCaseName(OnboardingRequest req) {
    String name = supplierName(req);
    return "Supplier Onboarding - " + name + " (existing)";
  }

  public static ClarificationContextResult buildClarificationContext(
      OnboardingRequest req, SupplierAgentResponse resp,
      int retryCount, String routingDecision) {

    List<ClarificationItem> items = new ArrayList<>();
    if (resp != null && resp.getValidationFindings() != null) {
      int count = 0;
      for (ValidationFinding f : resp.getValidationFindings()) {
        if (f.getSeverity() != null) {
          String sev = f.getSeverity().toUpperCase();
          if ("FAILURE".equals(sev) || "WARNING".equals(sev)
              || "INSUFFICIENT".equals(sev) || "CLARIFICATION_NEEDED".equals(sev)) {
            List<ClarificationItem> expanded = expandFinding(f);
            for (ClarificationItem ci : expanded) {
              items.add(ci);
              if (++count >= 10) break;
            }
            if (count >= 10) break;
          }
        }
      }
    }
    if (items.isEmpty()) {
      items.add(new ClarificationItem(
          "Additional supplier information required — please check the agent findings.",
          ClarificationProblemType.OTHER, null));
    }

    int cycle = retryCount + 1;
    String name = supplierName(req);
    String caseName = "Supplier Onboarding Clarification - Cycle " + cycle + " - " + name;

    int agg = (resp != null && resp.getRiskScore() != null) ? resp.getRiskScore().getAggregate() : 0;
    String lvl = (resp != null && resp.getRiskScore() != null && resp.getRiskScore().getLevel() != null)
        ? resp.getRiskScore().getLevel().name() : "YELLOW";
    String requester = requesterName(req);

    AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(Instant.now().toString());
    entry.setActor(requester);
    entry.setActorType(AuditActorType.USER);
    entry.setAction("Clarification required — cycle " + cycle + ", risk score " + agg + "/100 (" + lvl + ")");
    entry.setTechnicalDetail(routingDecision);

    return new ClarificationContextResult(items, cycle, caseName, entry);
  }

  public static AuditTrailEntry buildQmIsmAuditEntry(int cycle, String notes) {
    String detail = notes != null ? notes : "";
    AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(Instant.now().toString());
    entry.setActor("QM/ISM Manager");
    entry.setActorType(AuditActorType.USER);
    entry.setAction("QM/ISM clarification assistance provided — cycle " + cycle);
    entry.setTechnicalDetail(detail.length() > 200 ? detail.substring(0, 200) : detail);
    return entry;
  }

  public static ClarificationRetryResult processRetry(
      OnboardingRequest req, int currentCount, String lastNotes,
      boolean escalated, List<ClarificationItem> clarificationItems) {

    int newCount = currentCount + 1;
    String submitter = requesterName(req);
    String now = Instant.now().toString();
    String notes = lastNotes != null ? lastNotes : "";

    ClarificationRecord cr = new ClarificationRecord();
    cr.setCycle(newCount);
    cr.setSubmittedAt(now);
    cr.setSubmittedBy(submitter);
    cr.setSubmittedByRole(escalated ? "QM/ISM Manager" : "Requester");
    cr.setAdditionalNotes(lastNotes);
    cr.setEscalated(escalated);
    if (clarificationItems != null) {
      List<String> addressed = new ArrayList<>();
      for (ClarificationItem ci : clarificationItems) {
        if (ci.isResolved()) {
          addressed.add(ci.getMessage());
        }
      }
      cr.setItemsAddressed(addressed);
    }

    List<ResolvedClarificationItem> resolved = new ArrayList<>();
    if (clarificationItems != null) {
      for (ClarificationItem ci : clarificationItems) {
        if (ci.isResolved()) {
          String resolutionType = ci.getProblemType() == ClarificationProblemType.DOCUMENT
              ? "Document uploaded" : "Explanation provided";
          resolved.add(new ResolvedClarificationItem(ci.getMessage(), resolutionType, ci.getExplanation()));
        }
      }
    }

    AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(now);
    entry.setActor(submitter);
    entry.setActorType(AuditActorType.USER);
    entry.setAction("Clarification cycle " + newCount + " submitted — re-evaluating");
    entry.setTechnicalDetail(notes.length() > 200 ? notes.substring(0, 200) : notes);
    if (!resolved.isEmpty()) {
      entry.setResolvedItems(resolved);
    }

    return new ClarificationRetryResult(newCount, "Clarification cycle " + newCount + " completed.", cr, entry);
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
          if ("FAILURE".equalsIgnoreCase(f.getSeverity())) {
            sb.append(" ").append(f.getMessage()).append(";");
            if (++count >= 5) {
              break;
            }
          }
        }
      }
      summary = sb.toString();
    }

    int agg = (resp != null && resp.getRiskScore() != null) ? resp.getRiskScore().getAggregate() : 0;
    String lvl = (resp != null && resp.getRiskScore() != null && resp.getRiskScore().getLevel() != null)
        ? resp.getRiskScore().getLevel().name() : "RED";

    List<String> reasons = new ArrayList<>();
    if (resp != null && resp.getValidationFindings() != null) {
      for (ValidationFinding f : resp.getValidationFindings()) {
        if ("FAILURE".equalsIgnoreCase(f.getSeverity())) {
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

    DeclineRecord dr = new DeclineRecord();
    dr.setDeclinedAt(now);
    dr.setDeclinedBy(isWithdrawal ? requesterName(req) : "Supplier Agent");
    dr.setRiskScoreAggregate(agg);
    dr.setRiskScoreLevel(lvl);
    dr.setDeclineReasons(reasons);

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
    entry.setAction(isWithdrawal
        ? "Request withdrawn by requester"
        : "Automatic decline: risk score " + agg + "/100 (" + lvl + ") — below threshold 40");
    entry.setTechnicalDetail("DECLINE");

    return new DeclineOrchestrationResult(summary, "Supplier Onboarding Declined - " + name, dr, notifications, entry);
  }

  public static CompletionContext completeRequest(OnboardingRequest req, String caseId) {
    String now = Instant.now().toString();
    String name = supplierName(req);

    req.setStatus(OnboardingStatus.COMPLETED);
    req.setCompletedAt(now);
    if (req.getProcessDuration() == null || req.getProcessDuration().isBlank()) {
      req.setProcessDuration("Completed via green approval path");
    }
    if (req.getCaseReference() == null || req.getCaseReference().isBlank()) {
      req.setCaseReference("SO-" + caseId);
    }

    AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(now);
    entry.setActor("Supplier Agent");
    entry.setActorType(AuditActorType.SYSTEM);
    entry.setAction("Supplier onboarding completed for: " + name);
    entry.setTechnicalDetail("COMPLETED");

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

  /**
   * Expands a finding into one or more ClarificationItems.
   * Combined "all certificates missing" findings are split into one item per CertificationType.
   */
  private static List<ClarificationItem> expandFinding(ValidationFinding f) {
    List<ClarificationItem> result = new ArrayList<>();
    String msg = f.getMessage() != null ? f.getMessage() : "";
    String msgLower = msg.toLowerCase();

    // Already has a specific documentTypeKey from the AI — use it directly
    if (f.getDocumentTypeKey() != null && !f.getDocumentTypeKey().isBlank()) {
      result.add(new ClarificationItem(msg, ClarificationProblemType.DOCUMENT, f.getDocumentTypeKey(), f));
      return result;
    }

    // Combined "all certificates missing" finding — expand to one item per cert type
    boolean isCombinedCertFinding =
        (msgLower.contains("all cert") || msgLower.contains("certificates are missing") ||
         msgLower.contains("no cert") || msgLower.contains("none of the cert"))
        && (msgLower.contains("missing") || msgLower.contains("not upload") || msgLower.contains("unavailable"));
    if (isCombinedCertFinding) {
      for (LegalDocumentType ct : LegalDocumentType.certificationValues()) {
        String itemMsg = ct.getLabel() + " certificate is missing and thus no expiry date can be checked; considered missing.";
        result.add(new ClarificationItem(itemMsg, ClarificationProblemType.DOCUMENT, ct.getDocumentTypeKey(), f));
      }
      return result;
    }

    // Single cert mention — map to its specific documentTypeKey
    for (LegalDocumentType ct : LegalDocumentType.certificationValues()) {
      if (msgLower.contains(ct.name().toLowerCase().replace('_', ' '))
          || msgLower.contains(ct.getLabel().toLowerCase())) {
        result.add(new ClarificationItem(msg, ClarificationProblemType.DOCUMENT, ct.getDocumentTypeKey(), f));
        return result;
      }
    }

    // Fall through to original classifyFinding logic
    result.add(classifyFinding(f));
    return result;
  }

  /**
   * Classifies a ValidationFinding into a ClarificationItem with the appropriate
   * problem type and documentTypeKey.
   */
  private static ClarificationItem classifyFinding(ValidationFinding f) {
    String msg = f.getMessage() != null ? f.getMessage() : "";
    String src = f.getSource() != null ? f.getSource().toLowerCase() : "";

    // 1. Use AI-provided documentTypeKey if present
    if (f.getDocumentTypeKey() != null && !f.getDocumentTypeKey().isBlank()) {
      return new ClarificationItem(msg, ClarificationProblemType.DOCUMENT, f.getDocumentTypeKey(), f);
    }

    // 2. Duplicate check source → DUPLICATE
    if (src.contains("duplicate") || src.contains("erp")) {
      return new ClarificationItem(msg, ClarificationProblemType.DUPLICATE, null, f);
    }

    // 3. Generic keyword fallback (used only when AI classification is unavailable)
    String msgLower = msg.toLowerCase();
    if (msgLower.contains("duplicate")) {
      return new ClarificationItem(msg, ClarificationProblemType.DUPLICATE, null, f);
    }
    if (msgLower.contains("certif") || msgLower.contains("document") || msgLower.contains("upload")) {
      return new ClarificationItem(msg, ClarificationProblemType.DOCUMENT, null, f);
    }

    // 4. Default → OTHER
    return new ClarificationItem(msg, ClarificationProblemType.OTHER, null, f);
  }
}
