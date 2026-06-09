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
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.FindingSeverity;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.LogLineSeverity;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.repository.SupplierPolicyRuleRepository;

class ValidationUtils {

  static final int CONTENT_TRUNCATION_LIMIT = 3000;
  private static final int MAX_SCORE = 100;
  private static final int MIN_SCORE = 0;

  private static final String STEP_NAMES_CMS_PREFIX =
      "/Dialogs/com/axonivy/utils/smart/workflow/demo/erp/supplier/onboarding/components/SupplierAgentProcessingDetails/";

  private ValidationUtils() {
  }

  static String stepName(String cmsKey) {
    return Ivy.cms().co(STEP_NAMES_CMS_PREFIX + cmsKey);
  }

  static LogLineSeverity toLogSeverity(FindingSeverity severity) {
    if (severity == FindingSeverity.FAILURE) return LogLineSeverity.ERROR;
    if (severity == FindingSeverity.WARNING) return LogLineSeverity.WARNING;
    return LogLineSeverity.OK;
  }

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

  static int computeComplianceScore(PolicyValidationResult result, RuleType ruleType) {
    List<ValidationFinding> findings = result != null ? result.getFindings() : null;
    if (findings == null || findings.isEmpty()) {
      return MAX_SCORE;
    }
    Integer explicitScore = computeScoreFromExplicitFindings(findings);
    return explicitScore != null ? explicitScore : computeScoreFromRules(result, ruleType);
  }

  private static Integer computeScoreFromExplicitFindings(List<ValidationFinding> findings) {
    Map<String, Integer> maxScoreBySource = new HashMap<>();
    for (ValidationFinding finding : findings) {
      if (finding.getScore() != null && finding.getScore() > 0 && finding.getSource() != null) {
        maxScoreBySource.merge(normalizeKey(finding.getSource()), finding.getScore(), Math::max);
      }
    }
    if (maxScoreBySource.isEmpty()) {
      return null;
    }
    int totalDeduction = maxScoreBySource.values().stream().mapToInt(Integer::intValue).sum();
    return Math.max(MIN_SCORE, Math.min(MAX_SCORE, MAX_SCORE - totalDeduction));
  }

  private static int computeScoreFromRules(PolicyValidationResult result, RuleType ruleType) {
    List<SupplierPolicyRule> rules = loadRulesByType(ruleType);
    if (rules.isEmpty()) {
      return MAX_SCORE;
    }
    Map<String, Integer> highestSeverityByTarget = resolveHighestSeverityByTarget(result, rules);
    int score = MAX_SCORE;
    for (SupplierPolicyRule rule : rules) {
      int severityRank = highestSeverityByTarget.getOrDefault(normalizeKey(rule.getTarget()), 0);
      if (severityRank >= 2) {
        score -= rule.getRiskScore();
      } else if (severityRank == 1) {
        score -= Math.round(rule.getRiskScore() / 2.0f);
      }
    }
    return Math.max(MIN_SCORE, Math.min(MAX_SCORE, score));
  }
}
