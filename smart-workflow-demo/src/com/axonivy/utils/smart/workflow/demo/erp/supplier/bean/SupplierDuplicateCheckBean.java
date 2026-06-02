package com.axonivy.utils.smart.workflow.demo.erp.supplier.bean;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.faces.application.Application;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import com.axonivy.utils.smart.workflow.demo.erp.shared.Status;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingRequest;

@ManagedBean
@ViewScoped
public class SupplierDuplicateCheckBean implements Serializable {

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
    callLogicClose();
  }

  private void callLogicClose() {
    FacesContext fc = FacesContext.getCurrentInstance();
    ELContext el = fc.getELContext();
    Application app = fc.getApplication();
    MethodExpression closeMethod = app.getExpressionFactory()
        .createMethodExpression(el, "#{logic.close}", null,
            new Class<?>[] { OnboardingRequest.class, Boolean.class });
    closeMethod.invoke(el, new Object[] { request, false });
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
}
