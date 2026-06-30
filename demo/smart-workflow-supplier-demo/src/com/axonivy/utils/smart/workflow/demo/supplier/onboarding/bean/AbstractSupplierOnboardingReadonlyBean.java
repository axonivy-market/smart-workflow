package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.axonivy.utils.smart.workflow.demo.document.CertificationUploader;
import com.axonivy.utils.smart.workflow.demo.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.document.RequiredDocumentUploader;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentObjectType;
import com.axonivy.utils.smart.workflow.demo.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces.AgentResultView;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces.DocumentDisplaySupport;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces.SupplierFormSupport;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper.OnboardingRequestHelper;

public abstract class AbstractSupplierOnboardingReadonlyBean
    implements Serializable, CertificationUploader, RequiredDocumentUploader, AgentResultView, DocumentDisplaySupport,
               SupplierFormSupport {

  private static final long serialVersionUID = 1L;

  protected OnboardingRequest request;

  private List<LegalDocument> supplierDocuments = new ArrayList<>();
  private String pendingDocumentType;
  private SupplierAgentResponse agentResponse;

  public void init(OnboardingRequest request) {
    if (this.request != null) {
      return;
    }
    this.request = request;
    ensureNestedObjectsExist();
    loadDocuments();
    if (agentResponse == null) {
      agentResponse = AgentResultView.resolveAgentResponse();
    }
  }

  private void ensureNestedObjectsExist() {
    OnboardingRequestHelper.ensureNestedObjectsExist(request);
  }

  @Override
  public List<LegalDocument> getSupplierDocuments() {
    return supplierDocuments;
  }

  @Override
  public void setSupplierDocuments(List<LegalDocument> docs) {
    this.supplierDocuments = docs != null ? docs : new ArrayList<>();
  }

  @Override
  public String getPendingDocumentType() {
    return pendingDocumentType;
  }

  @Override
  public void setPendingDocumentType(String pendingDocumentType) {
    this.pendingDocumentType = pendingDocumentType;
  }

  @Override
  public String getObjectId() {
    return Optional.ofNullable(request)
        .map(OnboardingRequest::getSupplier)
        .map(Supplier::getSupplierId)
        .orElse(null);
  }

  @Override
  public LegalDocumentObjectType getObjectType() {
    return LegalDocumentObjectType.SUPPLIER;
  }

  public OnboardingRequest getRequest() {
    return request;
  }

  public Supplier getSupplier() {
    return Optional.ofNullable(request)
        .map(OnboardingRequest::getSupplier)
        .orElse(null);
  }

  public List<ValidationFinding> getPolicyValidationFindings() {
    return Optional.ofNullable(request)
        .map(OnboardingRequest::getPolicyValidationFindings)
        .orElse(List.of());
  }

  @Override
  public SupplierAgentResponse getAgentResponse() {
    return agentResponse;
  }
}
