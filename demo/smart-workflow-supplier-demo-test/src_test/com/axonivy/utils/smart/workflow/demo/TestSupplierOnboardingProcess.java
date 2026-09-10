package com.axonivy.utils.smart.workflow.demo;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.SupplierDemoTestProcessData;
import com.axonivy.utils.smart.workflow.demo.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.SupplierRiskScore;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.audit.AuditTrailEntry;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.ApprovalDecision;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.ApprovalStage;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AuditEntryType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.OnboardingStatus;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskLevel;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.process.SupplierOnboardingData;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service.OnboardingAuditEntryFactory;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmElement;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;

@IvyProcessTest
public class TestSupplierOnboardingProcess {

  private static final BpmProcess TEST_PROCESS = BpmProcess.name("SupplierDemoTestProcess");
  private static final BpmProcess ONBOARDING_PROCESS = BpmProcess.name("SupplierOnboarding");

  @Test
  void installDemoData_allRepositoriesSeededWithExpectedCounts(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testInstallDemoData")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getPolicyRuleCount()).isGreaterThan(0);
    assertThat(data.getEmployeeCount()).isGreaterThan(0);
    assertThat(data.getDepartmentCount()).isGreaterThan(0);
    assertThat(data.getSupplierCount()).isGreaterThan(0);
  }

  @Test
  void happyPath_supplierApproved_completesWithFullAuditTrail(BpmClient client) {
    SupplierRiskScore riskScore = new SupplierRiskScore();
    riskScore.setAggregate(85);
    riskScore.setLevel(RiskLevel.GREEN);

    SupplierAgentResponse greenResponse = new SupplierAgentResponse();
    greenResponse.setRiskScore(riskScore);
    greenResponse.setRoutingDecision("APPROVAL");
    greenResponse.setIsSupplierExisting(false);

    client.mock()
      .uiOf(ONBOARDING_PROCESS.elementName("Start demo"))
      .with((params, results) -> {});

    client.mock()
      .uiOf(ONBOARDING_PROCESS.elementName("Supplier request form"))
      .with((params, results) -> {
        try {
          OnboardingRequest req = (OnboardingRequest) params.get("request");
          Supplier s = new Supplier();
          s.setBusinessName("Test Supplier GmbH");
          req.setSupplier(s);
          req.setStatus(OnboardingStatus.DB_CHECK);
          results.set("request", req);
        } catch (NoSuchFieldException e) { throw new RuntimeException(e); }
      });

    client.mock()
      .element(BpmElement.process(ONBOARDING_PROCESS).name("AI Agent: Check duplicate supplier"))
      .with((params, results) -> {
        try {
          OnboardingRequest req = (OnboardingRequest) params.get("request");
          req.setMatchedSuppliers(List.of());
          SupplierAgentResponse dupResponse = new SupplierAgentResponse();
          dupResponse.setIsSupplierExisting(false);
          results.set("agentResponse", dupResponse);
          results.set("request", req);
        } catch (NoSuchFieldException e) { throw new RuntimeException(e); }
      });

    client.mock()
      .uiOf(ONBOARDING_PROCESS.elementName("Duplicate check result"))
      .with((params, results) -> {
        try { results.set("request", params.get("request")); }
        catch (NoSuchFieldException e) { throw new RuntimeException(e); }
      });

    client.mock()
      .uiOf(ONBOARDING_PROCESS.elementName("Supplier registration form"))
      .with((params, results) -> {
        try { results.set("request", params.get("request")); }
        catch (NoSuchFieldException e) { throw new RuntimeException(e); }
      });

    client.mock()
      .uiOf(ONBOARDING_PROCESS.elementName("Agent processing"))
      .with((params, results) -> {
        try {
          OnboardingRequest req = (OnboardingRequest) params.get("request");
          req.getAuditTrail().add(OnboardingAuditEntryFactory.buildAgentAnalysisAuditEntry(req, greenResponse));
          results.set("agentResponse", greenResponse);
          results.set("routingDecision", "APPROVAL");
        } catch (NoSuchFieldException e) { throw new RuntimeException(e); }
      });

    client.mock()
      .uiOf(ONBOARDING_PROCESS.elementName("Supervisor approval dialog"))
      .with((params, results) -> {
        try {
          AuditTrailEntry entry = new AuditTrailEntry();
          entry.setEntryType(AuditEntryType.APPROVAL);
          entry.setDecision(ApprovalDecision.APPROVED);
          entry.setStage(ApprovalStage.SUPERVISOR);
          entry.setTimestamp(Instant.now().toString());
          results.set("auditEntry", entry);
        } catch (NoSuchFieldException e) { throw new RuntimeException(e); }
      });

    client.mock()
      .uiOf(ONBOARDING_PROCESS.elementName("QM/ISM approval dialog"))
      .with((params, results) -> {
        try {
          AuditTrailEntry entry = new AuditTrailEntry();
          entry.setEntryType(AuditEntryType.APPROVAL);
          entry.setDecision(ApprovalDecision.APPROVED);
          entry.setStage(ApprovalStage.QM_ISM);
          entry.setTimestamp(Instant.now().toString());
          results.set("auditEntry", entry);
        } catch (NoSuchFieldException e) { throw new RuntimeException(e); }
      });

    client.mock()
      .uiOf(ONBOARDING_PROCESS.elementName("Completion summary"))
      .with((params, results) -> {});

    client.mock()
      .uiOf(ONBOARDING_PROCESS.elementName("End Demo"))
      .with((params, results) -> {});

    var res = client.start().process(ONBOARDING_PROCESS.elementName("Supplier Onboarding")).as().everybody().execute();
    res = client.start().anyActiveTask(res).as().everybody().execute(); // Agent perform duplicate check
    res = client.start().anyActiveTask(res).as().everybody().execute(); // Review duplicate check result
    res = client.start().anyActiveTask(res).as().everybody().execute(); // Register new supplier
    res = client.start().anyActiveTask(res).as().everybody().execute(); // Validate supplier
    res = client.start().anyActiveTask(res).as().everybody().execute(); // Supervisor approval
    res = client.start().anyActiveTask(res).as().everybody().execute(); // QM/ISM approval
    res = client.start().anyActiveTask(res).as().everybody().execute(); // Completion review

    SupplierOnboardingData data = res.data().last();
    OnboardingRequest request = data.getRequest();
    assertThat(request.getStatus()).isEqualTo(OnboardingStatus.COMPLETED);

    var auditTrail = request.getAuditTrail();
    assertThat(auditTrail).hasSize(7);
    assertThat(auditTrail.get(0).getEntryType()).isEqualTo(AuditEntryType.DUPLICATE_CHECK);
    assertThat(auditTrail.get(1).getEntryType()).isEqualTo(AuditEntryType.DUPLICATE_DECISION);
    assertThat(auditTrail.get(2).getEntryType()).isEqualTo(AuditEntryType.REGISTRATION_CAPTURED);
    assertThat(auditTrail.get(3).getEntryType()).isEqualTo(AuditEntryType.AI_ANALYSIS);
    assertThat(auditTrail.get(4).getEntryType()).isEqualTo(AuditEntryType.APPROVAL);
    assertThat(auditTrail.get(4).getStage()).isEqualTo(ApprovalStage.SUPERVISOR);
    assertThat(auditTrail.get(4).getDecision()).isEqualTo(ApprovalDecision.APPROVED);
    assertThat(auditTrail.get(5).getEntryType()).isEqualTo(AuditEntryType.APPROVAL);
    assertThat(auditTrail.get(5).getStage()).isEqualTo(ApprovalStage.QM_ISM);
    assertThat(auditTrail.get(5).getDecision()).isEqualTo(ApprovalDecision.APPROVED);
    assertThat(auditTrail.get(6).getEntryType()).isEqualTo(AuditEntryType.COMPLETION);
  }

  @Test
  void clearDemoData_afterInstall_allRepositoriesAreEmpty(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testClearDemoData")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getPolicyRuleCount()).isZero();
    assertThat(data.getEmployeeCount()).isZero();
    assertThat(data.getDepartmentCount()).isZero();
    assertThat(data.getSupplierCount()).isZero();
  }
}
