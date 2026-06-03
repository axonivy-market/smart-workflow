package com.axonivy.utils.smart.workflow.demo.erp.supplier.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.faces.application.Application;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import com.axonivy.utils.smart.workflow.demo.erp.department.model.Department;
import com.axonivy.utils.smart.workflow.demo.erp.department.repository.DepartmentRepository;
import com.axonivy.utils.smart.workflow.demo.erp.shared.Address;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AuditTrailEntry;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.Country;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.OnboardingStatus;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.Urgency;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.processor.SupplierOnboardingProcessService;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.repository.SupplierRepository;

import ch.ivyteam.ivy.environment.Ivy;

@ManagedBean
@ViewScoped
public class SupplierRequestBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private OnboardingRequest request;
  private List<Department> departments = new ArrayList<>();
  private List<Country> countries = new ArrayList<>();
  private List<Urgency> urgencies = new ArrayList<>();

  public void init(OnboardingRequest request) {
    this.request = request;

    if (request.getRequestedBy() == null) {
      request.setRequestedBy(Ivy.session().getSessionUser().getName());
    }
    if (request.getUrgency() == null) {
      request.setUrgency(Urgency.NORMAL.name());
    }
    if (request.getSupplier() == null) {
      Supplier supplier = new Supplier();
      supplier.setBusinessAddress(new Address());
      request.setSupplier(supplier);
    } else if (request.getSupplier().getBusinessAddress() == null) {
      request.getSupplier().setBusinessAddress(new Address());
    }

    departments = DepartmentRepository.getInstance().findAll();
    countries = Arrays.asList(Country.values());
    urgencies = Arrays.asList(Urgency.values());
  }

  public void submit() {
    request.setStatus(OnboardingStatus.DB_CHECK);
    request.setCaseUuid(Ivy.wfCase().uuid());
    AuditTrailEntry requestEntry = SupplierOnboardingProcessService.buildRequestAuditEntry(request);
    request.setAuditTrail(SupplierOnboardingProcessService.ensureAndAdd(request.getAuditTrail(), requestEntry));
  }

  public void saveDraft() {
    request.setStatus(OnboardingStatus.REQUEST);
    Supplier supplier = request.getSupplier();
    var repo = SupplierRepository.getInstance();
    if (supplier.getSupplierId() == null) {
      repo.create(supplier);
    } else {
      repo.update(supplier);
    }
  }

  public void saveDraftAndClose() {
    saveDraft();
    callLogicClose();
  }

  public void cancel() {
    request.setStatus(null);
  }

  public void cancelAndClose() {
    cancel();
    callLogicClose();
  }

  public void submitAndClose() {
    submit();
    callLogicClose();
  }

  private void callLogicClose() {
    FacesContext fc = FacesContext.getCurrentInstance();
    ELContext el = fc.getELContext();
    Application app = fc.getApplication();
    MethodExpression closeMethod = app.getExpressionFactory()
        .createMethodExpression(el, "#{logic.close}", null, new Class<?>[] { OnboardingRequest.class });
    closeMethod.invoke(el, new Object[] { request });
  }

  public String getDisplayRequester() {
    return Ivy.session().getSessionUser().getDisplayName();
  }

  public OnboardingRequest getRequest() {
    return request;
  }

  public Supplier getSupplier() {
    return request != null ? request.getSupplier() : null;
  }

  public List<Department> getDepartments() {
    return departments;
  }

  public List<Country> getCountries() {
    return countries;
  }

  public List<Urgency> getUrgencies() {
    return urgencies;
  }
}
