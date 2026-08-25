package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces;

import java.util.Optional;

import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.agent.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.LogLineSeverity;

import jakarta.faces.context.FacesContext;

public interface AgentResultView {

  String CMS_ONBOARDING              = "/Dialogs/com/axonivy/utils/smart/workflow/demo/erp/supplier/onboarding/";
  String CMS_AGENT_PROCESSING        = CMS_ONBOARDING + "SupplierAgentProcessing/";
  String CMS_AGENT_PROCESSING_DETAIL = CMS_ONBOARDING + "components/SupplierAgentProcessingDetails/";
  String CMS_CLARIFICATION           = CMS_ONBOARDING + "SupplierClarification/";
  String CMS_COMPLETION              = CMS_ONBOARDING + "SupplierOnboardingCompletion/";
  String CMS_DECLINE                 = CMS_ONBOARDING + "SupplierOnboardingDecline/";
  SupplierAgentResponse getAgentResponse();

  static SupplierAgentResponse resolveAgentResponse() {
    FacesContext ctx = FacesContext.getCurrentInstance();
    return (SupplierAgentResponse) ctx.getApplication()
        .evaluateExpressionGet(ctx, "#{data.agentResponse}", Object.class);
  }

  default boolean isAnalysisComplete() {
    return true;
  }

  default boolean isHasFailedStep() {
    return false;
  }

  default String getScoreBarClass(int score) {
    if (score >= 70) return "so-score-bar-green";
    if (score >= 40) return "so-score-bar-yellow";
    return "so-score-bar-red";
  }

  default String getStepBubbleClass(AgentProcessingStep step) {
    return switch (step == null ? null : step.getStatus()) {
      case COMPLETED     -> "so-tl-bubble-completed";
      case FAILED        -> "so-tl-bubble-failed";
      case null, default -> "so-tl-bubble-pending";
    };
  }

  default String getStepStatusIcon(AgentProcessingStep step) {
    return switch (step == null ? null : step.getStatus()) {
      case COMPLETED     -> "ti-circle-check";
      case RUNNING       -> "ti-loader";
      case FAILED        -> "ti-circle-x";
      case null, default -> "ti-clock";
    };
  }

  default String getStepRowClass(AgentProcessingStep step) {
    return switch (step == null ? null : step.getStatus()) {
      case COMPLETED     -> "so-checklist-item completed";
      case RUNNING       -> "so-checklist-item running";
      case FAILED        -> "so-checklist-item failed";
      case null, default -> "so-checklist-item pending";
    };
  }

  default String getSeverityIcon(LogLineSeverity severity) {
    return switch (severity) {
      case WARNING       -> "ti-alert-triangle";
      case ERROR         -> "ti-circle-x";
      case null, default -> "ti-circle-check";
    };
  }

  default String getSeverityClass(LogLineSeverity severity) {
    return switch (severity) {
      case WARNING       -> "so-log-line-warning";
      case ERROR         -> "so-log-line-error";
      case null, default -> "so-log-line-ok";
    };
  }

  default String getFormattedDuration(AgentProcessingStep step) {
    return Optional.ofNullable(step)
        .map(AgentProcessingStep::getDurationMs)
        .map(ms -> String.format("%.1fs", ms / 1000.0))
        .orElse("");
  }

  default String getRecipientInitials(String recipientName) {
    if (recipientName == null || recipientName.isBlank()) {
      return "?";
    }

    String[] parts = recipientName.trim().split("\\s+");
    
    if (parts.length == 1) {
      String name = parts[0];
      return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    String firstInitial = parts[0].substring(0, 1);
    String lastInitial = parts[parts.length - 1].substring(0, 1);

    return (firstInitial + lastInitial).toUpperCase();
  }
}
