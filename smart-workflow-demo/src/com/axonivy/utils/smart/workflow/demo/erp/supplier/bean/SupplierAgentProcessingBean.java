package com.axonivy.utils.smart.workflow.demo.erp.supplier.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ch.ivyteam.ivy.environment.Ivy;

import javax.el.ELContext;
import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import com.axonivy.utils.smart.workflow.demo.erp.assistant.AgentGuidance;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.DocumentExtractionResult;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.PolicyValidationResult;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.RiskScoreResult;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.ValidationRunner;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.SupplierPolicyRule;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep.LogLineSeverity;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep.StepStatus;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.RiskLevel;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.utils.IvyAdapterService;

@ManagedBean
@ViewScoped
public class SupplierAgentProcessingBean implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final String STEP_DOCUMENT_EXTRACTION    = "Document Extraction";
  public static final String STEP_POLICY_VALIDATION      = "Policy Validation";
  public static final String STEP_FINANCIAL_VALIDATION   = "Financial Validation";
  public static final String STEP_RISK_SCORE_CALCULATION = "Risk Score Calculation";

  private static final String CMS_SAPD = "/Dialogs/com/axonivy/utils/smart/workflow/demo/erp/supplier/onboarding/components/SupplierAgentProcessingDetails/";
  private static final String CMS_SAP  = "/Dialogs/com/axonivy/utils/smart/workflow/demo/erp/supplier/onboarding/SupplierAgentProcessing/";

  private static final String KEY_STEP_DOCUMENT_EXTRACTION    = "DOCUMENT_EXTRACTION";
  private static final String KEY_STEP_POLICY_VALIDATION      = "POLICY_VALIDATION";
  private static final String KEY_STEP_FINANCIAL_VALIDATION   = "FINANCIAL_VALIDATION";
  private static final String KEY_STEP_RISK_SCORE_CALCULATION = "RISK_SCORE_CALCULATION";

  private OnboardingRequest request;
  private SupplierAgentResponse agentResponse;
  private boolean initialized;

  // Per-step results — persisted across sequential AJAX calls (@ViewScoped)
  private DocumentExtractionResult extractionResult;
  private PolicyValidationResult policyValidationResult;
  private PolicyValidationResult financialValidationResult;
  private RiskScoreResult riskScoreResult;

  public void init() {
    if (initialized) {
      return;
    }
    FacesContext ctx = FacesContext.getCurrentInstance();
    this.request = (OnboardingRequest) ctx.getApplication()
        .evaluateExpressionGet(ctx, "#{data.request}", Object.class);
    this.agentResponse = (SupplierAgentResponse) ctx.getApplication()
        .evaluateExpressionGet(ctx, "#{data.agentResponse}", Object.class);
    initialized = true;
  }

  // ── Analysis state helpers ──────────────────────────────────────────────────

  public boolean isAnalysisComplete() {
    if (agentResponse == null || agentResponse.getProcessingSteps() == null) {
      return false;
    }
    List<AgentProcessingStep> steps = agentResponse.getProcessingSteps();
    if (steps.isEmpty()) {
      return false;
    }
    for (AgentProcessingStep step : steps) {
      if (step.getStatus() != StepStatus.COMPLETED) {
        return false;
      }
    }
    return true;
  }

  public boolean isAnalysisStarted() {
    // True only while analysis is actively in progress.
    // Returns false when not yet started, fully complete, or a step has failed
    // so the Analyze button becomes enabled again in all terminal states.
    if (isAnalysisComplete() || isHasFailedStep()) {
      return false;
    }
    if (agentResponse == null || agentResponse.getProcessingSteps() == null) {
      return false;
    }
    for (AgentProcessingStep step : agentResponse.getProcessingSteps()) {
      if (step.getStatus() != StepStatus.PENDING) {
        return true;
      }
    }
    return false;
  }

  public boolean isHasFailedStep() {
    if (agentResponse == null || agentResponse.getProcessingSteps() == null) {
      return false;
    }
    for (AgentProcessingStep step : agentResponse.getProcessingSteps()) {
      if (step.getStatus() == StepStatus.FAILED) {
        return true;
      }
    }
    return false;
  }

  // ── Risk level helpers ──────────────────────────────────────────────────────

  public String getRiskLevelCssClass() {
    RiskLevel level = getRiskLevel();
    if (level == RiskLevel.GREEN) {
      return "so-badge-green";
    } else if (level == RiskLevel.RED) {
      return "so-badge-red";
    }
    return "so-badge-yellow";
  }

  public String getDecisionBoxCssClass() {
    if (agentResponse != null && agentResponse.getRoutingDecision() != null) {
      String decision = agentResponse.getRoutingDecision().toUpperCase();
      if ("APPROVAL".equals(decision)) {
        return "so-success-banner";
      } else if ("DECLINE".equals(decision)) {
        return "so-decline-banner";
      }
    }
    return "";
  }

  public String getScoreCircleCssClass() {
    RiskLevel level = getRiskLevel();
    if (level == RiskLevel.GREEN) {
      return "so-score-circle-green";
    } else if (level == RiskLevel.RED) {
      return "so-score-circle-red";
    }
    return "so-score-circle-yellow";
  }

  public String getThresholdLabel() {
    RiskLevel level = getRiskLevel();
    if (level == RiskLevel.GREEN) {
      return Ivy.cms().co(CMS_SAPD + "ThresholdGreen");
    } else if (level == RiskLevel.YELLOW) {
      return Ivy.cms().co(CMS_SAPD + "ThresholdYellow");
    }
    return Ivy.cms().co(CMS_SAPD + "ThresholdRed");
  }

  private RiskLevel getRiskLevel() {
    if (agentResponse != null && agentResponse.getRiskScore() != null
        && agentResponse.getRiskScore().getLevel() != null) {
      return agentResponse.getRiskScore().getLevel();
    }
    return RiskLevel.YELLOW;
  }

  // ── Score bars ──────────────────────────────────────────────────────────────

  public String getScoreBarClass(int score) {
    if (score >= 70) {
      return "so-score-bar-green";
    } else if (score >= 40) {
      return "so-score-bar-yellow";
    }
    return "so-score-bar-red";
  }
  public String getScoreWidthClass(int score) {
    int rounded = (int) (Math.round(score / 5.0) * 5);
    rounded = Math.max(0, Math.min(100, rounded));
    return "so-w-" + rounded;
  }
  // ── Processing step icons ───────────────────────────────────────────────────

  public String getStepBubbleClass(AgentProcessingStep step) {
    if (step == null || step.getStatus() == null) {
      return "so-tl-bubble-pending";
    }
    return switch (step.getStatus()) {
      case COMPLETED -> "so-tl-bubble-completed";
      case FAILED    -> "so-tl-bubble-failed";
      default        -> "so-tl-bubble-pending";
    };
  }

  public String getStepStatusIcon(AgentProcessingStep step) {
    if (step == null || step.getStatus() == null) {
      return "ti-clock";
    }
      return switch (step.getStatus()) {
          case COMPLETED -> "ti-circle-check";
          case RUNNING -> "ti-loader";
          case FAILED -> "ti-circle-x";
          default -> "ti-clock";
      };
  }

  public String getStepStatusClass(AgentProcessingStep step) {
    if (step == null || step.getStatus() == null) {
      return "text-color-secondary";
    }
      return switch (step.getStatus()) {
          case COMPLETED -> "text-green-600";
          case RUNNING -> "text-blue-600";
          case FAILED -> "text-red-600";
          default -> "text-color-secondary";
      };
  }

  public String getStepRowClass(AgentProcessingStep step) {
    if (step == null || step.getStatus() == null) {
      return "so-checklist-item pending";
    }
      return switch (step.getStatus()) {
          case COMPLETED -> "so-checklist-item completed";
          case RUNNING -> "so-checklist-item running";
          case FAILED -> "so-checklist-item failed";
          default -> "so-checklist-item pending";
      };
  }

  public String getSeverityIcon(LogLineSeverity severity) {
    if (severity == null) {
      return "ti-circle-check";
    }
      return switch (severity) {
          case WARNING -> "ti-alert-triangle";
          case ERROR -> "ti-circle-x";
          default -> "ti-circle-check";
      };
  }

  public String getSeverityClass(LogLineSeverity severity) {
    if (severity == null) {
      return "so-log-line-ok";
    }
      return switch (severity) {
          case WARNING -> "so-log-line-warning";
          case ERROR -> "so-log-line-error";
          default -> "so-log-line-ok";
      };
  }

  // ── Duration ────────────────────────────────────────────────────────────────

  public String getFormattedDuration(AgentProcessingStep step) {
    if (step == null || step.getDurationMs() == null) {
      return "";
    }
    double seconds = step.getDurationMs() / 1000.0;
    if (seconds < 1.0) {
      return String.format("%.1fs", seconds);
    }
    return String.format("%.1fs", seconds);
  }

  // ── Policy rules ────────────────────────────────────────────────────────────

  public List<SupplierPolicyRule> getPolicyRules() {
    return ValidationRunner.loadPolicyRules();
  }

  // ── Findings ────────────────────────────────────────────────────────────────

  public String getFindingRowClass(ValidationFinding finding) {
    if (finding == null || finding.getSeverity() == null) {
      return "so-finding-green";
    }
      return switch (finding.getSeverity().toUpperCase()) {
          case "FAILURE" -> "so-finding-red";
          case "WARNING" -> "so-finding-yellow";
          default -> "so-finding-green";
      };
  }

  public String getFindingIcon(ValidationFinding finding) {
    if (finding == null || finding.getSeverity() == null) {
      return "ti-circle-check";
    }
      return switch (finding.getSeverity().toUpperCase()) {
          case "FAILURE" -> "ti-circle-x";
          case "WARNING" -> "ti-alert-triangle";
          default -> "ti-circle-check";
      };
  }

  // ── Routing button ──────────────────────────────────────────────────────────

  public String getContinueButtonLabel() {
    if (agentResponse == null || agentResponse.getRoutingDecision() == null) {
      return Ivy.cms().co(CMS_SAP + "ContinueButton");
    }
      return switch (agentResponse.getRoutingDecision().toUpperCase()) {
          case "APPROVAL" -> Ivy.cms().co(CMS_SAP + "ContinueToApprovalButton");
          case "CLARIFICATION" -> Ivy.cms().co(CMS_SAP + "ContinueToClarificationButton");
          case "DECLINE" -> Ivy.cms().co(CMS_SAP + "ViewDeclineButton");
          default -> Ivy.cms().co(CMS_SAP + "ContinueButton");
      };
  }

  public String getContinueButtonClass() {
    if (agentResponse == null || agentResponse.getRoutingDecision() == null) {
      return "ui-button-primary";
    }
      return switch (agentResponse.getRoutingDecision().toUpperCase()) {
          case "DECLINE" -> "ui-button-danger";
          default -> "ui-button-primary";
      };
  }

  // ── Agent guidance ───────────────────────────────────────────────────────────

  public List<AgentGuidance> getAgentGuidance() {
    return List.of(
        new AgentGuidance(
            "What does my risk score mean?",
            "explain the aggregate risk score shown on this page — what the number means, "
                + "how it is calculated from Financial Stability, Policy Compliance, "
                + "Certificate Validity scores, and what the GREEN/YELLOW/RED thresholds are"),
        new AgentGuidance(
            "Why did a validation step fail?",
            "look at the failed or warning validation findings on this page and explain "
                + "what each issue means, why it matters for supplier onboarding, "
                + "and what corrective action the supplier should take"),
        new AgentGuidance(
            "What are the next steps?",
            "based on the routing decision shown (APPROVAL, CLARIFICATION, or DECLINE), "
                + "explain what happens next in the onboarding workflow — "
                + "who reviews it, what the supplier needs to provide, and expected timelines"),
        new AgentGuidance(
            "What was checked in each step?",
            "describe what each of the four validation steps checks: "
                + "Document Extraction (parsing uploaded documents), "
                + "Policy Validation (checking onboarding rules and required fields), "
                + "Financial Validation (checking financial health indicators against financial rules), "
                + "and Risk Score Calculation (computing the aggregate risk score)"));
  }

  // ── Actions ──────────────────────────────────────────────────────────────────────────

  /**
   * Initialises the agentResponse with 4 PENDING steps and syncs it to
   * #{data.agentResponse}. The client then calls runStep1 via remoteCommand.
   */
  public void startAnalysis() {
    extractionResult          = null;
    policyValidationResult    = null;
    financialValidationResult = null;
    riskScoreResult           = null;

    SupplierAgentResponse response = new SupplierAgentResponse();
    List<AgentProcessingStep> steps = new ArrayList<>();
    steps.add(createPendingStep(KEY_STEP_DOCUMENT_EXTRACTION,    Ivy.cms().co(CMS_SAPD + "StepDocumentExtraction")));
    steps.add(createPendingStep(KEY_STEP_POLICY_VALIDATION,      Ivy.cms().co(CMS_SAPD + "StepPolicyValidation")));
    steps.add(createPendingStep(KEY_STEP_FINANCIAL_VALIDATION,   Ivy.cms().co(CMS_SAPD + "StepFinancialValidation")));
    steps.add(createPendingStep(KEY_STEP_RISK_SCORE_CALCULATION, Ivy.cms().co(CMS_SAPD + "StepRiskScoreCalculation")));
    response.setProcessingSteps(steps);
    this.agentResponse = response;
    syncAgentResponse(response);
  }

  /** Step 1 — Document extraction. */
  public void runStep1() {
    AgentProcessingStep step = getStep(0);
    try {
      step.setStatus(StepStatus.RUNNING);
      Map<String, Object> params = new LinkedHashMap<>();
      params.put("supplier", request.getSupplier());
      Map<String, Object> result = IvyAdapterService.startSubProcessInApplication(
          "supplierDocumentExtractAgent(com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier)",
          params);
      this.extractionResult = result != null
          ? (DocumentExtractionResult) result.get("extractionResult") : null;
      finalizeStep(step, extractionResult != null ? extractionResult.getProcessingStep() : null);
    } catch (Exception e) {
      step.setStatus(StepStatus.FAILED);
      addStepErrorMessage(KEY_STEP_DOCUMENT_EXTRACTION, e);
    }
  }

  /** Step 2 — Policy validation. */
  public void runStep2() {
    AgentProcessingStep step = getStep(1);
    try {
      step.setStatus(StepStatus.RUNNING);
      Map<String, Object> params = new LinkedHashMap<>();
      params.put("supplier", request.getSupplier());
      params.put("documents", extractionResult);
      Map<String, Object> result = IvyAdapterService.startSubProcessInApplication(
          "validateAgainstPolicy(com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier,com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.DocumentExtractionResult)",
          params);
      this.policyValidationResult = result != null
          ? (PolicyValidationResult) result.get("policyValidationResult") : null;
      finalizeStep(step, policyValidationResult != null ? policyValidationResult.getProcessingStep() : null);
    } catch (Exception e) {
      step.setStatus(StepStatus.FAILED);
      addStepErrorMessage(KEY_STEP_POLICY_VALIDATION, e);
    }
  }

  /** Step 3 — Financial policy validation. */
  public void runStep3() {
    AgentProcessingStep step = getStep(2);
    try {
      step.setStatus(StepStatus.RUNNING);
      Map<String, Object> params = new LinkedHashMap<>();
      params.put("supplier", request.getSupplier());
      params.put("documents", extractionResult);
      Map<String, Object> result = IvyAdapterService.startSubProcessInApplication(
          "validateFinancialPolicy(com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier,com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.DocumentExtractionResult)",
          params);
      this.financialValidationResult = result != null
          ? (PolicyValidationResult) result.get("financialValidationResult") : null;
      finalizeStep(step, financialValidationResult != null ? financialValidationResult.getProcessingStep() : null);
    } catch (Exception e) {
      step.setStatus(StepStatus.FAILED);
      addStepErrorMessage(KEY_STEP_FINANCIAL_VALIDATION, e);
    }
  }

  /** Step 4 — Risk assessment + final assembly. */
  public void runStep4() {
    AgentProcessingStep step = getStep(3);
    try {
      step.setStatus(StepStatus.RUNNING);
      Integer annualVolumeEur = request.getExpectedAnnualVolume() != null
          ? request.getExpectedAnnualVolume().intValue() : null;
      Map<String, Object> params = new LinkedHashMap<>();
      params.put("policyResult", policyValidationResult);
      params.put("financialResult", financialValidationResult);
      params.put("annualVolumeEur", annualVolumeEur);
      Map<String, Object> result = IvyAdapterService.startSubProcessInApplication(
          "callRiskAssessment(com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.PolicyValidationResult,com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.PolicyValidationResult,Integer,String)",
          params);
      this.riskScoreResult = result != null
          ? (RiskScoreResult) result.get("riskScoreResult") : null;
      finalizeStep(step, riskScoreResult != null ? riskScoreResult.getProcessingStep() : null);

      // Update the existing agentResponse in-place so the placeholder steps
      // (which already carry log lines from each runStepN call) are preserved.
      agentResponse.setRiskScore(riskScoreResult != null && riskScoreResult.getRiskScore() != null
          ? riskScoreResult.getRiskScore() : null);
      agentResponse.setRoutingDecision(riskScoreResult != null
          ? riskScoreResult.getRoutingDecision() : "CLARIFICATION");
      List<ValidationFinding> findings = new ArrayList<>();
      if (policyValidationResult != null && policyValidationResult.getFindings() != null) {
        policyValidationResult.getFindings().stream()
            .filter(f -> "FAILURE".equalsIgnoreCase(f.getSeverity()) || "WARNING".equalsIgnoreCase(f.getSeverity()))
            .forEach(findings::add);
      }
      if (financialValidationResult != null && financialValidationResult.getFindings() != null) {
        financialValidationResult.getFindings().stream()
            .filter(f -> "FAILURE".equalsIgnoreCase(f.getSeverity()) || "WARNING".equalsIgnoreCase(f.getSeverity()))
            .filter(f -> findings.stream().noneMatch(existing -> java.util.Objects.equals(existing.getMessage(), f.getMessage())))
            .forEach(findings::add);
      }
      agentResponse.setValidationFindings(findings);
      String orchestrationSummary = riskScoreResult != null && riskScoreResult.getRiskScore() != null
          ? "Risk score: " + riskScoreResult.getRiskScore().getAggregate()
              + ". Routing: " + riskScoreResult.getRoutingDecision()
          : "Analysis complete.";
      agentResponse.setFeedback(orchestrationSummary);
      syncAgentResponse(agentResponse);
    } catch (Exception e) {
      step.setStatus(StepStatus.FAILED);
      addStepErrorMessage(KEY_STEP_RISK_SCORE_CALCULATION, e);
    }
  }

  private AgentProcessingStep createPendingStep(String stepKey, String displayName) {
    AgentProcessingStep step = new AgentProcessingStep();
    step.setStepKey(stepKey);
    step.setName(displayName);
    step.setStatus(StepStatus.PENDING);
    return step;
  }

  /**
   * Copies timing and log lines from the sub-process step (if non-null) into
   * the placeholder step held in agentResponse.processingSteps.
   */
  private void finalizeStep(AgentProcessingStep placeholder, AgentProcessingStep source) {
    if (source != null) {
      placeholder.setStatus(source.getStatus());
      placeholder.setStartedAt(source.getStartedAt());
      placeholder.setCompletedAt(source.getCompletedAt());
      placeholder.setDurationMs(source.getDurationMs());
      if (source.getLogLines() != null) {
        placeholder.getLogLines().addAll(source.getLogLines());
      }
    } else {
      placeholder.setStatus(StepStatus.COMPLETED);
    }
  }

  private AgentProcessingStep getStep(int index) {
    if (agentResponse == null || agentResponse.getProcessingSteps() == null
        || agentResponse.getProcessingSteps().size() <= index) {
      return createPendingStep("STEP_" + (index + 1), "Step " + (index + 1));
    }
    return agentResponse.getProcessingSteps().get(index);
  }

  private void syncAgentResponse(SupplierAgentResponse response) {
    FacesContext fc = FacesContext.getCurrentInstance();
    fc.getApplication().getExpressionFactory()
        .createValueExpression(fc.getELContext(), "#{data.agentResponse}", Object.class)
        .setValue(fc.getELContext(), response);
  }

  private void addStepErrorMessage(String stepKey, Exception e) {
    String displayName = steps.stream()
        .filter(s -> stepKey.equals(s.getStepKey()))
        .map(AgentProcessingStep::getName)
        .findFirst()
        .orElse(stepKey);
    String detail = e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.";
    FacesContext.getCurrentInstance().addMessage(null,
        new FacesMessage(FacesMessage.SEVERITY_ERROR, displayName + " failed", detail));
  }

  public void close() {
    FacesContext fc = FacesContext.getCurrentInstance();
    ELContext el = fc.getELContext();
    Application app = fc.getApplication();
    app.getExpressionFactory()
        .createMethodExpression(el, "#{logic.close}", null, new Class<?>[0])
        .invoke(el, new Object[0]);
  }

  // ── Getters ─────────────────────────────────────────────────────────────────

  public OnboardingRequest getRequest() {
    return request;
  }

  public SupplierAgentResponse getAgentResponse() {
    return agentResponse;
  }
}
