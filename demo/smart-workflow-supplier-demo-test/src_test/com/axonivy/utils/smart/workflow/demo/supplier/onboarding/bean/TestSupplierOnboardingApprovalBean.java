package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.audit.AuditTrailEntry;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.ApprovalDecision;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.ApprovalStage;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AuditActorType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AuditEntryType;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestSupplierOnboardingApprovalBean {

  @Test
  void buildApprovalAuditEntry_setsAllFields() {
    SupplierOnboardingApprovalBean bean = new SupplierOnboardingApprovalBean();

    AuditTrailEntry entry = bean.buildApprovalAuditEntry(
        ApprovalDecision.APPROVED, "Looks good", "John Doe",
        "2024-01-01T00:00:00Z", ApprovalStage.SUPERVISOR);

    assertThat(entry.getActor()).isEqualTo("John Doe");
    assertThat(entry.getActorType()).isEqualTo(AuditActorType.USER);
    assertThat(entry.getEntryType()).isEqualTo(AuditEntryType.APPROVAL);
    assertThat(entry.getDecision()).isEqualTo(ApprovalDecision.APPROVED);
    assertThat(entry.getComment()).isEqualTo("Looks good");
    assertThat(entry.getStage()).isEqualTo(ApprovalStage.SUPERVISOR);
    assertThat(entry.getAction()).isEqualTo("SUPERVISOR approval decision");
    assertThat(entry.getTimestamp()).isEqualTo("2024-01-01T00:00:00Z");
  }

  @Test
  void buildApprovalAuditEntry_whenTimestampIsNull_generatesTimestamp() {
    SupplierOnboardingApprovalBean bean = new SupplierOnboardingApprovalBean();

    AuditTrailEntry entry = bean.buildApprovalAuditEntry(
        ApprovalDecision.APPROVED, null, "Jane", null, ApprovalStage.QM_ISM);

    assertThat(entry.getTimestamp()).isNotBlank();
  }

  @Test
  void buildApprovalAuditEntry_handlesNullInputs() {
    SupplierOnboardingApprovalBean bean = new SupplierOnboardingApprovalBean();

    AuditTrailEntry noStage = bean.buildApprovalAuditEntry(
        ApprovalDecision.APPROVED, null, "Admin", "2024-06-01T00:00:00Z", null);
    assertThat(noStage.getAction()).isEqualTo(" approval decision");

    AuditTrailEntry noComment = bean.buildApprovalAuditEntry(
        ApprovalDecision.APPROVED, null, "Admin", "2024-06-01T00:00:00Z", ApprovalStage.SUPERVISOR);
    assertThat(noComment.getComment()).isNull();
  }
}
