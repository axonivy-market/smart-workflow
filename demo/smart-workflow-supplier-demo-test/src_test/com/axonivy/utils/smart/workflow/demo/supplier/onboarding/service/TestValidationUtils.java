package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.supplier.SupplierPolicyRule;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.PolicyValidationResult;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.FindingSeverity;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.LogLineSeverity;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestValidationUtils {

  @Test
  void normalizeKey_uppercasesAndTrims() {
    assertThat(ValidationUtils.normalizeKey("vat_rule")).isEqualTo("VAT_RULE");
    assertThat(ValidationUtils.normalizeKey("  VAT  ")).isEqualTo("VAT");
    assertThat(ValidationUtils.normalizeKey("Commercial Register")).isEqualTo("COMMERCIAL REGISTER");
  }

  @Test
  void toLogSeverity_mapsAllSeverities() {
    assertThat(ValidationUtils.toLogSeverity(FindingSeverity.FAILURE)).isEqualTo(LogLineSeverity.ERROR);
    assertThat(ValidationUtils.toLogSeverity(FindingSeverity.WARNING)).isEqualTo(LogLineSeverity.WARNING);
    assertThat(ValidationUtils.toLogSeverity(FindingSeverity.PASSED)).isEqualTo(LogLineSeverity.OK);
  }

  @Test
  void resolveHighestSeverityByTarget_picksMostSevereRankPerTarget() {
    SupplierPolicyRule rule = new SupplierPolicyRule();
    rule.setTarget("VAT Rule");

    ValidationFinding warning = new ValidationFinding();
    warning.setSource("VAT Rule");
    warning.setSeverity(FindingSeverity.WARNING);

    ValidationFinding failure = new ValidationFinding();
    failure.setSource("VAT Rule");
    failure.setSeverity(FindingSeverity.FAILURE);

    PolicyValidationResult result = new PolicyValidationResult();
    result.setFindings(List.of(warning, failure));

    Map<String, Integer> map = ValidationUtils.resolveHighestSeverityByTarget(result, List.of(rule));

    assertThat(map.get("VAT RULE")).isEqualTo(FindingSeverity.FAILURE.rank);
  }

  @Test
  void computeComplianceScore_deductsScorePerFailingFinding() {
    ValidationFinding finding = new ValidationFinding();
    finding.setSource("VAT Validation");
    finding.setScore(20);
    finding.setSeverity(FindingSeverity.FAILURE);

    PolicyValidationResult result = new PolicyValidationResult();
    result.setFindings(List.of(finding));

    assertThat(ValidationUtils.computeComplianceScore(result, null)).isEqualTo(80);
  }

  @Test
  void computeComplianceScore_passedFindingsIgnored() {
    ValidationFinding passed = new ValidationFinding();
    passed.setSource("OK Rule");
    passed.setSeverity(FindingSeverity.PASSED);

    ValidationFinding failure = new ValidationFinding();
    failure.setSource("Bad Rule");
    failure.setScore(15);
    failure.setSeverity(FindingSeverity.FAILURE);

    PolicyValidationResult result = new PolicyValidationResult();
    result.setFindings(new ArrayList<>(List.of(passed, failure)));

    assertThat(ValidationUtils.computeComplianceScore(result, null)).isEqualTo(85);
  }
}
