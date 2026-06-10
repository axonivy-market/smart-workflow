package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AuditTrailEntry;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.builder.OnboardingRequestSummaryBuilder;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.AuditActorType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.AuditEntryType;

public class OnboardingAuditEntryFactory {

  private OnboardingAuditEntryFactory() {}

  public static AuditTrailEntry buildRequestAuditEntry(OnboardingRequest req) {
    AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(Instant.now().toString());
    entry.setActor(OnboardingRequestUtils.requesterName(req));
    entry.setActorType(AuditActorType.USER);
    entry.setEntryType(AuditEntryType.REQUEST_SUBMITTED);
    entry.setAction("Supplier onboarding request submitted");
    entry.setRequestSummaryLines(req != null ? OnboardingRequestSummaryBuilder.build(req) : new ArrayList<>());
    return entry;
  }

  public static AuditTrailEntry buildQmIsmAuditEntry(int cycle, String notes) {
    String detail = notes != null ? notes : "";
    AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(Instant.now().toString());
    entry.setActor("QM/ISM Manager");
    entry.setActorType(AuditActorType.USER);
    entry.setEntryType(AuditEntryType.QM_ASSISTANCE);
    entry.setAction("QM/ISM clarification assistance provided \u2014 cycle " + cycle);
    entry.setTechnicalDetail(detail.length() > 200 ? detail.substring(0, 200) : detail);
    return entry;
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
    String name = OnboardingRequestUtils.supplierName(req);
    AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(Instant.now().toString());
    entry.setActor(OnboardingRequestUtils.requesterName(req));
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
    entry.setActor(OnboardingRequestUtils.requesterName(req));
    entry.setActorType(AuditActorType.USER);
    entry.setEntryType(AuditEntryType.REGISTRATION_CAPTURED);
    entry.setAction("Supplier registration details captured \u2014 " + OnboardingRequestUtils.supplierName(req));
    entry.setRequestSummaryLines(req != null ? OnboardingRequestSummaryBuilder.build(req) : new ArrayList<>());
    return entry;
  }

  public static AuditTrailEntry buildAgentAnalysisAuditEntry(OnboardingRequest req, SupplierAgentResponse resp) {
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
    entry.setAction("AI analysis complete \u2014 Risk: " + agg + "/100 (" + lvl + ") \u2192 " + routing);
    if (req != null && req.getPolicyValidationFindings() != null) {
      entry.setFindings(new ArrayList<>(req.getPolicyValidationFindings()));
    }
    return entry;
  }
}
