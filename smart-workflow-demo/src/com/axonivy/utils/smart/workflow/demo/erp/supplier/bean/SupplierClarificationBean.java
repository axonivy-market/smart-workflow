package com.axonivy.utils.smart.workflow.demo.erp.supplier.bean;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentObjectType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.SupplierRiskScore;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep.LogLineSeverity;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.RiskLevel;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ValidationFinding;

import ch.ivyteam.ivy.environment.Ivy;

/**
 * Bean for the Supplier Clarification dialog (Screen 07).
 *
 * <p>Extends {@link ReadOnlySupplierDetailsBean} so the embedded
 * {@code SupplierDetails} and {@code SupplierAgentProcessingDetails}
 * composite components work in read-only mode on the detail tabs.
 */
@ManagedBean
@ViewScoped
public class SupplierClarificationBean extends ReadOnlySupplierDetailsBean {

  private static final long serialVersionUID = 1L;

  private SupplierAgentResponse agentResponse;

  /** Snapshot of the agent's original sub-scores, captured once during {@link #init}. */
  private int originalFinancialStability;
  private int originalPolicyCompliance;
  private int originalCertValidity;

  /** Non-PASSED findings loaded from request.policyValidationFindings during init. */
  private final java.util.List<ValidationFinding> clarificationFindings = new java.util.ArrayList<>();

  /** Index of the currently expanded resolve panel (-1 = none). */
  private int expandedItemIndex = -1;

  // ── Initialisation ────────────────────────────────────────────────────────

  @Override
  public void init(OnboardingRequest request) {
    super.init(request);
    FacesContext ctx = FacesContext.getCurrentInstance();
    if (agentResponse == null) {
      agentResponse = (SupplierAgentResponse) ctx.getApplication()
          .evaluateExpressionGet(ctx, "#{data.agentResponse}", Object.class);
      if (agentResponse != null && agentResponse.getRiskScore() != null) {
        SupplierRiskScore rs = agentResponse.getRiskScore();
        originalFinancialStability = rs.getFinancialStability();
        originalPolicyCompliance   = rs.getPolicyCompliance();
        originalCertValidity       = rs.getCertValidity();
      }
    }
    if (clarificationFindings.isEmpty() && request != null
        && request.getPolicyValidationFindings() != null) {
      for (ValidationFinding f : request.getPolicyValidationFindings()) {
        if (f.getSeverity() == null || !"PASSED".equalsIgnoreCase(f.getSeverity())) {
          clarificationFindings.add(f);
        }
      }
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

  // ── Analysis state (always complete — showing last agent result) ──────────

  public boolean isAnalysisComplete() {
    return true;
  }

  public boolean isHasFailedStep() {
    return false;
  }

  // ── Risk level helpers (YELLOW context) ──────────────────────────────────

  public String getBannerModifierClass() {
    return switch (getRiskLevel()) {
      case GREEN -> "so-success-banner";
      case RED   -> "so-decline-banner";
      default    -> "";
    };
  }

  public String getBannerBadgeClass() {
    return switch (getRiskLevel()) {
      case GREEN -> "so-badge-green";
      case RED   -> "so-badge-red";
      default    -> "so-badge-yellow";
    };
  }

  public String getBannerBadgeLabel() {
    return switch (getRiskLevel()) {
      case GREEN -> Ivy.cms().co(CMS_COMPL + "RiskScoreBadge");
      case RED   -> Ivy.cms().co(CMS_DECL  + "RiskScoreBadge");
      default    -> Ivy.cms().co(CMS_SC    + "RiskScoreBadge");
    };
  }

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
  private static final String CMS_SC   = "/Dialogs/com/axonivy/utils/smart/workflow/demo/erp/supplier/onboarding/SupplierClarification/";
  private static final String CMS_COMPL = "/Dialogs/com/axonivy/utils/smart/workflow/demo/erp/supplier/onboarding/SupplierOnboardingCompletion/";
  private static final String CMS_DECL  = "/Dialogs/com/axonivy/utils/smart/workflow/demo/erp/supplier/onboarding/SupplierOnboardingDecline/";

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

  // ── Resolve / expand ─────────────────────────────────────────────────────

  /**
   * Toggles the resolve panel for the given item index.
   * Also sets {@code pendingDocumentType} if the finding has a documentTypeKey
   * so that the SingleLegalDocument upload widget is pre-targeted.
   */
  public void toggleResolve(int index) {
    if (expandedItemIndex == index) {
      expandedItemIndex = -1;
    } else {
      expandedItemIndex = index;
      if (index >= 0 && index < clarificationFindings.size()) {
        ValidationFinding finding = clarificationFindings.get(index);
        if (finding.getDocumentTypeKey() != null) {
          setPendingDocumentType(finding.getDocumentTypeKey());
        }
      }
    }
  }

  public boolean isItemExpanded(int index) {
    return expandedItemIndex == index;
  }

  public int getExpandedItemIndex() {
    return expandedItemIndex;
  }

  public void setExpandedItemIndex(int expandedItemIndex) {
    this.expandedItemIndex = expandedItemIndex;
  }

  /**
   * Called when a document is uploaded via the per-item SingleLegalDocument component.
   * Marks the corresponding finding as resolved.
   */
  @Override
  public void onDocumentSaved(com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocument doc) {
    super.onDocumentSaved(doc);
    if (expandedItemIndex >= 0 && expandedItemIndex < clarificationFindings.size()) {
      clarificationFindings.get(expandedItemIndex).setResolved(true);
      recalculateScore();
    }
  }

  // ── Getters ───────────────────────────────────────────────────────────────

  /**
   * Returns clarification findings as a typed array so the JSF EL validator
   * can infer the element type (List&lt;T&gt; generics are not visible to EL).
   */
  public ValidationFinding[] getValidationFindingsArray() {
    return clarificationFindings.toArray(ValidationFinding[]::new);
  }

  /**
   * Marks the finding at the given index as resolved.
   * Called by ProblemExplanation component after explanation is saved.
   */
  public void markItemResolved(int index) {
    if (index >= 0 && index < clarificationFindings.size()) {
      clarificationFindings.get(index).setResolved(true);
      recalculateScore();
    }
    expandedItemIndex = -1;
  }

  /**
   * Boosts each sub-score proportionally to the fraction of resolved findings,
   * then recomputes the aggregate and risk level on the bean's cached {@code agentResponse}.
   *
   * <p>Always computed from the original scores captured during {@link #init} to avoid
   * compounding boosts across multiple resolve actions.
   */
  private void recalculateScore() {
    if (agentResponse == null || agentResponse.getRiskScore() == null || clarificationFindings.isEmpty()) {
      return;
    }
    long resolved = clarificationFindings.stream().filter(ValidationFinding::isResolved).count();
    double ratio = (double) resolved / clarificationFindings.size();

    SupplierRiskScore score = agentResponse.getRiskScore();
    int newFinancial = boost(originalFinancialStability, ratio);
    int newPolicy    = boost(originalPolicyCompliance,   ratio);
    int newCert      = boost(originalCertValidity,       ratio);
    int newAggregate = (newFinancial + newPolicy + newCert) / 3;

    score.setFinancialStability(newFinancial);
    score.setPolicyCompliance(newPolicy);
    score.setCertValidity(newCert);
    score.setAggregate(newAggregate);
    score.setLevel(RiskLevel.fromScore(newAggregate));
  }

  private static int boost(int original, double resolvedRatio) {
    return Math.min(100, (int) Math.round(original + (100 - original) * resolvedRatio));
  }

  /**
   * Returns true when all clarification findings have been resolved.
   * Used to switch the footer Submit button to a success style.
   */
  public boolean isAllItemsResolved() {
    return !clarificationFindings.isEmpty()
        && clarificationFindings.stream().allMatch(ValidationFinding::isResolved);
  }

  public SupplierAgentResponse getAgentResponse() {
    return agentResponse;
  }
}
