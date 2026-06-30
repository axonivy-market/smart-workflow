package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.SupplierRiskScore;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.audit.AuditTrailEntry;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AuditActorType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AuditEntryType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskLevel;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestOnboardingAuditEntryFactory {

  @Test
  void buildRequestAuditEntry_setsRequiredFields() {
    OnboardingRequest req = new OnboardingRequest();
    req.setRequestedBy("John Doe");

    AuditTrailEntry entry = OnboardingAuditEntryFactory.buildRequestAuditEntry(req);

    assertThat(entry.getActor()).isEqualTo("John Doe");
    assertThat(entry.getActorType()).isEqualTo(AuditActorType.USER);
    assertThat(entry.getEntryType()).isEqualTo(AuditEntryType.REQUEST_SUBMITTED);
    assertThat(entry.getAction()).isEqualTo("Supplier onboarding request submitted");
    assertThat(entry.getTimestamp()).isNotBlank();
  }

  @Test
  void buildQmIsmAuditEntry_setsFields() {
    AuditTrailEntry entry = OnboardingAuditEntryFactory.buildQmIsmAuditEntry(2, "Please clarify VAT.");

    assertThat(entry.getActorType()).isEqualTo(AuditActorType.USER);
    assertThat(entry.getEntryType()).isEqualTo(AuditEntryType.QM_ASSISTANCE);
    assertThat(entry.getAction()).contains("cycle 2");
    assertThat(entry.getTechnicalDetail()).isEqualTo("Please clarify VAT.");
  }

  @Test
  void buildDuplicateCheckAuditEntry_showsMatchCount() {
    OnboardingRequest req = new OnboardingRequest();
    AuditTrailEntry noMatch = OnboardingAuditEntryFactory.buildDuplicateCheckAuditEntry(req, null);
    assertThat(noMatch.getAction()).contains("0 match(es)");
    assertThat(noMatch.getActorType()).isEqualTo(AuditActorType.AGENT);
    assertThat(noMatch.getEntryType()).isEqualTo(AuditEntryType.DUPLICATE_CHECK);

    Supplier s1 = new Supplier();
    s1.setBusinessName("ACME Corp");
    Supplier s2 = new Supplier();
    s2.setBusinessName("ACME Ltd");
    req.setMatchedSuppliers(List.of(s1, s2));
    AuditTrailEntry withMatches = OnboardingAuditEntryFactory.buildDuplicateCheckAuditEntry(req, null);
    assertThat(withMatches.getAction()).contains("2 match(es)");
    assertThat(withMatches.getMatchedSupplierNames()).containsExactly("ACME Corp", "ACME Ltd");
  }

  @Test
  void buildDuplicateDecisionAuditEntry_actionDependsOnDecision() {
    OnboardingRequest req = new OnboardingRequest();
    Supplier supplier = new Supplier();
    supplier.setBusinessName("ACME Corp");
    req.setSupplier(supplier);

    AuditTrailEntry proceed = OnboardingAuditEntryFactory.buildDuplicateDecisionAuditEntry(req, true);
    assertThat(proceed.getAction()).contains("proceeding with existing supplier").contains("ACME Corp");
    assertThat(proceed.getEntryType()).isEqualTo(AuditEntryType.DUPLICATE_DECISION);

    supplier.setBusinessName("New Supplier GmbH");
    AuditTrailEntry register = OnboardingAuditEntryFactory.buildDuplicateDecisionAuditEntry(req, false);
    assertThat(register.getAction()).contains("registering").contains("New Supplier GmbH");
  }

  @Test
  void buildRegistrationAuditEntry_setsFields() {
    OnboardingRequest req = new OnboardingRequest();
    req.setRequestedBy("Jane Smith");
    Supplier supplier = new Supplier();
    supplier.setBusinessName("Test Supplier");
    req.setSupplier(supplier);

    AuditTrailEntry entry = OnboardingAuditEntryFactory.buildRegistrationAuditEntry(req);

    assertThat(entry.getActor()).isEqualTo("Jane Smith");
    assertThat(entry.getEntryType()).isEqualTo(AuditEntryType.REGISTRATION_CAPTURED);
    assertThat(entry.getAction()).contains("Test Supplier");
  }

  @Test
  void buildAgentAnalysisAuditEntry_setsRiskScoreAndRouting() {
    OnboardingRequest req = new OnboardingRequest();

    SupplierRiskScore riskScore = new SupplierRiskScore();
    riskScore.setAggregate(72);
    riskScore.setLevel(RiskLevel.YELLOW);

    SupplierAgentResponse resp = new SupplierAgentResponse();
    resp.setRiskScore(riskScore);
    resp.setRoutingDecision("clarification");

    AuditTrailEntry entry = OnboardingAuditEntryFactory.buildAgentAnalysisAuditEntry(req, resp);

    assertThat(entry.getActorType()).isEqualTo(AuditActorType.AGENT);
    assertThat(entry.getEntryType()).isEqualTo(AuditEntryType.AI_ANALYSIS);
    assertThat(entry.getAction()).contains("72").contains("YELLOW").contains("CLARIFICATION");
  }
}
