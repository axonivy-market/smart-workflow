package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import com.axonivy.utils.smart.workflow.demo.assistant.AgentGuidance;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.DocumentExtractionResult;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.PolicyValidationResult;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.agent.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces.AgentResultView;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces.DocumentDisplaySupport;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces.LogicCloseSupport;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces.RiskLevelSupport;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AgentStepStatus;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper.SupplierOnboardingGuidance;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service.AgentAnalysisService;

import ch.ivyteam.ivy.environment.Ivy;

@ManagedBean
@ViewScoped
public class SupplierAgentProcessingBean
    implements Serializable, RiskLevelSupport, DocumentDisplaySupport, LogicCloseSupport {

  private static final long serialVersionUID = 1L;

  public interface StepLabel {
    String DOCUMENT_EXTRACTION    = "Document Extraction";
    String POLICY_VALIDATION      = "Policy Validation";
    String FINANCIAL_VALIDATION   = "Financial Validation";
    String RISK_SCORE_CALCULATION = "Risk Score Calculation";
  }

  private OnboardingRequest request;
  private SupplierAgentResponse agentResponse;
  private boolean initialized;

  private DocumentExtractionResult extractionResult;
  private PolicyValidationResult policyValidationResult;
  private PolicyValidationResult financialValidationResult;

  public void init() {
    if (initialized) {
      return;
    }
    FacesContext ctx = FacesContext.getCurrentInstance();
    this.request = (OnboardingRequest) ctx.getApplication()
        .evaluateExpressionGet(ctx, "#{data.request}", Object.class);
    this.agentResponse = AgentResultView.resolveAgentResponse();
    initialized = true;
  }

  public void startAnalysis() {
    extractionResult          = null;
    policyValidationResult    = null;
    financialValidationResult = null;
    this.agentResponse = new AgentAnalysisService().startAnalysis(
        Ivy.cms().co(CMS_AGENT_PROCESSING_DETAIL + "StepDocumentExtraction"),
        Ivy.cms().co(CMS_AGENT_PROCESSING_DETAIL + "StepPolicyValidation"),
        Ivy.cms().co(CMS_AGENT_PROCESSING_DETAIL + "StepFinancialValidation"),
        Ivy.cms().co(CMS_AGENT_PROCESSING_DETAIL + "StepRiskScoreCalculation"));
    syncAgentResponse(agentResponse);
  }

  public void runStep1() {
    extractionResult = new AgentAnalysisService()
        .runStep1(request, agentResponse, FacesContext.getCurrentInstance());
  }

  public void runStep2() {
    policyValidationResult = new AgentAnalysisService()
        .runStep2(request, extractionResult, agentResponse, FacesContext.getCurrentInstance());
  }

  public void runStep3() {
    financialValidationResult = new AgentAnalysisService()
        .runStep3(request, extractionResult, agentResponse, FacesContext.getCurrentInstance());
  }

  public void runStep4() {
    new AgentAnalysisService().runStep4(
        request, policyValidationResult, financialValidationResult,
        agentResponse, FacesContext.getCurrentInstance());
    syncAgentResponse(agentResponse);
  }

  public OnboardingRequest getRequest() {
    return request;
  }

  @Override
  public SupplierAgentResponse getAgentResponse() {
    return agentResponse;
  }

  public List<ValidationFinding> getPolicyValidationFindings() {
    return Optional.ofNullable(request)
        .map(OnboardingRequest::getPolicyValidationFindings)
        .orElse(List.of());
  }

  public List<AgentGuidance> getAgentGuidance() {
    return SupplierOnboardingGuidance.forAgentProcessing();
  }

  @Override
  public boolean isAnalysisComplete() {
    return agentResponse != null
        && agentResponse.getRoutingDecision() != null
        && !agentResponse.getRoutingDecision().isEmpty();
  }

  public boolean isAnalysisStarted() {
    if (isAnalysisComplete() || isHasFailedStep()) {
      return false;
    }
    return steps().stream()
        .anyMatch(s -> s.getStatus() != null && s.getStatus() != AgentStepStatus.PENDING);
  }

  @Override
  public boolean isHasFailedStep() {
    return Optional.ofNullable(agentResponse)
        .map(SupplierAgentResponse::getProcessingSteps)
        .map(steps -> steps.stream().anyMatch(s -> AgentStepStatus.FAILED.equals(s.getStatus())))
        .orElse(false);
  }

  public String getContinueButtonLabel() {
    if (agentResponse == null || agentResponse.getRoutingDecision() == null) {
      return Ivy.cms().co(CMS_AGENT_PROCESSING + "ContinueButton");
    }
    return switch (agentResponse.getRoutingDecision().toUpperCase()) {
      case "APPROVAL"      -> Ivy.cms().co(CMS_AGENT_PROCESSING + "ContinueToApprovalButton");
      case "CLARIFICATION" -> Ivy.cms().co(CMS_AGENT_PROCESSING + "ContinueToClarificationButton");
      case "DECLINE"       -> Ivy.cms().co(CMS_AGENT_PROCESSING + "ViewDeclineButton");
      default              -> Ivy.cms().co(CMS_AGENT_PROCESSING + "ContinueButton");
    };
  }

  public String getContinueButtonClass() {
    if (agentResponse == null || agentResponse.getRoutingDecision() == null) {
      return "ui-button-primary";
    }
    return switch (agentResponse.getRoutingDecision().toUpperCase()) {
      case "DECLINE" -> "ui-button-danger";
      default        -> "ui-button-primary";
    };
  }

  @Override
  public String getDecisionBoxCssClass() {
    return Optional.ofNullable(agentResponse)
        .map(SupplierAgentResponse::getRoutingDecision)
        .map(String::toUpperCase)
        .map(decision -> switch (decision) {
          case "APPROVAL" -> "so-success-banner";
          case "DECLINE"  -> "so-decline-banner";
          default         -> "";
        })
        .orElse("");
  }

  public String getStepStatusClass(AgentProcessingStep step) {
    if (step == null || step.getStatus() == null) {
      return "text-color-secondary";
    }
    return switch (step.getStatus()) {
      case COMPLETED -> "text-green-600";
      case RUNNING   -> "text-blue-600";
      case FAILED    -> "text-red-600";
      default        -> "text-color-secondary";
    };
  }

  // --- Private helpers ---

  private List<AgentProcessingStep> steps() {
    return Optional.ofNullable(agentResponse)
        .map(SupplierAgentResponse::getProcessingSteps)
        .orElse(List.of());
  }

  private void syncAgentResponse(SupplierAgentResponse response) {
    FacesContext fc = FacesContext.getCurrentInstance();
    fc.getApplication().getExpressionFactory()
        .createValueExpression(fc.getELContext(), "#{data.agentResponse}", Object.class)
        .setValue(fc.getELContext(), response);
  }
}
