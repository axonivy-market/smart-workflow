package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.CrossReferenceResult;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.SupplierAgentResponse;
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
import com.axonivy.utils.smart.workflow.demo.erp.supplier.repository.SupplierRepository;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.repository.SupplierSearchCriteria;

/**
 * Stateless helpers for the cross-reference compliance check step of supplier
 * onboarding.
 *
 * <p>Checks performed: company register format validation, VAT ID format
 * validation, and ERP duplicate detection.
 * All external API calls are simulated for the demo; each check method is an
 * extension point for real integrations.</p>
 *
 * <p>All methods are stateless and static so that IvyScript in process Script
 * nodes can call them with a single import line.</p>
 */
public class CrossReferenceService {

  private static final Logger LOG = Logger.getLogger(CrossReferenceService.class.getName());

  // VAT ID format patterns keyed by ISO 3166-1 alpha-2 country code
  private static final Map<String, String> VAT_PATTERNS = new HashMap<>();

  static {
    VAT_PATTERNS.put("DE", "DE[0-9]{9}");
    VAT_PATTERNS.put("AT", "ATU[0-9]{8}");
    VAT_PATTERNS.put("CH", "CHE[0-9]{9}");
    VAT_PATTERNS.put("IT", "IT[0-9]{11}");
    VAT_PATTERNS.put("FR", "FR[0-9A-Z]{2}[0-9]{9}");
    VAT_PATTERNS.put("NL", "NL[0-9]{9}B[0-9]{2}");
    VAT_PATTERNS.put("BE", "BE0[0-9]{9}");
    VAT_PATTERNS.put("PL", "PL[0-9]{10}");
    VAT_PATTERNS.put("ES", "ES[A-Z][0-9]{7}[A-Z0-9]");
    VAT_PATTERNS.put("GB", "GB[0-9]{9}");
  }

  private CrossReferenceService() {
  }

  // ── Step lifecycle ─────────────────────────────────────────────────────────

  /**
   * Creates and returns a new {@link AgentProcessingStep} for cross-reference
   * validation, already in RUNNING state.
   */
  public static AgentProcessingStep startCrossReferenceStep() {
    AgentProcessingStep step = new AgentProcessingStep();
    step.setName(ValidationUtils.stepName("StepCrossReferenceChecks"));
    step.setStatus(AgentStepStatus.RUNNING);
    step.setStartedAt(Instant.now());
    return step;
  }

  /**
   * Marks the step COMPLETED, records timing, adds one log line per finding,
   * and attaches the step to the result.
   */
  public static void finalizeCrossReferenceStep(AgentProcessingStep step,
      CrossReferenceResult result) {
    step.setStatus(AgentStepStatus.COMPLETED);
    step.setCompletedAt(Instant.now());
    if (step.getStartedAt() != null) {
      step.setDurationMs(step.getCompletedAt().toEpochMilli() - step.getStartedAt().toEpochMilli());
    }
    if (step.getLogLines() == null) {
      step.setLogLines(new ArrayList<>());
    }
    if (result != null && result.getFindings() != null) {
      for (ValidationFinding finding : result.getFindings()) {
        finding.setRiskKind(RiskKind.AI_VALIDATION);
        FindingSeverity sev = finding.getSeverity();
        LogLineSeverity logSev = sev == FindingSeverity.FAILURE ? LogLineSeverity.ERROR
            : sev == FindingSeverity.WARNING ? LogLineSeverity.WARNING : LogLineSeverity.OK;
        step.getLogLines().add(LogLineBuilder.of(logSev, finding.getMessage()));
      }
    }
    if (result != null) {
      result.setProcessingStep(step);
    }
  }

  /**
   * Marks the step FAILED, logs the error, adds an error finding to the result,
   * and attaches the step.
   */
  public static void failCrossReferenceStep(AgentProcessingStep step,
      CrossReferenceResult result, Throwable error) {
    String msg = error != null ? error.getMessage() : "Unknown cross-reference error";
    LOG.log(Level.SEVERE, "Cross-reference check error: " + msg, error);
    step.setStatus(AgentStepStatus.FAILED);
    step.setCompletedAt(Instant.now());
    if (step.getStartedAt() != null) {
      step.setDurationMs(step.getCompletedAt().toEpochMilli() - step.getStartedAt().toEpochMilli());
    }
    if (step.getLogLines() == null) {
      step.setLogLines(new ArrayList<>());
    }
    step.getLogLines().add(LogLineBuilder.of(LogLineSeverity.ERROR,
        "Cross-reference check failed: " + msg));
    if (result != null) {
      ValidationFinding errorFinding = ValidationFindingBuilder.of(
          FindingSeverity.FAILURE, "Cross-reference check failed: " + msg,
          "system", RiskType.POLICY_COMPLIANCE);
      errorFinding.setRiskKind(RiskKind.AI_VALIDATION);
      result.getFindings().add(errorFinding);
      result.setProcessingStep(step);
    }
  }

  // ── Check orchestration ────────────────────────────────────────────────────

  /**
   * Runs all checks using pre-populated matched suppliers from Step 1,
   * avoiding a second DB query.
   */
  public static List<ValidationFinding> runAllChecks(String supplierId, String vatId,
      String commercialRegisterNo, String businessName, String country,
      List<Supplier> matchedSuppliers) {
    List<ValidationFinding> findings = new ArrayList<>();
    findings.add(validateCompanyRegister(commercialRegisterNo, country));
    findings.add(validateVatId(vatId, country));
    findings.add(checkErpDuplicate(supplierId, matchedSuppliers));
    return findings;
  }

  /** Runs all checks with a fresh ERP duplicate query. */
  public static List<ValidationFinding> runAllChecks(String supplierId, String vatId,
      String commercialRegisterNo, String businessName, String country) {
    List<ValidationFinding> findings = new ArrayList<>();
    findings.add(validateCompanyRegister(commercialRegisterNo, country));
    findings.add(validateVatId(vatId, country));
    findings.add(checkErpDuplicate(businessName, country, supplierId));
    return findings;
  }

  // ── Result formatting ──────────────────────────────────────────────────────

  /** Builds the plain-text findings summary returned to the process or LLM. */
  public static String buildFindingsSummary(CrossReferenceResult result) {
    StringBuilder summary = new StringBuilder("Cross-reference check completed. Findings:\n");
    for (ValidationFinding finding : result.getFindings()) {
      summary.append("- [").append(finding.getSeverity()).append("] ")
             .append(finding.getMessage()).append("\n");
    }
    if (result.getFindings().isEmpty()) {
      summary.append("No issues found.");
    }
    return summary.toString();
  }

  // ── Individual checks (extension points for real integrations) ────────────

  /**
   * Validates the format of a commercial register number.
   * For Germany: expects HRB/HRA prefix followed by digits.
   * Extension point: replace with real company register API call.
   */
  public static ValidationFinding validateCompanyRegister(String registerNo, String country) {
    String source = "Company Register";
    if (registerNo == null || registerNo.trim().isEmpty()) {
      return ValidationFindingBuilder.of(FindingSeverity.WARNING,
          "No commercial register number provided — manual verification required", source, RiskType.POLICY_COMPLIANCE);
    }
    String cleaned = registerNo.trim().toUpperCase().replace(" ", "");
    boolean valid;
    String detail;
    if ("DE".equalsIgnoreCase(country)) {
      valid = cleaned.matches("(HRB|HRA|HRE|GNR|PR|VR)[0-9]+");
      detail = valid ? "active, no insolvency proceedings" : "format invalid (expected HRB/HRA + digits for DE)";
    } else {
      valid = !cleaned.isEmpty();
      detail = valid ? "company registration document verified" : "register number is empty";
    }
    FindingSeverity severity = valid ? FindingSeverity.PASSED : FindingSeverity.WARNING;
    return ValidationFindingBuilder.of(severity, "Company Register: " + registerNo + " — " + detail, source, RiskType.POLICY_COMPLIANCE);
  }

  /**
   * Validates VAT ID format against country-specific regex patterns.
   * Extension point: replace with real EU VIES API call.
   */
  public static ValidationFinding validateVatId(String vatId, String country) {
    String source = "VAT Validation";
    if (vatId == null || vatId.trim().isEmpty()) {
      return ValidationFindingBuilder.of(FindingSeverity.WARNING,
          "No VAT ID provided — may be exempt for sole traders or certain legal forms", source, RiskType.POLICY_COMPLIANCE);
    }
    String cleaned = vatId.trim().replace(" ", "").replace("-", "").toUpperCase();
    String countryCode = (country != null && !country.isEmpty()) ? country.toUpperCase() : "";

    String pattern = VAT_PATTERNS.get(countryCode);
    if (pattern == null && cleaned.length() >= 2) {
      pattern = VAT_PATTERNS.get(cleaned.substring(0, 2));
    }
    if (pattern == null) {
      return ValidationFindingBuilder.of(FindingSeverity.PASSED,
          "VAT ID " + vatId + " — accepted (no country-specific format rule configured)", source, RiskType.POLICY_COMPLIANCE);
    }
    if (cleaned.matches(pattern)) {
      return ValidationFindingBuilder.of(FindingSeverity.PASSED, "VAT ID " + vatId + " — confirmed", source, RiskType.POLICY_COMPLIANCE);
    }
    return ValidationFindingBuilder.of(FindingSeverity.FAILURE,
        "VAT ID " + vatId + " — format invalid for country " + countryCode
            + " (expected pattern: " + pattern + ")", source, RiskType.POLICY_COMPLIANCE);
  }

  /**
   * Checks for duplicates using the pre-populated matched suppliers list from Step 1.
   * Avoids a second DB query by reusing matches already found during the initial
   * duplicate check step (stored in OnboardingRequest.matchedSuppliers).
   */
  public static ValidationFinding checkErpDuplicate(String supplierId,
      List<Supplier> matchedSuppliers) {
    String source = "ERP Duplicate Check";
    if (matchedSuppliers == null || matchedSuppliers.isEmpty()) {
      return ValidationFindingBuilder.of(FindingSeverity.PASSED,
          "No duplicate in ERP — no similar suppliers found", source, RiskType.POLICY_COMPLIANCE);
    }
    long count = 0;
    for (Supplier s : matchedSuppliers) {
      if (supplierId == null || !supplierId.equals(s.getSupplierId())) {
        count++;
      }
    }
    if (count == 0) {
      return ValidationFindingBuilder.of(FindingSeverity.PASSED,
          "No duplicate in ERP (distinct from existing supplier record)", source, RiskType.POLICY_COMPLIANCE);
    }
    return ValidationFindingBuilder.of(FindingSeverity.WARNING,
        "Possible ERP duplicate: " + count + " similar supplier(s) found — manual review recommended",
        source, RiskType.POLICY_COMPLIANCE);
  }

  /**
   * Checks for similar/duplicate suppliers via a fresh ERP query.
   * Extension point: enrich with fuzzy-name matching or external deduplication service.
   */
  public static ValidationFinding checkErpDuplicate(String businessName, String country,
      String excludeSupplierId) {
    String source = "ERP Duplicate Check";
    if (businessName == null || businessName.trim().isEmpty()) {
      return ValidationFindingBuilder.of(FindingSeverity.WARNING,
          "Business name not provided — ERP duplicate check skipped", source, RiskType.POLICY_COMPLIANCE);
    }
    SupplierSearchCriteria criteria = new SupplierSearchCriteria();
    criteria.setBusinessNameContains(businessName);
    if (country != null && !country.isEmpty()) {
      criteria.setCountry(country);
    }
    SupplierAgentResponse response = SupplierRepository.getInstance().findSimilarSuppliers(criteria);
    List<Supplier> matches = response.getSuppliers();
    if (matches == null || matches.isEmpty()) {
      return ValidationFindingBuilder.of(FindingSeverity.PASSED,
          "No duplicate in ERP — no similar suppliers found", source, RiskType.POLICY_COMPLIANCE);
    }
    long count = 0;
    for (Supplier s : matches) {
      if (!s.getSupplierId().equals(excludeSupplierId)) {
        count++;
      }
    }
    if (count == 0) {
      return ValidationFindingBuilder.of(FindingSeverity.PASSED,
          "No duplicate in ERP (distinct from existing supplier record)", source, RiskType.POLICY_COMPLIANCE);
    }
    return ValidationFindingBuilder.of(FindingSeverity.WARNING,
        "Possible ERP duplicate: " + count + " similar supplier(s) found — manual review recommended",
        source, RiskType.POLICY_COMPLIANCE);
  }

}
