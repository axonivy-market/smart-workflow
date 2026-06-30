package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.supplier.SupplierPolicyRule;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.PolicyValidationResult;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.FindingSeverity;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskKind;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskType;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestPolicyValidationService {

  @Test
  void filterExistingFindings_removesMissingDocFindings_keepsOthers() {
    ValidationFinding missingDoc = new ValidationFinding();
    missingDoc.setRiskKind(RiskKind.MISSING_DOC);
    missingDoc.setMessage("Missing commercial register");

    ValidationFinding aiValidation = new ValidationFinding();
    aiValidation.setRiskKind(RiskKind.AI_VALIDATION);
    aiValidation.setMessage("AI check passed");

    List<ValidationFinding> result = PolicyValidationService.filterExistingFindings(
        List.of(missingDoc, aiValidation));

    assertThat(result).doesNotContain(missingDoc);
    assertThat(result).contains(aiValidation);
  }

  @Test
  void wrapFindings_deduplicatesByMessage() {
    ValidationFinding f1 = new ValidationFinding();
    f1.setMessage("Duplicate message");
    f1.setSeverity(FindingSeverity.WARNING);
    ValidationFinding f2 = new ValidationFinding();
    f2.setMessage("Duplicate message");
    f2.setSeverity(FindingSeverity.WARNING);
    assertThat(PolicyValidationService.wrapFindings(List.of(f1, f2)).getFindings()).hasSize(1);

    ValidationFinding u1 = new ValidationFinding();
    u1.setMessage("First finding");
    ValidationFinding u2 = new ValidationFinding();
    u2.setMessage("Second finding");
    assertThat(PolicyValidationService.wrapFindings(List.of(u1, u2)).getFindings()).hasSize(2);
  }

  @Test
  void mergePresenceFindings_addsPresenceFindingsBeforeExisting() {
    PolicyValidationResult result = new PolicyValidationResult();
    ValidationFinding existing = new ValidationFinding();
    existing.setMessage("Existing finding");
    result.setFindings(new ArrayList<>(List.of(existing)));

    ValidationFinding presence = new ValidationFinding();
    presence.setMessage("Presence finding");

    PolicyValidationService.mergePresenceFindings(result, List.of(presence));

    assertThat(result.getFindings()).hasSize(2);
    assertThat(result.getFindings().get(0).getMessage()).isEqualTo("Presence finding");
    assertThat(result.getFindings().get(1).getMessage()).isEqualTo("Existing finding");
  }

  @Test
  void isRuleAlreadyEvaluated_matchesOnNormalizedSource() {
    SupplierPolicyRule rule = new SupplierPolicyRule();
    rule.setTarget("VAT_RULE");

    ValidationFinding matching = new ValidationFinding();
    matching.setSource("vat_rule");
    assertThat(PolicyValidationService.isRuleAlreadyEvaluated(rule, List.of(matching))).isTrue();

    ValidationFinding nonMatching = new ValidationFinding();
    nonMatching.setSource("COMMERCIAL_REGISTER");
    assertThat(PolicyValidationService.isRuleAlreadyEvaluated(rule, List.of(nonMatching))).isFalse();
  }

  @Test
  void mergeRuleFindings_setsRiskTypeAndRiskKind() {
    PolicyValidationResult ruleResult = new PolicyValidationResult();
    ValidationFinding f = new ValidationFinding();
    f.setMessage("Policy check");
    f.setSeverity(FindingSeverity.PASSED);
    ruleResult.setFindings(new ArrayList<>(List.of(f)));

    List<ValidationFinding> accumulated = new ArrayList<>();
    PolicyValidationService.mergeRuleFindings(accumulated, ruleResult);

    assertThat(accumulated).hasSize(1);
    assertThat(accumulated.get(0).getRiskType()).isEqualTo(RiskType.POLICY_COMPLIANCE);
    assertThat(accumulated.get(0).getRiskKind()).isEqualTo(RiskKind.AI_VALIDATION);
  }

  @Test
  void mergeRuleFindings_skipsDuplicateByDocumentTypeKey() {
    ValidationFinding existing = new ValidationFinding();
    existing.setDocumentTypeKey("CERTIFICATION:ISO_9001");
    existing.setMessage("Already there");

    ValidationFinding incoming = new ValidationFinding();
    incoming.setDocumentTypeKey("CERTIFICATION:ISO_9001");
    incoming.setMessage("Duplicate");

    PolicyValidationResult ruleResult = new PolicyValidationResult();
    ruleResult.setFindings(new ArrayList<>(List.of(incoming)));

    List<ValidationFinding> accumulated = new ArrayList<>(List.of(existing));
    PolicyValidationService.mergeRuleFindings(accumulated, ruleResult);

    assertThat(accumulated).hasSize(1);
  }
}
