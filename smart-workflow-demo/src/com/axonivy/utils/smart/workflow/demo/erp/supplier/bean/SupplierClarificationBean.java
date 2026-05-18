package com.axonivy.utils.smart.workflow.demo.erp.supplier.bean;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentObjectType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AgentProcessingStep.LogLineSeverity;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ClarificationItem;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ClarificationProblemType;
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

  /** Typed clarification items loaded during init (avoids repeated EL reads). */
  private final java.util.List<ClarificationItem> clarificationItems = new java.util.ArrayList<>();

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
    }
    if (clarificationItems.isEmpty()) {
      Object raw = ctx.getApplication()
          .evaluateExpressionGet(ctx, "#{data.clarificationItems}", Object.class);
      if (raw instanceof java.util.List<?> list) {
        for (Object element : list) {
          switch (element) {
            case ClarificationItem ci -> clarificationItems.add(ci);
            case String s -> clarificationItems.add(
                new ClarificationItem(s, ClarificationProblemType.OTHER, null));
            default -> { /* skip unknown elements */ }
          }
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
   * Also sets {@code pendingDocumentType} if the item has a documentTypeKey
   * so that the SingleLegalDocument upload widget is pre-targeted.
   */
  public void toggleResolve(int index) {
    if (expandedItemIndex == index) {
      expandedItemIndex = -1;
    } else {
      expandedItemIndex = index;
      if (index >= 0 && index < clarificationItems.size()) {
        ClarificationItem item = clarificationItems.get(index);
        if (item.getDocumentTypeKey() != null) {
          setPendingDocumentType(item.getDocumentTypeKey());
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
   * Marks the corresponding clarification item as resolved.
   */
  @Override
  public void onDocumentSaved(com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocument doc) {
    super.onDocumentSaved(doc);
    if (expandedItemIndex >= 0 && expandedItemIndex < clarificationItems.size()) {
      ClarificationItem item = clarificationItems.get(expandedItemIndex);
      item.setResolved(true);
      if (item.getFinding() != null) {
        item.getFinding().setResolved(true);
      }
    }
  }

  // ── Getters ───────────────────────────────────────────────────────────────

  /**
   * Returns clarification items as a typed array so the JSF EL validator
   * can infer the element type (List&lt;T&gt; generics are not visible to EL).
   */
  public ClarificationItem[] getClarificationItemsArray() {
    return clarificationItems.toArray(ClarificationItem[]::new);
  }

  /**
   * Marks the clarification item at the given index as resolved.
   * Called by ProblemExplanation component after explanation is saved.
   */
  public void markItemResolved(int index) {
    if (index >= 0 && index < clarificationItems.size()) {
      ClarificationItem item = clarificationItems.get(index);
      item.setResolved(true);
      if (item.getFinding() != null) {
        item.getFinding().setResolved(true);
      }
    }
    expandedItemIndex = -1;
  }

  /**
   * Returns true when all clarification items have been resolved.
   * Used to switch the footer Submit button to a success style.
   */
  public boolean isAllItemsResolved() {
    return !clarificationItems.isEmpty()
        && clarificationItems.stream().allMatch(ClarificationItem::isResolved);
  }

  public SupplierAgentResponse getAgentResponse() {
    return agentResponse;
  }
}
