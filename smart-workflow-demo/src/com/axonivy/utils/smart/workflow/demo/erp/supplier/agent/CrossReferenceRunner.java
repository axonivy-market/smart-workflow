package com.axonivy.utils.smart.workflow.demo.erp.supplier.agent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.RiskKind;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.RiskType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep.LogLineSeverity;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep.StepStatus;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.repository.SupplierRepository;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.repository.SupplierSearchCriteria;

/**
 * Runs cross-reference compliance checks for supplier onboarding and assembles
 * the result with a timed {@link AgentProcessingStep}.
 *
 * <p>Checks performed: company register format validation, VAT ID format
 * validation, and ERP duplicate detection.
 * All external API calls are simulated for the demo; each check method is an
 * extension point for real integrations.</p>
 */
public class CrossReferenceRunner {

  private static final Logger LOG = Logger.getLogger(CrossReferenceRunner.class.getName());

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

  private CrossReferenceRunner() {
  }

  // ── Process entry points ───────────────────────────────────────────────────

  /**
   * Runs all cross-reference checks for the given onboarding request and
   * returns a {@link CrossReferenceResult} with findings and a timed
   * {@link AgentProcessingStep}.
   */
  public static CrossReferenceResult run(OnboardingRequest request) {
    AgentProcessingStep step = new AgentProcessingStep();
    step.setName("Cross-Reference Checks");
    step.setStatus(StepStatus.RUNNING);
    step.setStartedAt(Instant.now());

    String supplierId = request.getSupplier().getSupplierId();
    String vatId = request.getSupplier().getVatId();
    String commercialRegisterNo = request.getSupplier().getCommercialRegisterNo();
    String businessName = request.getSupplier().getBusinessName();
    String country = request.getSupplier().getBusinessAddress() != null
        ? request.getSupplier().getBusinessAddress().getCountry() : null;

    CrossReferenceResult result = new CrossReferenceResult();
    try {
      result.setFindings(runAllChecks(
          supplierId, vatId, commercialRegisterNo, businessName, country,
          request.getMatchedSuppliers()));
      step.setStatus(StepStatus.COMPLETED);
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Cross-reference check error: " + e.getMessage(), e);
      ValidationFinding errorFinding = new ValidationFinding(
          "FAILURE", "Cross-reference check failed: " + e.getMessage(), "system", RiskType.POLICY_COMPLIANCE);
      errorFinding.setRiskKind(RiskKind.AI_VALIDATION);
      result.getFindings().add(errorFinding);
      step.setStatus(StepStatus.FAILED);
      step.getLogLines().add(new AgentProcessingStep.LogLine(
          LogLineSeverity.ERROR, "Cross-reference check failed: " + e.getMessage()));
    }

    step.setCompletedAt(Instant.now());
    if (step.getStartedAt() != null) {
      step.setDurationMs(step.getCompletedAt().toEpochMilli() - step.getStartedAt().toEpochMilli());
    }

    for (ValidationFinding finding : result.getFindings()) {
      finding.setRiskKind(RiskKind.AI_VALIDATION);
      LogLineSeverity sev = LogLineSeverity.OK;
      if ("FAILURE".equals(finding.getSeverity())) {
        sev = LogLineSeverity.ERROR;
      } else if ("WARNING".equals(finding.getSeverity())) {
        sev = LogLineSeverity.WARNING;
      }
      step.getLogLines().add(new AgentProcessingStep.LogLine(sev, finding.getMessage()));
    }

    result.setProcessingStep(step);
    return result;
  }

  /** Builds the tool-result summary string returned to the LLM agent. */
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

  // ── Check orchestration ────────────────────────────────────────────────────

  /**
   * Runs all four checks using pre-populated matched suppliers from Step 1,
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

  /** Runs all three checks with a fresh ERP duplicate query. */
  public static List<ValidationFinding> runAllChecks(String supplierId, String vatId,
      String commercialRegisterNo, String businessName, String country) {
    List<ValidationFinding> findings = new ArrayList<>();
    findings.add(validateCompanyRegister(commercialRegisterNo, country));
    findings.add(validateVatId(vatId, country));
    findings.add(checkErpDuplicate(businessName, country, supplierId));
    return findings;
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
      return new ValidationFinding("WARNING",
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
    String severity = valid ? "PASSED" : "WARNING";
    return new ValidationFinding(severity, "Company Register: " + registerNo + " — " + detail, source, RiskType.POLICY_COMPLIANCE);
  }

  /**
   * Validates VAT ID format against country-specific regex patterns.
   * Extension point: replace with real EU VIES API call.
   */
  public static ValidationFinding validateVatId(String vatId, String country) {
    String source = "VAT Validation";
    if (vatId == null || vatId.trim().isEmpty()) {
      return new ValidationFinding("WARNING",
          "No VAT ID provided — may be exempt for sole traders or certain legal forms", source, RiskType.POLICY_COMPLIANCE);
    }
    String cleaned = vatId.trim().replace(" ", "").replace("-", "").toUpperCase();
    String countryCode = (country != null && !country.isEmpty()) ? country.toUpperCase() : "";

    String pattern = VAT_PATTERNS.get(countryCode);
    if (pattern == null && cleaned.length() >= 2) {
      pattern = VAT_PATTERNS.get(cleaned.substring(0, 2));
    }
    if (pattern == null) {
      return new ValidationFinding("PASSED",
          "VAT ID " + vatId + " — accepted (no country-specific format rule configured)", source, RiskType.POLICY_COMPLIANCE);
    }
    if (cleaned.matches(pattern)) {
      return new ValidationFinding("PASSED", "VAT ID " + vatId + " — confirmed", source, RiskType.POLICY_COMPLIANCE);
    }
    return new ValidationFinding("FAILURE",
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
      return new ValidationFinding("PASSED",
          "No duplicate in ERP — no similar suppliers found", source, RiskType.POLICY_COMPLIANCE);
    }
    long count = 0;
    for (Supplier s : matchedSuppliers) {
      if (supplierId == null || !supplierId.equals(s.getSupplierId())) {
        count++;
      }
    }
    if (count == 0) {
      return new ValidationFinding("PASSED",
          "No duplicate in ERP (distinct from existing supplier record)", source, RiskType.POLICY_COMPLIANCE);
    }
    return new ValidationFinding("WARNING",
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
      return new ValidationFinding("WARNING",
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
      return new ValidationFinding("PASSED",
          "No duplicate in ERP — no similar suppliers found", source, RiskType.POLICY_COMPLIANCE);
    }
    long count = 0;
    for (Supplier s : matches) {
      if (!s.getSupplierId().equals(excludeSupplierId)) {
        count++;
      }
    }
    if (count == 0) {
      return new ValidationFinding("PASSED",
          "No duplicate in ERP (distinct from existing supplier record)", source, RiskType.POLICY_COMPLIANCE);
    }
    return new ValidationFinding("WARNING",
        "Possible ERP duplicate: " + count + " similar supplier(s) found — manual review recommended",
        source, RiskType.POLICY_COMPLIANCE);
  }

}
