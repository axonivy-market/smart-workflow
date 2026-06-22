package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.axonivy.utils.smart.workflow.demo.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskKind;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RiskType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RuleType;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierPolicyRule;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.DocumentExtractionResult;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.PolicyValidationResult;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.agent.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder.LogLineBuilder;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder.PolicyContextBuilder;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder.ValidationFindingBuilder;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AgentStepStatus;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.FindingSeverity;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.LogLineSeverity;

import ch.ivyteam.ivy.environment.Ivy;

public class PolicyValidationService {

  private static final String STEP_NAME_KEY = "StepPolicyValidation";
  private static final String UNKNOWN_ERROR_MSG = "Unknown policy validation error";
  private static final String POLICY_FAIL_PREFIX = "Policy validation failed: ";
  private static final String POLICY_CANT_COMPLETE_PREFIX = "Policy validation could not complete: ";
  private static final String ALL_CHECKS_PASSED = "All policy checks passed.";
  private static final String ALL_RULES_PASSED_FORMAT = "All %d policy rules passed.";
  private static final String FINDING_SUMMARY_FORMAT = "[%s] %s\n";

  private PolicyValidationService() {
  }

  public static List<SupplierPolicyRule> loadPolicyRules(String caseUuid) {
    return ValidationUtils.loadRulesByType(RuleType.POLICY, caseUuid);
  }

  public static List<ValidationFinding> checkRequiredDocuments(List<LegalDocument> docs) {
    List<ValidationFinding> findings = new ArrayList<>();
    for (LegalDocumentType docType : LegalDocumentType.values()) {
      if (!docType.isRequired()) {
        continue;
      }
      boolean present = docs != null && docs.stream().anyMatch(doc -> docType.equals(doc.getDocumentType()));
      RiskKind messageKind = present ? RiskKind.AI_VALIDATION : RiskKind.MISSING_DOC;
      ValidationFinding f = ValidationFindingBuilder.of(
          present ? FindingSeverity.PASSED : FindingSeverity.FAILURE,
          Ivy.cms().co(messageKind.getCmsUri(), Arrays.asList(docType.getLabel())),
          docType.name(), RiskType.CERTIFICATION_VALIDITY);
      f.setDocumentTypeKey(docType.getDocumentTypeKey());
      f.setRiskKind(RiskKind.MISSING_DOC);
      f.setScore(0);
      findings.add(f);
    }
    return findings;
  }

  public static void mergePresenceFindings(PolicyValidationResult result,
      List<ValidationFinding> presenceFindings) {
    if (result == null || presenceFindings == null || presenceFindings.isEmpty()) {
      return;
    }
    List<ValidationFinding> merged = new ArrayList<>(presenceFindings);
    if (result.getFindings() != null) {
      merged.addAll(result.getFindings());
    }
    result.setFindings(merged);
  }

  public static boolean hasRuleDocument(SupplierPolicyRule rule,
      DocumentExtractionResult extractionResult) {
    return PolicyContextBuilder.hasRuleDocument(rule, extractionResult);
  }

  public static List<ValidationFinding> filterExistingFindings(
      List<ValidationFinding> existingFindings) {
    if (existingFindings == null || existingFindings.isEmpty()) {
      return new ArrayList<>();
    }
    List<ValidationFinding> kept = new ArrayList<>();
    for (ValidationFinding f : existingFindings) {
      if (RiskKind.MISSING_DOC.equals(f.getRiskKind())) {
        continue;
      }
      kept.add(f);
    }
    return kept;
  }

  public static boolean isRuleAlreadyEvaluated(SupplierPolicyRule rule,
      List<ValidationFinding> filteredExistingFindings) {
    if (rule == null || filteredExistingFindings == null || filteredExistingFindings.isEmpty()) {
      return false;
    }
    String ruleTarget = ValidationUtils.normalizeKey(rule.getTarget());
    for (ValidationFinding f : filteredExistingFindings) {
      if (ruleTarget.equals(ValidationUtils.normalizeKey(f.getSource()))) {
        return true;
      }
    }
    return false;
  }

  public static List<ValidationFinding> initAccumulatedFromExisting(
      List<ValidationFinding> filteredExistingFindings) {
    if (filteredExistingFindings == null || filteredExistingFindings.isEmpty()) {
      return new ArrayList<>();
    }
    return new ArrayList<>(filteredExistingFindings);
  }

  public static void mergeRuleFindings(List<ValidationFinding> accumulated,
      PolicyValidationResult ruleResult) {
    if (ruleResult == null || ruleResult.getFindings() == null || accumulated == null) {
      return;
    }
    for (ValidationFinding f : ruleResult.getFindings()) {
      f.setRiskType(RiskType.POLICY_COMPLIANCE);
      f.setRiskKind(RiskKind.AI_VALIDATION);
      if (!isDuplicate(accumulated, f)) {
        accumulated.add(f);
      }
    }
  }

  public static PolicyValidationResult wrapFindings(List<ValidationFinding> findings) {
    PolicyValidationResult result = new PolicyValidationResult();
    findings = findings != null ? findings : new ArrayList<>();
    List<ValidationFinding> distinct = new ArrayList<>();
    Set<String> seenMessages = new LinkedHashSet<>();
    for (ValidationFinding f : findings) {
      if (seenMessages.add(f.getMessage())) {
        f.setUserExplanation(null);
        distinct.add(f);
      }
    }
    result.setFindings(distinct);
    return result;
  }

  public static List<SupplierPolicyRule> evaluatePolicyRules(PolicyValidationResult result, String caseUuid) {
    List<SupplierPolicyRule> rules = loadPolicyRules(caseUuid);
    Map<String, Integer> highestSeverityByTarget = ValidationUtils.resolveHighestSeverityByTarget(result, rules);
    for (SupplierPolicyRule rule : rules) {
      int severityRank = highestSeverityByTarget.getOrDefault(ValidationUtils.normalizeKey(rule.getTarget()), 0);
      rule.setPassed(severityRank == 0);
    }
    return rules;
  }

  public static int computePolicyComplianceScore(PolicyValidationResult result, String caseUuid) {
    return ValidationUtils.computeComplianceScore(result, RuleType.POLICY, caseUuid);
  }

  public static AgentProcessingStep startPolicyStep() {
    AgentProcessingStep step = new AgentProcessingStep();
    step.setName(ValidationUtils.stepName(STEP_NAME_KEY));
    step.setStatus(AgentStepStatus.RUNNING);
    step.setStartedAt(Instant.now());
    return step;
  }

  public static String finalizePolicyStep(AgentProcessingStep step,
      PolicyValidationResult result, String caseUuid) {
    step.setStatus(AgentStepStatus.COMPLETED);
    step.setCompletedAt(Instant.now());
    if (step.getStartedAt() != null) {
      step.setDurationMs(step.getCompletedAt().toEpochMilli() - step.getStartedAt().toEpochMilli());
    }
    if (step.getLogLines() == null) {
      step.setLogLines(new ArrayList<>());
    }

    if (result == null) {
      result = new PolicyValidationResult();
    }

    StringBuilder summary = new StringBuilder();
    if (result.getFindings() != null) {
      for (ValidationFinding finding : result.getFindings()) {
        FindingSeverity sev = finding.getSeverity();
        if (sev == FindingSeverity.PASSED) {
          continue;
        }
        LogLineSeverity logSev = sev == FindingSeverity.FAILURE ? LogLineSeverity.ERROR
            : sev == FindingSeverity.WARNING ? LogLineSeverity.WARNING : LogLineSeverity.OK;
        step.getLogLines().add(LogLineBuilder.of(logSev, finding.getMessage()));
        summary.append(String.format(FINDING_SUMMARY_FORMAT, sev, finding.getMessage()));
      }
    }
    if (step.getLogLines().isEmpty()) {
      int count = loadPolicyRules(caseUuid).size();
      String summaryMsg = count > 0
          ? String.format(ALL_RULES_PASSED_FORMAT, count)
          : ALL_CHECKS_PASSED;
      step.getLogLines().add(LogLineBuilder.of(LogLineSeverity.OK, summaryMsg));
    }
    result.setProcessingStep(step);
    return summary.length() > 0 ? summary.toString() : ALL_CHECKS_PASSED;
  }

  public static String failPolicyStep(AgentProcessingStep step,
      PolicyValidationResult result, Throwable error) {
    step.setName(ValidationUtils.stepName(STEP_NAME_KEY));
    step.setStatus(AgentStepStatus.FAILED);
    step.setCompletedAt(Instant.now());
    if (step.getStartedAt() != null) {
      step.setDurationMs(step.getCompletedAt().toEpochMilli() - step.getStartedAt().toEpochMilli());
    }
    if (step.getLogLines() == null) {
      step.setLogLines(new ArrayList<>());
    }
    String msg = error != null ? error.getMessage() : UNKNOWN_ERROR_MSG;
    Ivy.log().error(POLICY_FAIL_PREFIX + msg, error);
    step.getLogLines().add(LogLineBuilder.of(LogLineSeverity.ERROR, POLICY_FAIL_PREFIX + msg));
    ValidationFinding errorFinding = ValidationFindingBuilder.of(
        FindingSeverity.FAILURE, POLICY_CANT_COMPLETE_PREFIX + msg, "system", RiskType.POLICY_COMPLIANCE);
    errorFinding.setRiskKind(RiskKind.AI_VALIDATION);
    result.getFindings().add(errorFinding);
    result.setProcessingStep(step);
    return POLICY_FAIL_PREFIX + msg;
  }

  public static PolicyValidationResult finalizePolicyValidation(
      List<ValidationFinding> accumulatedFindings,
      List<ValidationFinding> presenceFindings,
      AgentProcessingStep processingStep,
      OnboardingRequest onboardingRequest,
      String caseUuid) {
    PolicyValidationResult result = wrapFindings(accumulatedFindings);
    mergePresenceFindings(result, presenceFindings);
    finalizePolicyStep(processingStep, result, caseUuid);
    result.setRuleEvaluations(evaluatePolicyRules(result, caseUuid));
    result.setComplianceScore(computePolicyComplianceScore(result, caseUuid));
    if (onboardingRequest != null) {
      onboardingRequest.setPolicyValidationFindings(result.getFindings());
    }
    return result;
  }

private static boolean isDuplicate(List<ValidationFinding> accumulated, ValidationFinding incoming) {
    String docKey = incoming.getDocumentTypeKey();
    String source = incoming.getSource();
    for (ValidationFinding existing : accumulated) {
      if (docKey != null) {
        if (docKey.equalsIgnoreCase(existing.getDocumentTypeKey())) {
          return true;
        }
      } else if (source != null && source.equalsIgnoreCase(existing.getSource())
          && existing.getDocumentTypeKey() == null) {
        return true;
      }
    }
    return false;
  }
}
