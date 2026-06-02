package com.axonivy.utils.smart.workflow.demo.erp.supplier.bean;

import java.time.Instant;

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.faces.application.Application;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentObjectType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep.LogLineSeverity;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ApprovalDecision;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ApprovalStage;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AuditActorType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AuditEntryType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AuditTrailEntry;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.RiskLevel;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.SupplierOnboardingApproval.SupplierOnboardingApprovalData;
import ch.ivyteam.ivy.environment.Ivy;

/**
 * Bean for the Supervisor/QM-ISM Approval dialog.
 *
 * <p>Extends {@link ReadOnlySupplierDetailsBean} so that the embedded
 * {@code SupplierDetails} composite component can read supplier data
 * (countries, legal forms, documents) in read-only mode.
 *
 * <p>Also provides display utilities for the embedded
 * {@code SupplierAgentProcessingDetails} composite component (risk-score
 * colours, step icons, finding styles, etc.).  These are copied from
 * {@link SupplierAgentProcessingBean} — analysis is always complete by the
 * time approval runs, so the action methods are not needed here.
 */
@ManagedBean
@ViewScoped
public class SupplierOnboardingApprovalBean extends ReadOnlySupplierDetailsBean {

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

  // ── Close action ───────────────────────────────────────────────────────────

  public void close() {
    FacesContext ctx = FacesContext.getCurrentInstance();
    SupplierOnboardingApprovalData data = (SupplierOnboardingApprovalData) ctx.getApplication()
        .evaluateExpressionGet(ctx, "#{data}", Object.class);

    if (data.getApprovalDecision() == null) {
      data.setApprovalDecision(ApprovalDecision.APPROVED);
    }
    if (data.getApprovalActor() == null || data.getApprovalActor().isBlank()) {
      data.setApprovalActor(Ivy.session().getSessionUser().getName());
    }
    if (data.getApprovalAt() == null || data.getApprovalAt().isBlank()) {
      data.setApprovalAt(Instant.now().toString());
    }
    data.setAuditEntry(buildAuditEntry(
        data.getApprovalDecision(), data.getApprovalComment(),
        data.getApprovalActor(), data.getApprovalAt(), data.getApprovalStage()));

    ELContext el = ctx.getELContext();
    Application app = ctx.getApplication();
    MethodExpression closeMethod = app.getExpressionFactory()
        .createMethodExpression(el, "#{logic.close}", null, new Class<?>[0]);
    closeMethod.invoke(el, new Object[0]);
  }

  // ── Audit entry builder ───────────────────────────────────────────────────

  public AuditTrailEntry buildAuditEntry(ApprovalDecision decision, String comment,
      String actor, String timestamp, ApprovalStage stage) {
    AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(timestamp != null ? timestamp : Instant.now().toString());
    entry.setActor(actor);
    entry.setActorType(AuditActorType.USER);
    entry.setEntryType(AuditEntryType.APPROVAL);
    String stageName = stage != null ? stage.name() : "";
    entry.setAction(stageName + " approval decision");
    entry.setTechnicalDetail(null);
    entry.setStage(stage);
    entry.setDecision(decision);
    entry.setComment(comment);
    return entry;
  }

  // ── DocumentUploader contract (read-only — uploads disabled in this context) ──

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

  // ── Analysis state (always complete in the approval context) ─────────────

  public boolean isAnalysisComplete() {
    return true;
  }

  public boolean isHasFailedStep() {
    return false;
  }

  // ── Risk level helpers ────────────────────────────────────────────────────

  public String getDecisionBoxCssClass() {
    RiskLevel level = getRiskLevel();
    if (level == RiskLevel.GREEN) return "so-agent-decision-green";
    if (level == RiskLevel.RED)   return "so-agent-decision-red";
    return "so-agent-decision-yellow";
  }

  public String getScoreCircleCssClass() {
    RiskLevel level = getRiskLevel();
    if (level == RiskLevel.GREEN) return "so-score-circle-green";
    if (level == RiskLevel.RED)   return "so-score-circle-red";
    return "so-score-circle-yellow";
  }

  private static final String CMS_SAPD = "/Dialogs/com/axonivy/utils/smart/workflow/demo/erp/supplier/onboarding/components/SupplierAgentProcessingDetails/";

  public String getThresholdLabel() {
    RiskLevel level = getRiskLevel();
    if (level == RiskLevel.GREEN)  return Ivy.cms().co(CMS_SAPD + "ThresholdGreen");
    if (level == RiskLevel.YELLOW) return Ivy.cms().co(CMS_SAPD + "ThresholdYellow");
    return Ivy.cms().co(CMS_SAPD + "ThresholdRed");
  }

  private RiskLevel getRiskLevel() {
    if (agentResponse != null && agentResponse.getRiskScore() != null
        && agentResponse.getRiskScore().getLevel() != null) {
      return agentResponse.getRiskScore().getLevel();
    }
    return RiskLevel.YELLOW;
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

  // ── Getters ───────────────────────────────────────────────────────────────

  public SupplierAgentResponse getAgentResponse() {
    return agentResponse;
  }
}
