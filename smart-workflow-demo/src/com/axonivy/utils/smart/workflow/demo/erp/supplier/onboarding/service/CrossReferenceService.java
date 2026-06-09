package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.service;

import java.time.Instant;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.CrossReferenceResult;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.RiskKind;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.RiskType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.builder.LogLineBuilder;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.builder.ValidationFindingBuilder;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.AgentStepStatus;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.FindingSeverity;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.LogLineSeverity;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.ValidationFindingType;

import ch.ivyteam.ivy.environment.Ivy;

public class CrossReferenceService {

  private static final String STEP_NAME_KEY = "StepCrossReferenceChecks";
  private static final String UNKNOWN_ERROR_MSG = "Unknown cross-reference error";
  private static final String CROSS_REF_LOG_ERROR_PREFIX = "Cross-reference check error: ";
  private static final String CROSS_REF_FAIL_PREFIX = "Cross-reference check failed: ";
  private static final String FINDINGS_HEADER = "Cross-reference check completed. Findings:\n";
  private static final String FINDING_FORMAT = "- [%s] %s\n";
  private static final String NO_ISSUES_MSG = "No issues found.";
  private static final Pattern DE_REGISTER_PATTERN = Pattern.compile("(HRB|HRA|HRE|GNR|PR|VR)[0-9]+");

  private static final Map<String, String> VAT_PATTERNS = Map.of(
      "DE", "DE[0-9]{9}",
      "AT", "ATU[0-9]{8}",
      "CH", "CHE[0-9]{9}",
      "IT", "IT[0-9]{11}",
      "FR", "FR[0-9A-Z]{2}[0-9]{9}",
      "NL", "NL[0-9]{9}B[0-9]{2}",
      "BE", "BE0[0-9]{9}",
      "PL", "PL[0-9]{10}",
      "ES", "ES[A-Z][0-9]{7}[A-Z0-9]",
      "GB", "GB[0-9]{9}"
  );

  private CrossReferenceService() {
  }

  public static AgentProcessingStep startCrossReferenceStep() {
    AgentProcessingStep step = new AgentProcessingStep();
    step.setName(ValidationUtils.stepName(STEP_NAME_KEY));
    step.setStatus(AgentStepStatus.RUNNING);
    step.setStartedAt(Instant.now());
    return step;
  }

  public static void finalizeCrossReferenceStep(AgentProcessingStep step,
      CrossReferenceResult result) {
    completeStep(step, AgentStepStatus.COMPLETED);
    if (result != null) {
      if (result.getFindings() != null) {
        for (ValidationFinding finding : result.getFindings()) {
          finding.setRiskKind(RiskKind.AI_VALIDATION);
          step.getLogLines().add(LogLineBuilder.of(
              ValidationUtils.toLogSeverity(finding.getSeverity()), finding.getMessage()));
        }
      }
      result.setProcessingStep(step);
    }
  }

  public static String failCrossReferenceStep(AgentProcessingStep step,
      CrossReferenceResult result, Throwable error) {
    String message = error != null ? error.getMessage() : UNKNOWN_ERROR_MSG;
    Ivy.log().error(CROSS_REF_LOG_ERROR_PREFIX + message, error);
    step.setName(ValidationUtils.stepName(STEP_NAME_KEY));
    completeStep(step, AgentStepStatus.FAILED);
    String summary = CROSS_REF_FAIL_PREFIX + message;
    step.getLogLines().add(LogLineBuilder.of(LogLineSeverity.ERROR, summary));
    if (result != null) {
      ValidationFinding errorFinding = ValidationFindingBuilder.of(
          FindingSeverity.FAILURE, summary, "system", RiskType.POLICY_COMPLIANCE);
      errorFinding.setRiskKind(RiskKind.AI_VALIDATION);
      result.getFindings().add(errorFinding);
      result.setProcessingStep(step);
    }
    return summary;
  }

  private static void completeStep(AgentProcessingStep step, AgentStepStatus status) {
    step.setStatus(status);
    step.setCompletedAt(Instant.now());
    if (step.getStartedAt() != null) {
      step.setDurationMs(step.getCompletedAt().toEpochMilli() - step.getStartedAt().toEpochMilli());
    }
    if (step.getLogLines() == null) {
      step.setLogLines(new ArrayList<>());
    }
  }

  public static List<ValidationFinding> runAllChecks(String supplierId, String vatId,
      String commercialRegisterNo, String country, List<Supplier> matchedSuppliers) {
    List<ValidationFinding> findings = new ArrayList<>();
    findings.add(validateCompanyRegister(commercialRegisterNo, country));
    findings.add(validateVatId(vatId, country));
    findings.add(checkErpDuplicate(supplierId, matchedSuppliers));
    return findings;
  }

  public static String buildFindingsSummary(CrossReferenceResult result) {
    List<ValidationFinding> findings = result.getFindings();
    StringBuilder summary = new StringBuilder(FINDINGS_HEADER);
    for (ValidationFinding finding : findings) {
      summary.append(String.format(FINDING_FORMAT, finding.getSeverity(), finding.getMessage()));
    }
    if (findings.isEmpty()) {
      summary.append(NO_ISSUES_MSG);
    }
    return summary.toString();
  }

  public static ValidationFinding validateCompanyRegister(String registerNo, String country) {
    if (registerNo == null || registerNo.trim().isEmpty()) {
      return ValidationFindingBuilder.of(ValidationFindingType.COMPANY_REGISTER_MISSING);
    }
    String cleaned = registerNo.trim().toUpperCase().replace(" ", "");
    if ("DE".equalsIgnoreCase(country)) {
      boolean valid = DE_REGISTER_PATTERN.matcher(cleaned).matches();
      return ValidationFindingBuilder.of(
          valid ? ValidationFindingType.COMPANY_REGISTER_VALID_DE
                : ValidationFindingType.COMPANY_REGISTER_FORMAT_INVALID_DE,
          registerNo);
    }
    return ValidationFindingBuilder.of(ValidationFindingType.COMPANY_REGISTER_VALID, registerNo);
  }

  public static ValidationFinding validateVatId(String vatId, String country) {
    if (vatId == null || vatId.trim().isEmpty()) {
      return ValidationFindingBuilder.of(ValidationFindingType.VAT_ID_MISSING);
    }
    String cleaned = vatId.trim().replace(" ", "").replace("-", "").toUpperCase();
    String vatPrefix = cleaned.length() >= 2 ? cleaned.substring(0, 2) : "";
    String countryCode = (country != null && !country.isEmpty()) ? country.toUpperCase() : vatPrefix;
    Optional<String> pattern = Optional.ofNullable(VAT_PATTERNS.get(countryCode));
    if (pattern.isEmpty()) {
      return ValidationFindingBuilder.of(ValidationFindingType.VAT_ID_NO_RULE, vatId);
    }
    if (cleaned.matches(pattern.get())) {
      return ValidationFindingBuilder.of(ValidationFindingType.VAT_ID_CONFIRMED, vatId);
    }
    return ValidationFindingBuilder.of(ValidationFindingType.VAT_ID_INVALID, vatId, countryCode, pattern.get());
  }

  public static ValidationFinding checkErpDuplicate(String supplierId,
      List<Supplier> matchedSuppliers) {
    if (matchedSuppliers == null || matchedSuppliers.isEmpty()) {
      return ValidationFindingBuilder.of(ValidationFindingType.ERP_NO_SIMILAR);
    }
    long duplicateCount = matchedSuppliers.stream()
        .filter(candidate -> supplierId == null || !supplierId.equals(candidate.getSupplierId()))
        .count();
    if (duplicateCount == 0) {
      return ValidationFindingBuilder.of(ValidationFindingType.ERP_NO_DUPLICATE);
    }
    return ValidationFindingBuilder.of(ValidationFindingType.ERP_DUPLICATE_FOUND, duplicateCount);
  }

}
