package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AuditActorType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AuditEntryType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.FindingSeverity;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service.DeclineOrchestrationService.DeclineOrchestrationResult;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestDeclineOrchestrationService {

  @Test
  void buildDecline_withdrawal_summaryAndActorType() {
    DeclineOrchestrationResult result = DeclineOrchestrationService.buildDecline(null, null, true, "John");

    assertThat(result.summary()).contains("withdrawn");
    assertThat(result.auditEntry().getActorType()).isEqualTo(AuditActorType.USER);
  }

  @Test
  void buildDecline_autoDecline_setsAllFields() {
    DeclineOrchestrationResult result = DeclineOrchestrationService.buildDecline(null, null, false, "John");

    assertThat(result.summary()).startsWith("Automatic decline");
    assertThat(result.auditEntry().getActorType()).isEqualTo(AuditActorType.AGENT);
    assertThat(result.notificationRecords()).hasSize(3);
    assertThat(result.auditEntry().getEntryType()).isEqualTo(AuditEntryType.DECLINE);
  }

  @Test
  void buildDecline_caseNameContainsSupplierName() {
    OnboardingRequest req = new OnboardingRequest();
    Supplier supplier = new Supplier();
    supplier.setBusinessName("ACME Corp");
    req.setSupplier(supplier);

    DeclineOrchestrationResult result = DeclineOrchestrationService.buildDecline(req, null, false, "John");

    assertThat(result.caseName()).contains("ACME Corp");
  }

  @Test
  void buildDecline_declineReasons_fromFindingsOrDefault() {
    SupplierAgentResponse resp = new SupplierAgentResponse();
    ValidationFinding finding = new ValidationFinding();
    finding.setSeverity(FindingSeverity.FAILURE);
    finding.setMessage("Missing self-declaration");
    resp.setValidationFindings(List.of(finding));

    assertThat(DeclineOrchestrationService.buildDecline(null, resp, false, "John")
        .auditEntry().getDeclineReasons()).contains("Missing self-declaration");
    assertThat(DeclineOrchestrationService.buildDecline(null, null, false, "John")
        .auditEntry().getDeclineReasons()).contains("Risk score below minimum threshold");
  }
}
