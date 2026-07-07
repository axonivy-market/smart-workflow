package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.faces.context.FacesContext;

import com.axonivy.utils.smart.workflow.demo.supplier.agent.DocumentExtractionResult;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.PolicyValidationResult;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.RiskScoreResult;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.agent.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.audit.AuditTrailEntry;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces.SubProcessCaller;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AgentStepStatus;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.FindingSeverity;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper.AgentStepHelper;

import ch.ivyteam.ivy.environment.Ivy;

public class AgentAnalysisService {

  private interface Signature {
    String DOCUMENT_EXTRACT   = "supplierDocumentExtractAgent(com.axonivy.utils.smart.workflow.demo.supplier.Supplier,com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest)";
    String VALIDATE_POLICY    = "validateAgainstPolicy(com.axonivy.utils.smart.workflow.demo.supplier.Supplier,com.axonivy.utils.smart.workflow.demo.supplier.agent.DocumentExtractionResult,com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest)";
    String VALIDATE_FINANCIAL = "validateFinancialPolicy(com.axonivy.utils.smart.workflow.demo.supplier.Supplier,com.axonivy.utils.smart.workflow.demo.supplier.agent.DocumentExtractionResult,String)";
    String RISK_ASSESSMENT    = "callRiskAssessment(com.axonivy.utils.smart.workflow.demo.supplier.agent.PolicyValidationResult,com.axonivy.utils.smart.workflow.demo.supplier.agent.PolicyValidationResult,Integer,String,String)";
  }

  private interface Field {
    String SUPPLIER                    = "supplier";
    String ONBOARDING_REQUEST          = "onboardingRequest";
    String DOCUMENTS                   = "documents";
    String CASE_UUID                   = "caseUuid";
    String POLICY_RESULT               = "policyResult";
    String FINANCIAL_RESULT            = "financialResult";
    String ANNUAL_VOLUME_EUR           = "annualVolumeEur";
    String SUPPLIER_ID                 = "supplierId";
    String EXTRACTION_RESULT           = "extractionResult";
    String POLICY_VALIDATION_RESULT    = "policyValidationResult";
    String FINANCIAL_VALIDATION_RESULT = "financialValidationResult";
    String RISK_SCORE_RESULT           = "riskScoreResult";
  }

  private interface StepKey {
    String DOCUMENT_EXTRACTION    = "DOCUMENT_EXTRACTION";
    String POLICY_VALIDATION      = "POLICY_VALIDATION";
    String FINANCIAL_VALIDATION   = "FINANCIAL_VALIDATION";
    String RISK_SCORE_CALCULATION = "RISK_SCORE_CALCULATION";
  }

  private static final String RISK_SCORE_FORMAT = "Risk score: %s. Routing: %s";
  private static final String ANALYSIS_COMPLETE = "Analysis complete.";

  public SupplierAgentResponse startAnalysis(
      String step1Name, String step2Name, String step3Name, String step4Name) {
    SupplierAgentResponse response = new SupplierAgentResponse();
    List<AgentProcessingStep> steps = new ArrayList<>();
    steps.add(AgentStepHelper.createPendingStep(StepKey.DOCUMENT_EXTRACTION,    step1Name));
    steps.add(AgentStepHelper.createPendingStep(StepKey.POLICY_VALIDATION,      step2Name));
    steps.add(AgentStepHelper.createPendingStep(StepKey.FINANCIAL_VALIDATION,   step3Name));
    steps.add(AgentStepHelper.createPendingStep(StepKey.RISK_SCORE_CALCULATION, step4Name));
    response.setProcessingSteps(steps);
    return response;
  }

  public DocumentExtractionResult runStep1(
      OnboardingRequest request, SupplierAgentResponse agentResponse, FacesContext fc) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put(Field.SUPPLIER, request.getSupplier());
    params.put(Field.ONBOARDING_REQUEST, request);
    return executeStep(0, Signature.DOCUMENT_EXTRACT, params, fc, agentResponse,
        r -> (DocumentExtractionResult) r.get(Field.EXTRACTION_RESULT),
        DocumentExtractionResult::getProcessingStep);
  }

  public PolicyValidationResult runStep2(
      OnboardingRequest request, DocumentExtractionResult extractionResult,
      SupplierAgentResponse agentResponse, FacesContext fc) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put(Field.SUPPLIER, request.getSupplier());
    params.put(Field.DOCUMENTS, extractionResult);
    params.put(Field.ONBOARDING_REQUEST, request);
    return executeStep(1, Signature.VALIDATE_POLICY, params, fc, agentResponse,
        r -> (PolicyValidationResult) r.get(Field.POLICY_VALIDATION_RESULT),
        PolicyValidationResult::getProcessingStep);
  }

  public PolicyValidationResult runStep3(
      OnboardingRequest request, DocumentExtractionResult extractionResult,
      SupplierAgentResponse agentResponse, FacesContext fc) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put(Field.SUPPLIER, request.getSupplier());
    params.put(Field.DOCUMENTS, extractionResult);
    params.put(Field.CASE_UUID, Ivy.wfCase().uuid());
    return executeStep(2, Signature.VALIDATE_FINANCIAL, params, fc, agentResponse,
        r -> (PolicyValidationResult) r.get(Field.FINANCIAL_VALIDATION_RESULT),
        PolicyValidationResult::getProcessingStep);
  }

  public void runStep4(
      OnboardingRequest request,
      PolicyValidationResult policyValidationResult, PolicyValidationResult financialValidationResult,
      SupplierAgentResponse agentResponse, FacesContext fc) {
    Integer annualVolumeEur = request.getExpectedAnnualVolume() != null
        ? request.getExpectedAnnualVolume().intValue() : null;
    String supplierId = request.getSupplier() != null ? request.getSupplier().getSupplierId() : null;
    Map<String, Object> params = new LinkedHashMap<>();
    params.put(Field.POLICY_RESULT, policyValidationResult);
    params.put(Field.FINANCIAL_RESULT, financialValidationResult);
    params.put(Field.ANNUAL_VOLUME_EUR, annualVolumeEur);
    params.put(Field.SUPPLIER_ID, supplierId);
    params.put(Field.CASE_UUID, Ivy.wfCase().uuid());
    RiskScoreResult riskScoreResult = executeStep(3, Signature.RISK_ASSESSMENT, params, fc, agentResponse,
        r -> (RiskScoreResult) r.get(Field.RISK_SCORE_RESULT),
        RiskScoreResult::getProcessingStep);
    if (riskScoreResult == null) {
      return;
    }
    finalizeAgentResponse(agentResponse, riskScoreResult, policyValidationResult, financialValidationResult);
    appendAuditTrailEntry(request, agentResponse);
  }

  private void finalizeAgentResponse(
      SupplierAgentResponse agentResponse, RiskScoreResult riskScoreResult,
      PolicyValidationResult policyValidationResult, PolicyValidationResult financialValidationResult) {
    var riskScore       = riskScoreResult.getRiskScore();
    var routingDecision = riskScoreResult.getRoutingDecision() != null
        ? riskScoreResult.getRoutingDecision() : "CLARIFICATION";

    agentResponse.setRiskScore(riskScore);
    agentResponse.setRoutingDecision(routingDecision);
    agentResponse.setValidationFindings(mergeFindings(policyValidationResult, financialValidationResult));

    String summary = riskScore != null
        ? String.format(RISK_SCORE_FORMAT, riskScore.getAggregate(), routingDecision)
        : ANALYSIS_COMPLETE;
    agentResponse.setFeedback(summary);
  }

  private void appendAuditTrailEntry(OnboardingRequest request, SupplierAgentResponse agentResponse) {
    AuditTrailEntry analysisEntry =
        OnboardingAuditEntryFactory.buildAgentAnalysisAuditEntry(request, agentResponse);
    if (request.getAuditTrail() == null) {
      request.setAuditTrail(new ArrayList<>());
    }
    request.getAuditTrail().add(analysisEntry);
  }

  private List<ValidationFinding> mergeFindings(
      PolicyValidationResult policyValidationResult, PolicyValidationResult financialValidationResult) {
    Set<String> seen = new HashSet<>();
    return Stream.of(policyValidationResult, financialValidationResult)
        .filter(Objects::nonNull)
        .map(PolicyValidationResult::getFindings)
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .filter(f -> FindingSeverity.FAILURE.equals(f.getSeverity())
                  || FindingSeverity.WARNING.equals(f.getSeverity()))
        .filter(f -> seen.add(f.getMessage()))
        .toList();
  }

  private <R> R executeStep(int index, String signature,
      Map<String, Object> params, FacesContext fc, SupplierAgentResponse agentResponse,
      Function<Map<String, Object>, R> extract,
      Function<R, AgentProcessingStep> toProcessingStep) {
    AgentProcessingStep step = AgentStepHelper.getStep(index, agentResponse);
    step.setStatus(AgentStepStatus.RUNNING);
    try {
      Map<String, Object> result = SubProcessCaller.callSubProcess(signature, params);
      R typed = extract.apply(result);
      AgentStepHelper.finalizeStep(step, typed != null ? toProcessingStep.apply(typed) : null);
      return typed;
    } catch (Exception ex) {
      step.setStatus(AgentStepStatus.FAILED);
      AgentStepHelper.addStepErrorMessage(step.getStepKey(), ex,
          agentResponse.getProcessingSteps(), fc);
      return null;
    }
  }
}
