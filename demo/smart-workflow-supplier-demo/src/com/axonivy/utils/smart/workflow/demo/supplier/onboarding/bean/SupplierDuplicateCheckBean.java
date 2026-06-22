package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.axonivy.utils.smart.workflow.demo.assistant.AgentGuidance;
import com.axonivy.utils.smart.workflow.demo.enums.Status;
import com.axonivy.utils.smart.workflow.demo.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.OnboardingStatus;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces.LogicCloseSupport;

@ManagedBean
@ViewScoped
public class SupplierDuplicateCheckBean implements Serializable, LogicCloseSupport {

  private static final long serialVersionUID = 1L;

  private OnboardingRequest request;
  private SupplierAgentResponse agentResponse;
  private boolean initialized;

  public String init(OnboardingRequest request, SupplierAgentResponse agentResponse) {
    if (initialized) {
      return null;
    }
    initialized = true;
    this.request = request;
    this.agentResponse = agentResponse;
    return null;
  }

  public void confirmAndClose() {
    callLogicClose(request);
  }

  public void cancelRequest() {
    request.setStatus(OnboardingStatus.DECLINED);
    callLogicClose(request);
  }

  public boolean isHasMatches() {
    List<Supplier> matches = request != null ? request.getMatchedSuppliers() : null;
    return matches != null && !matches.isEmpty();
  }

  public List<Supplier> getMatches() {
    if (request == null || request.getMatchedSuppliers() == null) {
      return Collections.emptyList();
    }
    return request.getMatchedSuppliers();
  }

  public int getMatchCount() {
    List<Supplier> matches = request != null ? request.getMatchedSuppliers() : null;
    return matches != null ? matches.size() : 0;
  }

  public String getColorClass() {
    if (agentResponse != null && agentResponse.getStatus() == Status.ERROR) {
      return Status.ERROR.colorClass;
    }
    return isHasMatches() ? "text-yellow-600" : Status.SUCCESS.colorClass;
  }

  public String getIconClass() {
    if (agentResponse != null && agentResponse.getStatus() == Status.ERROR) {
      return Status.ERROR.iconClass;
    }
    return isHasMatches() ? "ti-alert-triangle" : Status.SUCCESS.iconClass;
  }

  public OnboardingRequest getRequest() {
    return request;
  }

  public SupplierAgentResponse getAgentResponse() {
    return agentResponse;
  }

  public List<AgentGuidance> getAgentGuidance() {
    return List.of(
        guidance("What does a potential match mean?",
            "explain that the AI found existing suppliers with a similar name, country, or business purpose — the user should review each match and decide if it is the same legal entity or a different supplier"),
        guidance("How is the match score determined?",
            "explain that the AI compares name similarity, country, VAT ID, and business purpose overlap — a same-country supplier in the same business category is a stronger duplicate signal even if the name differs slightly"),
        guidance("Should I proceed if a duplicate is found?",
            "explain that if the match is clearly the same legal entity the user should stop and reuse the existing supplier; if it is genuinely a different company despite the similarity, they can confirm and proceed with a new registration"),
        guidance("What information was compared?",
            "explain that the system searched by business name and country, then the AI analysed name similarity, VAT ID, and business purpose to assess whether it is likely the same supplier"));
  }

  private static AgentGuidance guidance(String questionPattern, String instruction) {
    AgentGuidance g = new AgentGuidance();
    g.setQuestionPattern(questionPattern);
    g.setInstruction(instruction);
    return g;
  }
}
