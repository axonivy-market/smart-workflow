package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.SupplierRiskScore;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.audit.AuditTrailEntry;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder.OnboardingRequestSummaryBuilder;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AuditActorType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AuditEntryType;

public class OnboardingAuditEntryFactory {

  private static final String ACTOR_QM_ISM = "QM/ISM Manager";
  private static final String ACTOR_VALIDATION_AGENT = "Supplier Validation Agent";
  private static final String UNKNOWN = "UNKNOWN";

  private static final String ACTION_REQUEST_SUBMITTED = "Supplier onboarding request submitted";
  private static final String ACTION_QM_ISM_FORMAT = "QM/ISM clarification assistance provided - cycle %d";
  private static final String ACTION_DUPLICATE_CHECK_FORMAT = "Duplicate check complete - %d match(es) found";
  private static final String ACTION_DUPLICATE_PROCEED_FORMAT = "Duplicate review: proceeding with existing supplier '%s'";
  private static final String ACTION_DUPLICATE_REGISTER_FORMAT = "Duplicate review: registering '%s' as new supplier";
  private static final String ACTION_REGISTRATION_FORMAT = "Supplier registration details captured - %s";
  private static final String ACTION_AI_ANALYSIS_FORMAT = "AI analysis complete - Risk: %d/100 (%s) -> %s";

  private OnboardingAuditEntryFactory() {}

  public static AuditTrailEntry buildRequestAuditEntry(OnboardingRequest req) {
    AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(Instant.now().toString());
    entry.setActor(OnboardingRequestUtils.requesterName(req));
    entry.setActorType(AuditActorType.USER);
    entry.setEntryType(AuditEntryType.REQUEST_SUBMITTED);
    entry.setAction(ACTION_REQUEST_SUBMITTED);
    entry.setRequestSummaryLines(req != null ? OnboardingRequestSummaryBuilder.build(req) : new ArrayList<>());
    return entry;
  }

  public static AuditTrailEntry buildQmIsmAuditEntry(int cycle, String notes) {
    String detail = notes != null ? notes : "";
    AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(Instant.now().toString());
    entry.setActor(ACTOR_QM_ISM);
    entry.setActorType(AuditActorType.USER);
    entry.setEntryType(AuditEntryType.QM_ASSISTANCE);
    entry.setAction(String.format(ACTION_QM_ISM_FORMAT, cycle));
    entry.setTechnicalDetail(detail.length() > 200 ? detail.substring(0, 200) : detail);
    return entry;
  }

  public static AuditTrailEntry buildDuplicateCheckAuditEntry(OnboardingRequest req, SupplierAgentResponse resp) {
    int count = (req != null && req.getMatchedSuppliers() != null) ? req.getMatchedSuppliers().size() : 0;
    List<String> names = new ArrayList<>();
    if (req != null && req.getMatchedSuppliers() != null) {
      for (com.axonivy.utils.smart.workflow.demo.supplier.Supplier s : req.getMatchedSuppliers()) {
        if (s.getBusinessName() != null) names.add(s.getBusinessName());
      }
    }
    AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(Instant.now().toString());
    entry.setActor(ACTOR_VALIDATION_AGENT);
    entry.setActorType(AuditActorType.AGENT);
    entry.setEntryType(AuditEntryType.DUPLICATE_CHECK);
    entry.setAction(String.format(ACTION_DUPLICATE_CHECK_FORMAT, count));
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
        ? String.format(ACTION_DUPLICATE_PROCEED_FORMAT, name)
        : String.format(ACTION_DUPLICATE_REGISTER_FORMAT, name));
    return entry;
  }

  public static AuditTrailEntry buildRegistrationAuditEntry(OnboardingRequest req) {
    AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(Instant.now().toString());
    entry.setActor(OnboardingRequestUtils.requesterName(req));
    entry.setActorType(AuditActorType.USER);
    entry.setEntryType(AuditEntryType.REGISTRATION_CAPTURED);
    entry.setAction(String.format(ACTION_REGISTRATION_FORMAT, OnboardingRequestUtils.supplierName(req)));
    entry.setRequestSummaryLines(req != null ? OnboardingRequestSummaryBuilder.build(req) : new ArrayList<>());
    return entry;
  }

  public static AuditTrailEntry buildAgentAnalysisAuditEntry(OnboardingRequest req, SupplierAgentResponse resp) {
    String now = Instant.now().toString();
    int agg = Optional.of(resp)
      .map(SupplierAgentResponse::getRiskScore)
      .map(SupplierRiskScore::getAggregate)
      .orElse(0);
    String lvl = (resp != null && resp.getRiskScore() != null
        && resp.getRiskScore().getLevel() != null)
        ? resp.getRiskScore().getLevel().name() : UNKNOWN;
    String routing = (resp != null && resp.getRoutingDecision() != null)
        ? resp.getRoutingDecision().toUpperCase() : UNKNOWN;

    AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(now);
    entry.setActor(ACTOR_VALIDATION_AGENT);
    entry.setActorType(AuditActorType.AGENT);
    entry.setEntryType(AuditEntryType.AI_ANALYSIS);
    entry.setAction(String.format(ACTION_AI_ANALYSIS_FORMAT, agg, lvl, routing));
    if (req != null && req.getPolicyValidationFindings() != null) {
      entry.setFindings(new ArrayList<>(req.getPolicyValidationFindings()));
    }
    return entry;
  }
}
