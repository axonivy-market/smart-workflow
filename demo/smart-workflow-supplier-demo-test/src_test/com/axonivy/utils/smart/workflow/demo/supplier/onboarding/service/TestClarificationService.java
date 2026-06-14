package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AuditEntryType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskKind;
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
}
