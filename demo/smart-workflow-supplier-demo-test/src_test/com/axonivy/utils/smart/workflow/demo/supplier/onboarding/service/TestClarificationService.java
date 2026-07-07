package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.SupplierRiskScore;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AuditEntryType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.FindingSeverity;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.OnboardingStatus;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskKind;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskLevel;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service.ClarificationService.ClarificationCycleResult;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service.ClarificationService.ClarificationRetryResult;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestClarificationService {

  @Test
  void processRetry_incrementsCountAndSetsActorFromRequest() {
    OnboardingRequest req = new OnboardingRequest();
    req.setRequestedBy("Jane Doe");

    ClarificationRetryResult result = ClarificationService.processRetry(req, 1, "Additional notes", false);

    assertThat(result.newCount()).isEqualTo(2);
    assertThat(result.auditEntry().getActor()).isEqualTo("Jane Doe");
    assertThat(result.auditEntry().getEntryType()).isEqualTo(AuditEntryType.CLARIFICATION_SUBMITTED);
    assertThat(result.auditEntry().getTechnicalDetail()).isEqualTo("Additional notes");
  }

  @Test
  void processRetry_resolvedItems_classifiedByType() {
    OnboardingRequest req = new OnboardingRequest();
    ValidationFinding docFinding = new ValidationFinding();
    docFinding.setResolved(true);
    docFinding.setDocumentTypeKey("CERTIFICATION:ISO_9001");
    docFinding.setMessage("ISO cert missing");

    ValidationFinding nonDocFinding = new ValidationFinding();
    nonDocFinding.setResolved(true);
    nonDocFinding.setRiskKind(RiskKind.AI_VALIDATION);
    nonDocFinding.setMessage("VAT format issue");

    ValidationFinding unresolvedFinding = new ValidationFinding();
    unresolvedFinding.setResolved(false);
    unresolvedFinding.setMessage("Some issue");

    req.setPolicyValidationFindings(List.of(docFinding, nonDocFinding, unresolvedFinding));

    ClarificationRetryResult result = ClarificationService.processRetry(req, 1, null, false);

    assertThat(result.auditEntry().getResolvedItems()).hasSize(2);
    assertThat(result.auditEntry().getResolvedItems().get(0).getResolutionType()).isEqualTo("Document uploaded");
    assertThat(result.auditEntry().getResolvedItems().get(1).getResolutionType()).isEqualTo("Explanation provided");
  }

  @Test
  void startClarificationCycle_setsStatusAndAddsAuditEntry() {
    OnboardingRequest req = new OnboardingRequest();
    req.setRequestedBy("Jane Doe");

    ClarificationCycleResult result = ClarificationService.startClarificationCycle(
        req, 0, "Some notes", false, null);

    assertThat(req.getStatus()).isEqualTo(OnboardingStatus.CLARIFICATION_REQUIRED);
    assertThat(req.getAuditTrail()).hasSize(1);
    assertThat(req.getAuditTrail().get(0).getEntryType()).isEqualTo(AuditEntryType.CLARIFICATION_SUBMITTED);
    assertThat(result.newCount()).isEqualTo(1);
    assertThat(result.summary()).contains("1");
    assertThat(result.routingDecision()).isNull();
  }

  @Test
  void startClarificationCycle_whenNullCount_treatsAsZero() {
    OnboardingRequest req = new OnboardingRequest();

    ClarificationCycleResult result = ClarificationService.startClarificationCycle(
        req, null, null, false, null);

    assertThat(result.newCount()).isEqualTo(1);
  }

  @Test
  void startClarificationCycle_whenAllFindingsResolved_overridesScoreAndRoutesToApproval() {
    OnboardingRequest req = new OnboardingRequest();
    ValidationFinding resolved = new ValidationFinding();
    resolved.setSeverity(FindingSeverity.WARNING);
    resolved.setResolved(true);
    req.setPolicyValidationFindings(new ArrayList<>(List.of(resolved)));

    SupplierAgentResponse agentResponse = new SupplierAgentResponse();
    agentResponse.setRiskScore(new SupplierRiskScore());

    ClarificationCycleResult result = ClarificationService.startClarificationCycle(
        req, 0, null, false, agentResponse);

    assertThat(result.routingDecision()).isEqualTo("APPROVAL");
    assertThat(agentResponse.getRiskScore().getAggregate()).isEqualTo(100);
    assertThat(agentResponse.getRiskScore().getLevel()).isEqualTo(RiskLevel.GREEN);
  }

  @Test
  void startClarificationCycle_whenFindingUnresolved_doesNotRouteToApproval() {
    OnboardingRequest req = new OnboardingRequest();
    ValidationFinding unresolved = new ValidationFinding();
    unresolved.setSeverity(FindingSeverity.FAILURE);
    unresolved.setResolved(false);
    req.setPolicyValidationFindings(new ArrayList<>(List.of(unresolved)));

    ClarificationCycleResult result = ClarificationService.startClarificationCycle(
        req, 0, null, false, null);

    assertThat(result.routingDecision()).isNull();
  }
}
