package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.SupplierRiskScore;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.audit.AuditTrailEntry;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.audit.ResolvedClarificationItem;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder.ClarificationProblemTypeBuilder;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AuditActorType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AuditEntryType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.ClarificationProblemType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.FindingSeverity;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.OnboardingStatus;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskLevel;

public class ClarificationService {

  public record ClarificationRetryResult(
      int newCount,
      String summary,
      AuditTrailEntry auditEntry) {}

  public record ClarificationCycleResult(
      int newCount,
      String summary,
      String routingDecision) {}

  private ClarificationService() {}

  public static ClarificationCycleResult startClarificationCycle(
      OnboardingRequest request, Integer currentCount, String lastNotes,
      boolean escalated, SupplierAgentResponse agentResponse) {

    request.setStatus(OnboardingStatus.CLARIFICATION_REQUIRED);

    var effectiveCount = currentCount != null ? currentCount : 0;
    ClarificationRetryResult retry = processRetry(request, effectiveCount, lastNotes, escalated);

    proceedAuditTrail(request, retry);

    String routingDecision = null;
    if (areAllFindingsResolved(request)) {
      overrideRiskScoreToGreen(agentResponse);
      routingDecision = "APPROVAL";
    }

    return new ClarificationCycleResult(retry.newCount(), retry.summary(), routingDecision);
  }

  private static void proceedAuditTrail(OnboardingRequest request, ClarificationRetryResult retry) {
    if (request.getAuditTrail() == null) {
        request.setAuditTrail(new ArrayList<>());
    }
    request.getAuditTrail().add(retry.auditEntry());
  }

  private static boolean areAllFindingsResolved(OnboardingRequest request) {
    List<ValidationFinding> findings = request != null ? request.getPolicyValidationFindings() : null;
    if (findings == null || findings.isEmpty()) {
      return false;
    }
    for (ValidationFinding f : findings) {
      if (f.getSeverity() != FindingSeverity.PASSED && !Boolean.TRUE.equals(f.getResolved())) {
        return false;
      }
    }
    return true;
  }

  private static void overrideRiskScoreToGreen(SupplierAgentResponse agentResponse) {
    if (agentResponse == null || agentResponse.getRiskScore() == null) {
      return;
    }
    SupplierRiskScore score = agentResponse.getRiskScore();
    score.setFinancialStability(100);
    score.setPolicyCompliance(100);
    score.setCertValidity(100);
    score.setAggregate(100);
    score.setLevel(RiskLevel.GREEN);
  }

  public static ClarificationRetryResult processRetry(
      OnboardingRequest req, int currentCount, String lastNotes, boolean escalated) {

    int newCount = currentCount + 1;
    String submitter = OnboardingRequestUtils.requesterName(req);
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
    entry.setAction("Clarification cycle " + newCount + " submitted - re-evaluating");
    entry.setTechnicalDetail(notes.length() > 200 ? notes.substring(0, 200) : notes);
    if (!resolved.isEmpty()) {
      entry.setResolvedItems(resolved);
    }

    return new ClarificationRetryResult(newCount, "Clarification cycle " + newCount + " completed.", entry);
  }
}
