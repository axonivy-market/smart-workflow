package com.axonivy.utils.smart.workflow.demo.erp.supplier.bean;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.faces.application.Application;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingRequest;

@ManagedBean
@ViewScoped
public class SupplierDuplicateCheckBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private OnboardingRequest request;
  private SupplierAgentResponse agentResponse;
  private Supplier selectedSupplier;
  private boolean useExistingSupplier;
  private boolean initialized;

  public String init(OnboardingRequest request, SupplierAgentResponse agentResponse) {
    if (initialized) {
      return null;
    }
    initialized = true;
    this.request = request;
    this.agentResponse = agentResponse;
    List<Supplier> matches = request != null ? request.getMatchedSuppliers() : null;
    this.selectedSupplier = (matches != null && !matches.isEmpty()) ? matches.get(0) : null;
    this.useExistingSupplier = this.selectedSupplier != null;
    return null;
  }

  public void selectMatch(Supplier supplier) {
    this.selectedSupplier = supplier;
    this.useExistingSupplier = true;
  }

  public void chooseUseExistingSupplier() {
    this.useExistingSupplier = true;
  }

  public void chooseCreateNew() {
    this.useExistingSupplier = false;
  }

  public void confirmAndClose() {
    if (useExistingSupplier && selectedSupplier != null) {
      request.setSupplier(selectedSupplier);
    }
    callLogicClose();
  }

  private void callLogicClose() {
    FacesContext fc = FacesContext.getCurrentInstance();
    ELContext el = fc.getELContext();
    Application app = fc.getApplication();
    MethodExpression closeMethod = app.getExpressionFactory()
        .createMethodExpression(el, "#{logic.close}", null,
            new Class<?>[] { OnboardingRequest.class, Boolean.class });
    closeMethod.invoke(el, new Object[] { request, useExistingSupplier });
  }

  public boolean isSelected(String supplierId) {
    return selectedSupplier != null && Objects.equals(selectedSupplier.getSupplierId(), supplierId);
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

  public OnboardingRequest getRequest() {
    return request;
  }

  public SupplierAgentResponse getAgentResponse() {
    return agentResponse;
  }

  public Supplier getSelectedSupplier() {
    return selectedSupplier;
  }

  public void setSelectedSupplier(Supplier selectedSupplier) {
    this.selectedSupplier = selectedSupplier;
  }

  public boolean isUseExistingSupplier() {
    return useExistingSupplier;
  }
}
