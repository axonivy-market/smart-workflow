package com.axonivy.utils.smart.workflow.demo.erp.supplier.bean;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentObjectType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep.LogLineSeverity;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ValidationFinding;

import ch.ivyteam.ivy.environment.Ivy;

/**
 * Bean for the Supplier Onboarding Completion dialog (Screen 09).
 *
 * <p>Extends {@link ReadOnlySupplierDetailsBean} so the embedded
 * {@code SupplierDetails} and {@code SupplierAgentProcessingDetails}
 * composite components work in read-only mode.
 *
 * <p>Mirrors {@link SupplierOnboardingDeclineBean} — analysis is always
 * complete by the time the completion screen is shown.
 */
@ManagedBean
@ViewScoped
public class SupplierOnboardingCompletionBean extends ReadOnlySupplierDetailsBean {

  private static final long serialVersionUID = 1L;

  private SupplierAgentResponse agentResponse;

  // ── Initialisation ────────────────────────────────────────────────────────

  @Override
  public void init(OnboardingRequest request) {
    super.init(request);
    if (agentResponse == null) {
      FacesContext ctx = FacesContext.getCurrentInstance();
      agentResponse = (SupplierAgentResponse) ctx.getApplication()
          .evaluateExpressionGet(ctx, "#{data.agentResponse}", Object.class);
    }
  }

  // ── DocumentUploader contract ─────────────────────────────────────────────

  @Override
  public String getObjectId() {
    return request != null && request.getSupplier() != null
        ? request.getSupplier().getSupplierId()
        : null;
  }

  @Override
  public LegalDocumentObjectType getObjectType() {
    return LegalDocumentObjectType.SUPPLIER;
  }

  // ── Analysis state (always complete in the completion context) ────────────

  public boolean isAnalysisComplete() {
    return true;
  }

  public boolean isHasFailedStep() {
    return false;
  }

  // ── Risk level helpers ────────────────────────────────────────────────────

  public String getDecisionBoxCssClass() {
    return "so-success-banner";
  }

  public String getScoreCircleCssClass() {
    return "so-score-circle-green";
  }

  private static final String CMS_SAPD = "/Dialogs/com/axonivy/utils/smart/workflow/demo/erp/supplier/onboarding/components/SupplierAgentProcessingDetails/";

  public String getThresholdLabel() {
    return Ivy.cms().co(CMS_SAPD + "ThresholdAboveMin");
  }

  // ── Score bars ────────────────────────────────────────────────────────────

  public String getScoreBarClass(int score) {
    if (score >= 70) return "so-score-bar-green";
    if (score >= 40) return "so-score-bar-yellow";
    return "so-score-bar-red";
  }

  // ── Processing step icons ─────────────────────────────────────────────────

  public String getStepBubbleClass(AgentProcessingStep step) {
    if (step == null || step.getStatus() == null) return "so-tl-bubble-pending";
    return switch (step.getStatus()) {
      case COMPLETED -> "so-tl-bubble-completed";
      case FAILED    -> "so-tl-bubble-failed";
      default        -> "so-tl-bubble-pending";
    };
  }

  public String getStepStatusIcon(AgentProcessingStep step) {
    if (step == null || step.getStatus() == null) return "ti-clock";
    return switch (step.getStatus()) {
      case COMPLETED -> "ti-circle-check";
      case RUNNING   -> "ti-loader";
      case FAILED    -> "ti-circle-x";
      default        -> "ti-clock";
    };
  }

  public String getStepRowClass(AgentProcessingStep step) {
    if (step == null || step.getStatus() == null) return "so-checklist-item pending";
    return switch (step.getStatus()) {
      case COMPLETED -> "so-checklist-item completed";
      case RUNNING   -> "so-checklist-item running";
      case FAILED    -> "so-checklist-item failed";
      default        -> "so-checklist-item pending";
    };
  }

  public String getSeverityIcon(LogLineSeverity severity) {
    if (severity == null) return "ti-circle-check";
    return switch (severity) {
      case WARNING -> "ti-alert-triangle";
      case ERROR   -> "ti-circle-x";
      default      -> "ti-circle-check";
    };
  }

  public String getSeverityClass(LogLineSeverity severity) {
    if (severity == null) return "so-log-line-ok";
    return switch (severity) {
      case WARNING -> "so-log-line-warning";
      case ERROR   -> "so-log-line-error";
      default      -> "so-log-line-ok";
    };
  }

  // ── Duration ──────────────────────────────────────────────────────────────

  public String getFormattedDuration(AgentProcessingStep step) {
    if (step == null || step.getDurationMs() == null) return "";
    return String.format("%.1fs", step.getDurationMs() / 1000.0);
  }

  // ── Findings ──────────────────────────────────────────────────────────────

  public String getFindingRowClass(ValidationFinding finding) {
    if (finding == null || finding.getSeverity() == null) return "so-finding-green";
    return switch (finding.getSeverity().toUpperCase()) {
      case "FAILURE" -> "so-finding-red";
      case "WARNING" -> "so-finding-yellow";
      default        -> "so-finding-green";
    };
  }

  public String getFindingIcon(ValidationFinding finding) {
    if (finding == null || finding.getSeverity() == null) return "ti-circle-check";
    return switch (finding.getSeverity().toUpperCase()) {
      case "FAILURE" -> "ti-circle-x";
      case "WARNING" -> "ti-alert-triangle";
      default        -> "ti-circle-check";
    };
  }

  // ── Audit timeline ────────────────────────────────────────────────────────

  public String getAuditBubbleClass(com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AuditActorType actorType) {
    if (actorType == null) return "so-tl-bubble-completed";
    return switch (actorType) {
      case APPROVER -> "so-tl-bubble-completed";
      case AGENT    -> "so-tl-bubble-running";
      case USER     -> "so-tl-bubble-completed";
      default       -> "so-tl-bubble-completed";
    };
  }

  public String getAuditIcon(com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AuditActorType actorType) {
    if (actorType == null) return "ti-cpu";
    return switch (actorType) {
      case APPROVER -> "ti-user-check";
      case AGENT    -> "ti-robot";
      case USER     -> "ti-user";
      default       -> "ti-cpu";
    };
  }

  // ── Notification avatar initials ──────────────────────────────────────────

  public String getRecipientInitials(String recipientName) {
    if (recipientName == null || recipientName.isBlank()) return "?";
    String[] parts = recipientName.trim().split("\\s+");
    if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
    return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
  }

  // ── Getters ───────────────────────────────────────────────────────────────

  public SupplierAgentResponse getAgentResponse() {
    return agentResponse;
  }
}
