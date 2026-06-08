package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.bean;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import java.util.List;

import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentObjectType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.LogLineSeverity;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AuditTrailEntry;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingRequest;
import ch.ivyteam.ivy.environment.Ivy;

/**
 * Bean for the Supplier Onboarding Decline dialog (Screen 08).
 *
 * <p>Extends {@link ReadOnlySupplierDetailsBean} so the embedded
 * {@code SupplierDetails} and {@code SupplierAgentProcessingDetails}
 * composite components work in read-only mode.
 *
 * <p>Mirrors {@link SupplierOnboardingApprovalBean} — analysis is always
 * complete by the time the decline screen is shown.
 */
@ManagedBean
@ViewScoped
public class SupplierOnboardingDeclineBean extends ReadOnlySupplierDetailsBean {

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

  // ── Analysis state (always complete in the decline context) ───────────────

  public boolean isAnalysisComplete() {
    return true;
  }

  public boolean isHasFailedStep() {
    return false;
  }

  // ── Risk level helpers ────────────────────────────────────────────────────

  public String getDecisionBoxCssClass() {
    return "so-agent-decision-red";
  }

  public String getScoreCircleCssClass() {
    return "so-score-circle-red";
  }

  private static final String CMS_SAPD = "/Dialogs/com/axonivy/utils/smart/workflow/demo/erp/supplier/onboarding/components/SupplierAgentProcessingDetails/";

  public String getThresholdLabel() {
    return Ivy.cms().co(CMS_SAPD + "ThresholdRed");
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

  // ── Decline audit entry ───────────────────────────────────────────────────

  public AuditTrailEntry getDeclineEntry() {
    List<AuditTrailEntry> trail = request != null ? request.getAuditTrail() : null;
    if (trail == null || trail.isEmpty()) return null;
    for (int i = trail.size() - 1; i >= 0; i--) {
      AuditTrailEntry e = trail.get(i);
      if (e.getDeclineReasons() != null && !e.getDeclineReasons().isEmpty()) return e;
    }
    return trail.get(trail.size() - 1);
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
