package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.SupplierRiskScore;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AuditEntryType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.OnboardingStatus;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service.OnboardingRequestService.CompletionContext;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestOnboardingRequestService {

  @Test
  void initRequest_setsSupplierAddressAndStatus() {
    OnboardingRequest noSupplier = new OnboardingRequest();
    OnboardingRequestService.initRequest(noSupplier);
    assertThat(noSupplier.getSupplier()).isNotNull();
    assertThat(noSupplier.getSupplier().getBusinessAddress()).isNotNull();
    assertThat(noSupplier.getStatus()).isEqualTo(OnboardingStatus.REQUEST);

    OnboardingRequest noAddress = new OnboardingRequest();
    noAddress.setSupplier(new Supplier());
    OnboardingRequestService.initRequest(noAddress);
    assertThat(noAddress.getSupplier().getBusinessAddress()).isNotNull();
    assertThat(noAddress.getStatus()).isEqualTo(OnboardingStatus.REQUEST);
  }

  @Test
  void applyRoutingState_allDecisions_setsStatusAndReturnsSummary() {
    OnboardingRequest req = new OnboardingRequest();
    SupplierAgentResponse resp = new SupplierAgentResponse();
    resp.setRiskScore(new SupplierRiskScore());

    assertThat(OnboardingRequestService.applyRoutingState(req, resp, "APPROVAL"))
        .contains("Green");
    assertThat(req.getStatus()).isEqualTo(OnboardingStatus.APPROVAL_PENDING);
    assertThat(req.getRiskScore()).isNotNull();

    assertThat(OnboardingRequestService.applyRoutingState(req, null, "CLARIFICATION"))
        .contains("Yellow");
    assertThat(req.getStatus()).isEqualTo(OnboardingStatus.CLARIFICATION_REQUIRED);

    assertThat(OnboardingRequestService.applyRoutingState(req, null, "DECLINE"))
        .contains("Red");
    assertThat(req.getStatus()).isEqualTo(OnboardingStatus.DECLINED);

    assertThat(OnboardingRequestService.applyRoutingState(req, null, "UNKNOWN"))
        .contains("unavailable");
    assertThat(req.getStatus()).isEqualTo(OnboardingStatus.RISK_SCORING);
  }

  @Test
  void completeRequest_setsStatusTimestampAndContextFields() {
    OnboardingRequest req = new OnboardingRequest();
    Supplier supplier = new Supplier();
    supplier.setBusinessName("ACME Corp");
    req.setSupplier(supplier);

    CompletionContext ctx = OnboardingRequestService.completeRequest(req);

    assertThat(req.getStatus()).isEqualTo(OnboardingStatus.COMPLETED);
    assertThat(req.getCompletedAt()).isNotBlank();
    assertThat(req.getProcessDuration()).isNotBlank();
    assertThat(ctx.caseName()).contains("ACME Corp");
    assertThat(ctx.summary()).contains("ACME Corp");
    assertThat(ctx.auditEntry().getEntryType()).isEqualTo(AuditEntryType.COMPLETION);
  }

  @Test
  void buildPostAgentCaseName_approvalReturnsCaseName_othersReturnNull() {
    OnboardingRequest req = new OnboardingRequest();
    Supplier supplier = new Supplier();
    supplier.setBusinessName("ACME Corp");
    req.setSupplier(supplier);

    assertThat(OnboardingRequestService.buildPostAgentCaseName(req, "APPROVAL"))
        .contains("ACME Corp");
    assertThat(OnboardingRequestService.buildPostAgentCaseName(req, "DECLINE")).isNull();
    assertThat(OnboardingRequestService.buildPostAgentCaseName(req, "CLARIFICATION")).isNull();
  }
}
