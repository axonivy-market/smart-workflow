package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service;

import java.time.Instant;

import com.axonivy.utils.smart.workflow.demo.common.Address;
import com.axonivy.utils.smart.workflow.demo.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.audit.AuditTrailEntry;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AuditActorType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AuditEntryType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.OnboardingStatus;

public class OnboardingRequestService {

  public record CompletionContext(
      String caseName,
      String summary,
      AuditTrailEntry auditEntry) {}

  private OnboardingRequestService() {}

  public static void initRequest(OnboardingRequest req) {
    if (req.getSupplier() == null) {
      req.setSupplier(new Supplier());
    }
    if (req.getSupplier().getBusinessAddress() == null) {
      req.getSupplier().setBusinessAddress(new Address());
    }
    req.setStatus(OnboardingStatus.REQUEST);
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

  public static CompletionContext completeRequest(OnboardingRequest req) {
    String now = Instant.now().toString();
    String name = OnboardingRequestUtils.supplierName(req);

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
}
