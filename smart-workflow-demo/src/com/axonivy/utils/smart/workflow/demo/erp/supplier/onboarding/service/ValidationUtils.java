package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.PolicyValidationResult;
import ch.ivyteam.ivy.environment.Ivy;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.RuleType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.SupplierPolicyRule;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.repository.SupplierPolicyRuleRepository;

/**
 * Package-private shared utilities used by {@link DocumentExtractionService},
 * {@link PolicyValidationService}, and {@link FinancialValidationService}.
 *
 * <p>Has zero public surface area — it is an internal implementation detail of
 * the onboarding.service package and must never be referenced from outside it.</p>
 */
class ValidationUtils {

  private static final String STEP_NAMES_CMS_PREFIX =
      "/Dialogs/com/axonivy/utils/smart/workflow/demo/erp/supplier/onboarding/components/SupplierAgentProcessingDetails/";

  private ValidationUtils() {
  }

  /**
   * Returns the localized step display name for the given CMS key
   * (e.g. {@code "StepDocumentExtraction"}).
   */
  static String stepName(String cmsKey) {
    return Ivy.cms().co(STEP_NAMES_CMS_PREFIX + cmsKey);
  }

  /**
   * Loads detached copies of policy rules filtered by type so runtime mutation
   * of {@code isPassed} does not persist globally.
   */
  static List<SupplierPolicyRule> loadRulesByType(RuleType type) {
    List<SupplierPolicyRule> stored = SupplierPolicyRuleRepository.getInstance().findAllOrdered();
    List<SupplierPolicyRule> detached = new ArrayList<>();
    for (SupplierPolicyRule rule : stored) {
      if (!type.equals(rule.getRuleType())) {
        continue;
      }
      SupplierPolicyRule copy = new SupplierPolicyRule(
          rule.getTarget(), rule.getRule(), rule.getRiskScore(), false, rule.getRuleType());
      copy.setLegalDocumentType(rule.getLegalDocumentType());
      copy.setCertificationType(rule.getCertificationType());
      detached.add(copy);
    }
    return detached;
  }

  /**
   * Builds a map of normalized rule target → highest finding severity rank
   * across all findings in {@code result}.
   */
  static Map<String, Integer> resolveHighestSeverityByTarget(
      PolicyValidationResult result, List<SupplierPolicyRule> rules) {
    Map<String, Integer> highestByTarget = new HashMap<>();
    if (result == null || result.getFindings() == null || rules == null || rules.isEmpty()) {
      return highestByTarget;
    }
    for (ValidationFinding finding : result.getFindings()) {
      String target = resolveTargetFromFinding(finding, rules);
      if (target == null) {
        continue;
      }
      int rank = finding.getSeverity() != null ? finding.getSeverity().rank : 0;
      if (rank <= 0) {
        continue;
      }
      highestByTarget.merge(target, rank, Math::max);
    }
    return highestByTarget;
  }

  private static String resolveTargetFromFinding(ValidationFinding finding,
      List<SupplierPolicyRule> rules) {
    if (finding == null || finding.getSource() == null || rules == null) {
      return null;
    }
    String source = normalizeKey(finding.getSource());
    if (source.isEmpty()) {
      return null;
    }
    for (SupplierPolicyRule rule : rules) {
      String target = normalizeKey(rule.getTarget());
      if (source.equals(target) || source.contains(target)) {
        return target;
      }
    }
    return null;
  }

  static String normalizeKey(String key) {
    return key != null ? key.trim().toUpperCase() : "";
  }
}
